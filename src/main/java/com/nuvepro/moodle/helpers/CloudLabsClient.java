package com.nuvepro.moodle.helpers;

import com.nuvepro.moodle.config.Settings;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CloudLabs (provider) API client — SEPARATE system from Moodle. Authenticates with an admin
 * login (username/password → JSON {"token": ...}) and uses that token as the X-CSRF-Token header.
 * Used to EXTEND an existing lab's duration so long/existing-lab tests don't expire mid-run.
 * The Manage Labs lab id == the provider subscriptionId.
 *
 *   POST {gateway}/v1/users/login              (form: username, password) -> {"token": "..."}
 *   POST {gateway}/v1/subscriptions/extendDuration  (form: subscriptionId, duration, durationType)
 *        header X-CSRF-Token: {token}          -> {"ResponseStatus":"SUCCESS", ...}
 */
public final class CloudLabsClient {
    private CloudLabsClient() {}

    private static final HttpClient HTTP = trustAll();   // strictSSL=false in the Postman collection
    private static volatile String token;

    public static boolean isConfigured() {
        return !Settings.CLOUDLABS_GATEWAY_URL.isEmpty()
                && !Settings.CLOUDLABS_ADMIN_USER.isEmpty()
                && !Settings.CLOUDLABS_ADMIN_PASS.isEmpty();
    }

    private static String gateway() {
        return Settings.CLOUDLABS_GATEWAY_URL.replaceAll("/+$", "");
    }

    private static String loginUrl() {
        // Always derive from the gateway base (validated: {gateway}/v1/users/login returns the token).
        // Ignore CLOUDLABS_LOGIN_URL to avoid pointing it at the UI host by mistake.
        return gateway() + "/v1/users/login";
    }

    /** Authenticate and cache the token. Returns the token, or throws on failure. */
    public static synchronized String login() {
        String body = "username=" + enc(Settings.CLOUDLABS_ADMIN_USER)
                + "&password=" + enc(Settings.CLOUDLABS_ADMIN_PASS);
        HttpResponse<String> resp = post(loginUrl(), body, null);
        if (resp.statusCode() != 200) {
            throw new RuntimeException("CloudLabs login failed: HTTP " + resp.statusCode() + " " + snippet(resp.body()));
        }
        Matcher m = Pattern.compile("\"token\"\\s*:\\s*\"([^\"]+)\"").matcher(resp.body());
        if (!m.find()) throw new RuntimeException("CloudLabs login: no token in response: " + snippet(resp.body()));
        token = m.group(1);
        return token;
    }

    /** Extend a lab's duration. Returns true on ResponseStatus SUCCESS. Re-logs-in once on 401/403. */
    public static boolean extendDuration(String subscriptionId, int duration, String durationType) {
        if (subscriptionId == null || subscriptionId.isEmpty()) return false;
        if (token == null) login();
        String body = "subscriptionId=" + enc(subscriptionId)
                + "&duration=" + duration + "&durationType=" + enc(durationType);
        String url = gateway() + "/v1/subscriptions/extendDuration";
        HttpResponse<String> resp = post(url, body, token);
        if (resp.statusCode() == 401 || resp.statusCode() == 403) {   // token expired -> re-login once
            login();
            resp = post(url, body, token);
        }
        boolean ok = resp.statusCode() == 200 && resp.body() != null && resp.body().contains("SUCCESS");
        System.out.println("[CloudLabs] extendDuration sub=" + subscriptionId + " +" + duration + durationType
                + " -> " + (ok ? "SUCCESS" : "HTTP " + resp.statusCode() + " " + snippet(resp.body())));
        return ok;
    }

    /** Convenience: extend by N hours. */
    public static boolean extendHours(String subscriptionId, int hours) {
        return extendDuration(subscriptionId, hours, "Hours");
    }

    /** Reduce a lab's duration by N hours (negative extend). */
    public static boolean reduceHours(String subscriptionId, int hours) {
        return extendDuration(subscriptionId, -Math.abs(hours), "Hours");
    }

    /** Force a lab to EXPIRE by pushing its end time well into the past. */
    public static boolean expireLab(String subscriptionId) {
        return reduceHours(subscriptionId, 240);   // -10 days: end time is now in the past
    }

    // ---- authoritative status (replaces UI scraping) ----

    /** A subscription's current action/status from CloudLabs (nl_action / nl_status). */
    public static final class SubStatus {
        public final String action;
        public final String status;
        public SubStatus(String action, String status) { this.action = action; this.status = status; }
    }

    /** Live status of a lab from CloudLabs. Returns null if the id isn't found. */
    public static SubStatus getStatus(String subscriptionId) {
        if (token == null) login();
        String url = gateway() + "/v1/subscriptions/getSubscriptionsStatus";
        HttpResponse<String> resp = post(url, "subscriptionIds=" + enc(subscriptionId), token);
        if (resp.statusCode() == 401 || resp.statusCode() == 403) { login(); resp = post(url, "subscriptionIds=" + enc(subscriptionId), token); }
        String b = resp.body();
        if (b == null) return null;
        // [{"id":"34522","nl_action":"Create","nl_status":"Complete",...}]
        Matcher m = Pattern.compile("\"nl_action\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"nl_status\"\\s*:\\s*\"([^\"]*)\"").matcher(b);
        if (m.find()) return new SubStatus(m.group(1), m.group(2));
        return null;
    }

    /**
     * Poll CloudLabs (fast, logged) until the lab reaches one of the target states, or timeout.
     * Replaces the slow/opaque Manage-Labs UI polling.
     */
    public static com.nuvepro.moodle.pages.ManageLabs.LabState waitForState(
            String subscriptionId, java.util.Set<com.nuvepro.moodle.pages.ManageLabs.LabState> targets,
            int maxSeconds, int intervalSeconds) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        com.nuvepro.moodle.pages.ManageLabs.LabState st;
        int poll = 0;
        do {
            SubStatus s = getStatus(subscriptionId);
            st = (s == null) ? com.nuvepro.moodle.pages.ManageLabs.LabState.NOTCREATED
                    : com.nuvepro.moodle.pages.ManageLabs.classifyActionStatus(s.action, s.status);
            System.out.println("[CloudLabs] poll " + (++poll) + " lab=" + subscriptionId + " -> "
                    + (s == null ? "(not found)" : s.action + "/" + s.status) + " = " + st);
            if (targets.contains(st)) return st;
            try { Thread.sleep(intervalSeconds * 1000L); } catch (InterruptedException ignored) {}
        } while (System.currentTimeMillis() < deadline);
        return st;
    }

    // ---- plumbing ----

    private static HttpResponse<String> post(String url, String formBody, String csrf) {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody));
        if (csrf != null) b.header("X-CSRF-Token", csrf);
        try {
            return HTTP.send(b.build(), HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException("CloudLabs POST " + url + " failed: " + e.getMessage(), e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String snippet(String s) {
        if (s == null) return "";
        return s.length() > 160 ? s.substring(0, 160) : s;
    }

    private static HttpClient trustAll() {
        // A CookieManager is REQUIRED: login sets a session cookie (SSESS...) that must accompany
        // the X-CSRF-Token on extendDuration, else the API sees the user as "anonymous" (403).
        java.net.CookieManager cookies = new java.net.CookieManager();
        cookies.setCookiePolicy(java.net.CookiePolicy.ACCEPT_ALL);
        try {
            SSLContext ssl = SSLContext.getInstance("TLS");
            ssl.init(null, new TrustManager[]{new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }}, new SecureRandom());
            return HttpClient.newBuilder().sslContext(ssl).cookieHandler(cookies).build();
        } catch (Exception e) {
            return HttpClient.newBuilder().cookieHandler(cookies).build();
        }
    }
}
