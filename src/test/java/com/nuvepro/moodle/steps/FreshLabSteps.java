package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.helpers.CloudLabsClient;
import com.nuvepro.moodle.pages.ManageLabs;
import com.nuvepro.moodle.pages.PlaygroundLanding;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.EnumSet;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Lab-action tests on a FRESH per-user lab, to avoid the shared-lab expiry problem.
 * Flow: seed a user + enrol (Web Services) -> create a lab for them (Manage Labs, admin,
 * cp.php-free) -> perform actions and verify completion via the user's Manage Labs row.
 *
 * NOTE: the seeding token has no delete-user function, so test users accumulate (delete via
 * the Moodle UI or add core_user_delete_users to the service to enable API cleanup).
 */
public class FreshLabSteps {
    private final TestContext ctx;
    private ApiClient.SeededUser user;
    private String subscriptionId;
    private boolean extendOk;

    public FreshLabSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private int sectionId() {
        if (ctx.sectionId == null || ctx.sectionId.isEmpty()) {
            ctx.landing.open(ctx.cmid);
            ctx.sectionId = ctx.landing.hiddenSectionId();
        }
        return Integer.parseInt(ctx.sectionId);
    }

    @Given("a freshly enrolled user")
    public void aFreshlyEnrolledUser() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        long stamp = System.currentTimeMillis();
        user = ApiClient.createUser(stamp);
        ApiClient.enrolUser(user.id, Long.parseLong(Settings.COURSE_ID), 5); // 5 = student
        System.out.println("[FreshLab] seeded user " + user.username + " (id=" + user.id + ")");
    }

    @When("I create a lab for that user from Manage Labs")
    public void iCreateALabForThatUser() {
        ManageLabs ml = new ManageLabs(ctx.page);
        ml.createLabForUserEmail(user.email, sectionId());
        GlobalHooks.labProvisioned = true;
        // Capture the lab id (== provider subscriptionId) from the created row while the user exists
        // (its email is shown). From here on we poll STATUS via the CloudLabs API, not the UI.
        for (int i = 0; i < 12 && subscriptionId == null; i++) {
            ml.open(sectionId());
            subscriptionId = ml.labIdForEmail(user.email);
            if (subscriptionId == null) ctx.page.waitForTimeout(3_000);
        }
        assertNotNull(subscriptionId, "could not capture lab id after create for " + user.email);
        System.out.println("[FreshLab] captured lab id (subscriptionId) = " + subscriptionId);
    }

    @Then("that user's lab reaches Running")
    public void thatUsersLabReachesRunning() {
        ManageLabs.LabState st = CloudLabsClient.waitForState(subscriptionId,
                EnumSet.of(ManageLabs.LabState.RUNNING, ManageLabs.LabState.FAILED), 180, 5);
        assertEquals(st, ManageLabs.LabState.RUNNING, "fresh lab did not reach Running (state=" + st + ")");
    }

    @When("I extend that user's lab duration by {int} hour")
    public void iExtendThatUsersLabDuration(int hours) {
        if (!CloudLabsClient.isConfigured()) throw new SkipException("CloudLabs API not configured (.env)");
        assertNotNull(subscriptionId, "lab id not captured");
        extendOk = CloudLabsClient.extendHours(subscriptionId, hours);   // reuse captured id
    }

    @Then("the lab duration extension succeeds")
    public void theLabDurationExtensionSucceeds() {
        assertTrue(extendOk, "CloudLabs extendDuration did not return SUCCESS for subscription " + subscriptionId);
    }

    @When("I expire that user's lab")
    public void iExpireThatUsersLab() {
        if (!CloudLabsClient.isConfigured()) throw new SkipException("CloudLabs API not configured (.env)");
        assertNotNull(subscriptionId, "lab id not captured");
        assertTrue(CloudLabsClient.expireLab(subscriptionId), "CloudLabs reduce/expire did not succeed");
        ctx.page.waitForTimeout(8_000);   // let the provider apply the new (past) end time
    }

    @Then("that user sees the lab as expired")
    public void thatUserSeesLabAsExpired() {
        // The expired state is user-facing: log in AS the seeded user and open their lab console.
        ctx.studentContext = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page sp = ctx.studentContext.newPage();
        sp.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(sp, user.username, user.password);
        String bodyClass = sp.locator("body").getAttribute("class");
        System.out.println("[Expired] after login url=" + sp.url() + " loggedIn="
                + (bodyClass == null || !bodyClass.contains("notloggedin")));
        PlaygroundLanding landing = new PlaygroundLanding(sp);
        // Expiry surfaces on the LANDING as "Lab Duration Remaining" = 0h (#lab-remaining-pl),
        // populated by the status worker — no console/cp.php needed.
        boolean expired = false;
        String remaining = "";
        for (int i = 0; i < 18 && !expired; i++) {
            if (i == 0 || i == 6 || i == 12) landing.open(ctx.cmid);   // fresh loads to recompute
            Locator rem = sp.locator("#lab-remaining-pl");
            if (rem.count() > 0) {
                remaining = rem.first().innerText().trim();
                String norm = remaining.toLowerCase().replaceAll("\\s", "");
                if (norm.equals("0h") || norm.equals("0") || norm.equals("0h0m") || norm.equals("00:00")
                        || norm.matches("0h0m.*")) {
                    expired = true;
                    break;
                }
            }
            sp.waitForTimeout(5_000);
        }
        System.out.println("[Expired] lab-remaining-pl='" + remaining + "' status="
                + (sp.locator("#lab-status-pl").count() > 0 ? sp.locator("#lab-status-pl").first().innerText().trim() : "?"));
        assertTrue(expired, "expired lab 'Lab Duration Remaining' was not 0h (was '" + remaining + "')");
    }

    @When("I stop that user's lab")
    public void iStopThatUsersLab() {
        new ManageLabs(ctx.page).performActionForEmail(user.email, ManageLabs.STOP, sectionId());
    }

    @Then("that user's lab reaches Stopped")
    public void thatUsersLabReachesStopped() {
        ManageLabs.LabState st = CloudLabsClient.waitForState(subscriptionId,
                EnumSet.of(ManageLabs.LabState.STOPPED, ManageLabs.LabState.FAILED), 180, 5);
        assertEquals(st, ManageLabs.LabState.STOPPED, "fresh lab did not reach Stopped (state=" + st + ")");
    }

    @When("I delete that user's lab")
    public void iDeleteThatUsersLab() {
        new ManageLabs(ctx.page).performActionForEmail(user.email, ManageLabs.DELETE, sectionId());
    }

    @Then("that user's lab reaches Deleted")
    public void thatUsersLabReachesDeleted() {
        ManageLabs.LabState st = CloudLabsClient.waitForState(subscriptionId,
                EnumSet.of(ManageLabs.LabState.DELETED), 600, 15);   // delete ≈5-8 min (slow, variable)
        assertEquals(st, ManageLabs.LabState.DELETED, "fresh lab did not reach Deleted (state=" + st + ")");
    }

    /** Delete the seeded user after the scenario (core_user_delete_users is now enabled). */
    @After("@freshlab")
    public void cleanupSeededUser() {
        if (user != null) {
            ApiClient.deleteUser(user.id);
            System.out.println("[FreshLab] cleaned up user " + user.username + " (id=" + user.id + ")");
            user = null;
        }
    }
}
