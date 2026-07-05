package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;

/**
 * Shared Page Object helpers. Locator discipline lives here:
 * prefer stable author id/class -> role -> label -> text.
 * Mirrors pages/base_page.py.
 */
public abstract class BasePage {
    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
        this.page.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
    }

    public Page page() {
        return page;
    }

    protected void navigate(String path) {
        // Use DOMCONTENTLOADED (not default LOAD): cloudlabs activity pages run lab-status/countdown
        // web-workers that poll forever, so the page never reaches 'load'/networkidle.
        // Retry on transient network blips (ERR_CONNECTION_TIMED_OUT / reset) — the site occasionally
        // drops a single request under load.
        com.microsoft.playwright.Page.NavigateOptions opts = new com.microsoft.playwright.Page.NavigateOptions()
                .setTimeout(45_000)
                .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED);
        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                page.navigate(Settings.BASE_URL + path, opts);
                return;
            } catch (RuntimeException e) {
                last = e;
                page.waitForTimeout(3_000);   // brief backoff, then retry
            }
        }
        throw last;
    }

    protected Locator byCss(String selector) {
        return page.locator(selector);
    }

    protected String hiddenInputValue(String cssId) {
        return page.locator(cssId).inputValue();
    }

    /**
     * Click the primary/confirm button of Moodle's *visible* confirm modal, regardless of
     * its label (Start="Yes", Stop="Stop", ...). Targets the live dialog by visibility because
     * CSS data-action selectors also match hidden modal duplicates in Moodle 4.5.
     */
    protected void confirmModal() {
        // Poll until a VISIBLE modal-container's confirm button is clickable. Avoids a race where
        // stale (hidden) modal duplicates are already attached but the real dialog hasn't rendered.
        for (int i = 0; i < 50; i++) { // up to ~25s (test instance can be slow)
            Object res = page.evaluate(CONFIRM_JS);
            if ("ok".equals(res)) return;
            page.waitForTimeout(500);
        }
        throw new AssertionError("confirmModal: no visible confirm dialog appeared in time");
    }

    /** Click the Cancel/secondary button of the visible confirm modal. */
    protected void cancelModal() {
        for (int i = 0; i < 20; i++) {
            Object res = page.evaluate(CANCEL_JS);
            if ("ok".equals(res)) return;
            page.waitForTimeout(300);
        }
        throw new AssertionError("cancelModal: no visible confirm dialog appeared");
    }

    private static final String CANCEL_JS =
        "() => {" +
        "  const cont = [...document.querySelectorAll('[data-region=\"modal-container\"]')]" +
        "     .find(c => c.getClientRects().length > 0 && c.getAttribute('aria-hidden') !== 'true');" +
        "  if (!cont) return 'no-modal';" +
        "  const btn = cont.querySelector('[data-action=\"cancel\"]') || cont.querySelector('.btn-secondary');" +
        "  if (!btn) return 'no-btn';" +
        "  btn.click();" +
        "  return 'ok';" +
        "}";

    private static final String CONFIRM_JS =
        "() => {" +
        "  const cont = [...document.querySelectorAll('[data-region=\"modal-container\"]')]" +
        "     .find(c => c.getClientRects().length > 0 && c.getAttribute('aria-hidden') !== 'true');" +
        "  if (!cont) return 'no-modal';" +
        "  const btn = cont.querySelector('[data-action=\"save\"]') || cont.querySelector('.btn-primary');" +
        "  if (!btn) return 'no-btn';" +
        "  btn.click();" +
        "  return 'ok';" +
        "}";
}
