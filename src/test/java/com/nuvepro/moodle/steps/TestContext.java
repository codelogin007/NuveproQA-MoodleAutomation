package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.PlaygroundControlPanel;
import com.nuvepro.moodle.pages.PlaygroundLanding;

/**
 * Per-scenario Playwright state, created by PicoContainer and shared across the step classes
 * of one scenario. A fresh BrowserContext is created from the run-wide browser using the
 * saved storageState, so the scenario is already logged in (no re-login).
 */
public class TestContext {
    public BrowserContext context;
    public Page page;
    public PlaygroundLanding landing;
    public PlaygroundControlPanel cp;
    public final int cmid = Integer.parseInt(Settings.PLAYGROUND_CMID);

    // transient per-scenario carry-state
    public String sectionId;
    public String newActivityCmid;
    public String labStatus = "";
    public Page openedTab;
    public BrowserContext studentContext;
    public Page studentPage;
    public String bulkAction;

    public void start() {
        context = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL)
                .setIgnoreHTTPSErrors(true)
                .setStorageStatePath(GlobalHooks.STORAGE_STATE));
        page = context.newPage();
        page.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        landing = new PlaygroundLanding(page);
        cp = new PlaygroundControlPanel(page);
    }

    public void stop() {
        if (openedTab != null) {
            try { openedTab.close(); } catch (Exception ignored) {}
        }
        if (studentContext != null) {
            try { studentContext.close(); } catch (Exception ignored) {}
        }
        if (context != null) context.close();
    }
}
