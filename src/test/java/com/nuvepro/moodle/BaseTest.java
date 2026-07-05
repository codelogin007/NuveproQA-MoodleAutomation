package com.nuvepro.moodle;

import com.microsoft.playwright.*;
import com.nuvepro.moodle.config.Settings;
import org.testng.annotations.*;

/**
 * TestNG lifecycle for Playwright — mirrors conftest.py's browser/page fixtures.
 * Playwright + Browser are created once per class; a fresh Context + Page per test method.
 */
public abstract class BaseTest {
    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeClass(alwaysRun = true)
    public void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(Settings.HEADLESS).setSlowMo(Settings.SLOWMO_MS));
    }

    @BeforeMethod(alwaysRun = true)
    public void newPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL)
                .setIgnoreHTTPSErrors(true));
        page = context.newPage();
        page.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
    }

    @AfterMethod(alwaysRun = true)
    public void closePage() {
        if (context != null) context.close();
    }

    @AfterClass(alwaysRun = true)
    public void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }
}
