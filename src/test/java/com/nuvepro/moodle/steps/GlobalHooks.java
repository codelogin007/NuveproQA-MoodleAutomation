package com.nuvepro.moodle.steps;

import com.microsoft.playwright.*;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.pages.PlaygroundControlPanel;
import com.nuvepro.moodle.pages.PlaygroundLanding;
import io.cucumber.java.AfterAll;
import io.cucumber.java.BeforeAll;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Run-wide setup/teardown for the BDD suite:
 *  - @BeforeAll: launch ONE browser and log in ONCE, saving the auth session to storageState
 *    so every scenario starts already-logged-in (no per-scenario login -> no login churn).
 *  - @AfterAll: best-effort stop any running playground lab (safety net for @provisioning
 *    scenarios), then close the browser.
 */
public final class GlobalHooks {
    static Playwright playwright;
    static Browser browser;
    static final Path STORAGE_STATE = Paths.get("target", "state.json");
    /** Set true by a step that provisions/starts a lab, so the @AfterAll safety net only
     *  runs when there could actually be a running lab (keeps non-provisioning runs clean). */
    public static volatile boolean labProvisioned = false;

    private GlobalHooks() {}

    @BeforeAll
    public static void globalSetup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Settings.HEADLESS).setSlowMo(Settings.SLOWMO_MS));
        // Log in once and persist the session.
        try (BrowserContext c = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true))) {
            Page p = c.newPage();
            p.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
            Auth.loginAsAdmin(p);
            c.storageState(new BrowserContext.StorageStateOptions().setPath(STORAGE_STATE));
        }
    }

    @AfterAll
    public static void globalTeardown() {
        // Only run the lab-stop safety net if a lab was actually provisioned this run.
        // Non-provisioning runs (e.g. PG-1 create) skip it entirely — no lab page opened.
        if (labProvisioned) {
            try (BrowserContext c = browser.newContext(new Browser.NewContextOptions()
                    .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true)
                    .setStorageStatePath(STORAGE_STATE))) {
                Page p = c.newPage();
                p.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
                PlaygroundLanding landing = new PlaygroundLanding(p);
                int cmid = Integer.parseInt(Settings.PLAYGROUND_CMID);
                landing.open(cmid);
                String sectionId = landing.hiddenSectionId();
                // Stop via Manage Labs (does not depend on the cp.php console loading).
                com.nuvepro.moodle.pages.ManageLabs ml = new com.nuvepro.moodle.pages.ManageLabs(p);
                ml.open(Integer.parseInt(sectionId));
                Integer running = ml.counters().get("Running");
                if (running != null && running > 0) {
                    try {
                        ml.performActionOnFirstRow(com.nuvepro.moodle.pages.ManageLabs.STOP);
                        p.waitForTimeout(10_000);
                        System.out.println("[globalTeardown] running lab stopped via Manage Labs");
                    } catch (Throwable e) {
                        System.out.println("[globalTeardown] ML stop failed: " + e.getMessage());
                    }
                } else {
                    System.out.println("[globalTeardown] no running lab to stop");
                }
            } catch (Throwable e) {
                System.out.println("[globalTeardown] best-effort stop failed: " + e.getMessage());
            }
        }
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
