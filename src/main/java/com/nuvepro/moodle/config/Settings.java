package com.nuvepro.moodle.config;

import io.github.cdimascio.dotenv.Dotenv;

/**
 * Central configuration, loaded from the .env file (or environment).
 * Mirrors config/settings.py from the Python suite.
 */
public final class Settings {
    private static final Dotenv ENV = Dotenv.configure()
            .directory(".")
            .ignoreIfMissing()
            .load();

    private Settings() {}

    private static String get(String key, String def) {
        String v = ENV.get(key);
        if (v == null || v.isBlank()) v = System.getenv(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    public static final String BASE_URL = stripTrailingSlash(get("MOODLE_BASE_URL", "http://localhost"));
    public static final String ADMIN_USER = get("MOODLE_ADMIN_USER", "admin");
    public static final String ADMIN_PASS = get("MOODLE_ADMIN_PASS", "");
    public static final String STUDENT_USER = get("MOODLE_STUDENT_USER", "student1");
    public static final String STUDENT_PASS = get("MOODLE_STUDENT_PASS", "");
    public static final String WS_TOKEN = get("MOODLE_WS_TOKEN", "");
    // CloudLabs (provider) API — separate system from Moodle; used to extend a lab's duration so
    // existing-lab tests don't expire mid-run. Auth = admin login/pwd against the CloudLabs login API.
    public static final String CLOUDLABS_GATEWAY_URL = get("CLOUDLABS_GATEWAY_URL", "");
    public static final String CLOUDLABS_LOGIN_URL = get("CLOUDLABS_LOGIN_URL", "");
    public static final String CLOUDLABS_ADMIN_USER = get("CLOUDLABS_ADMIN_USER", "");
    public static final String CLOUDLABS_ADMIN_PASS = get("CLOUDLABS_ADMIN_PASS", "");
    public static final String PROVISIONING_MODE = get("PROVISIONING_MODE", "stub");
    public static final String COURSE_ID = get("COURSE_ID", "");
    public static final String PLAYGROUND_CMID = get("PLAYGROUND_CMID", "");
    public static final String GUIDED_CMID = get("GUIDED_CMID", "");
    public static final String ASSESSMENT_CMID = get("ASSESSMENT_CMID", "");
    public static final String PLAYGROUND_VM_CMID = get("PLAYGROUND_VM_CMID", "");
    public static final String PLAYGROUND_CATALOG_CMID = get("PLAYGROUND_CATALOG_CMID", "");
    public static final String PLAYGROUND_BADTEMPLATE_CMID = get("PLAYGROUND_BADTEMPLATE_CMID", "");
    public static final String PLAYGROUND_UNLIMITED_CMID = get("PLAYGROUND_UNLIMITED_CMID", "");
    // Headed/headless and slow-motion can be overridden per-run from the command line:
    //   mvn test -Dheadless=false            -> watch the browser
    //   mvn test -Dheadless=false -Dslowmo=500 -> watch, slowed to 500ms/action
    public static final boolean HEADLESS = resolveBool("headless", "HEADLESS", "true");
    public static final double SLOWMO_MS = resolveNum("slowmo", "SLOWMO_MS", "0");
    public static final double DEFAULT_TIMEOUT_MS = Double.parseDouble(get("DEFAULT_TIMEOUT_MS", "15000"));

    private static boolean resolveBool(String sysProp, String envKey, String def) {
        String sys = System.getProperty(sysProp);
        if (sys != null && !sys.isBlank()) return Boolean.parseBoolean(sys.trim());
        return Boolean.parseBoolean(get(envKey, def));
    }

    private static double resolveNum(String sysProp, String envKey, String def) {
        String sys = System.getProperty(sysProp);
        if (sys != null && !sys.isBlank()) return Double.parseDouble(sys.trim());
        return Double.parseDouble(get(envKey, def));
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
