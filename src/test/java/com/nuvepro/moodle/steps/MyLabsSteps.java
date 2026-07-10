package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.function.Consumer;

import static org.testng.Assert.assertTrue;

/**
 * My Labs (theme_nuvetheme, /theme/nuvetheme/mylabs.php) — batch 1, no provisioning. Lab cards
 * (.course-card) + search, unauthenticated -> login redirect, and search XSS-safety. The page is
 * require_login and user-scoped (each user sees their own labs).
 */
public class MyLabsSteps {
    private static final String URL = "/theme/nuvetheme/mylabs.php";
    private static final String CARD = ".course-card";

    private final TestContext ctx;
    private BrowserContext anonCtx;

    public MyLabsSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private void open(Page p) {
        p.navigate(Settings.BASE_URL + URL,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        p.waitForTimeout(3_000);
    }

    private Locator searchField() {
        Locator cands = ctx.page.locator("#region-main input[type='search'], "
                + "#region-main input[placeholder*='ab'], #region-main input[placeholder*='earch']");
        for (int i = 0; i < cands.count(); i++)
            if (cands.nth(i).isVisible()) return cands.nth(i);
        return null;
    }

    @When("admin opens the My Labs page")
    public void adminOpensTheMyLabsPage() {
        open(ctx.page);
    }

    @Then("the My Labs page shows lab cards and a search field")
    public void theMyLabsPageShowsCardsAndSearch() {
        assertTrue(ctx.page.locator("body").innerText().contains("My Labs"), "My Labs heading not present");
        assertTrue(ctx.page.locator(CARD).count() > 0, "no lab cards (.course-card) on My Labs");
        assertTrue(searchField() != null, "no search field on My Labs");
    }

    @Then("an unauthenticated user is redirected to login from My Labs")
    public void anUnauthenticatedUserIsRedirected() {
        anonCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page an = anonCtx.newPage();
        an.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        an.navigate(Settings.BASE_URL + URL,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        an.waitForTimeout(2_500);
        assertTrue(an.url().contains("login"),
                "unauthenticated access to My Labs was not redirected to login (url=" + an.url() + ")");
    }

    @Then("entering a script payload in the My Labs search does not execute it")
    public void scriptPayloadIsNotExecuted() {
        final boolean[] alertFired = {false};
        Consumer<Dialog> handler = d -> { alertFired[0] = true; d.accept(); };
        ctx.page.onDialog(handler);
        try {
            Locator search = searchField();
            assertTrue(search != null, "no search field on My Labs");
            String payload = "<script>alert('xss')</script>";
            search.fill(payload);
            search.press("Enter");
            ctx.page.waitForTimeout(1_500);
            assertTrue(!alertFired[0],
                    "XSS: a script payload entered in the My Labs search EXECUTED (alert fired)");
            // the field keeps the raw text (treated as data, not markup)
            assertTrue(payload.equals(search.inputValue()) || search.inputValue().contains("script"),
                    "search field did not retain the payload as text");
        } finally {
            ctx.page.offDialog(handler);
        }
    }

    @After("@mylabs")
    public void cleanup() {
        if (anonCtx != null) { try { anonCtx.close(); } catch (Throwable ignored) {} anonCtx = null; }
    }
}
