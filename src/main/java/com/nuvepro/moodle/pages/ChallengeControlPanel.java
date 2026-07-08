package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Assessment (challenge) control panel — selectors from challenge-controlpanel.mustache. Detection
 * is CONTROLS-based (cp.php may not expose a container class — the guided lesson). The lab is NOT
 * auto-provisioned: click the "Start hands-on lab" launch button. Lab-id hidden inputs are
 * server-rendered (stale) — callers read the lab id from admin Manage Labs instead.
 */
public class ChallengeControlPanel extends BasePage {
    public static final String SUBMIT = "#np-ap-cl-submit-attempt-btn";
    public static final String H_LAB_ID = "#np-ap-cl-labId, #np-ap-mod-labId";
    public static final String LAUNCH = "#np-ap-mod-access-launch-btn-acc, #np-ap-mod-access-launch-btn-vm, "
            + "button:has-text('Start hands-on lab'), a:has-text('Start hands-on lab')";
    public static final String ACTION_INPUT_DLG = "#np-ap-cl-actionInput-dlg";
    public static final String ACTION_INPUT_SUBMIT = "#np-ap-cl-actionInput-action";

    public ChallengeControlPanel(Page page) {
        super(page);
    }

    public boolean isShown() {
        try {
            Locator sub = page.locator(SUBMIT);
            return sub.count() > 0 && sub.first().isVisible();
        } catch (Throwable midNavigation) {
            return false;
        }
    }

    public String labId() {
        for (String sel : H_LAB_ID.split(",\\s*")) {
            Locator h = page.locator(sel);
            if (h.count() > 0) {
                String v = h.first().inputValue();
                if (v != null && !v.trim().isEmpty()) return v.trim();
            }
        }
        return null;
    }

    public void confirmAnyModal() {
        try { confirmModal(); } catch (Throwable ignored) {}
    }

    /** If no lab id yet, click the "Start hands-on lab" launch button (+ confirm) to provision. */
    public void startHandsOnLabIfNeeded() {
        String id = labId();
        if (id != null && !id.isEmpty()) return;
        Locator launch = page.locator(LAUNCH);
        for (int i = 0; i < Math.min(launch.count(), 4); i++) {
            if (launch.nth(i).isVisible()) {
                launch.nth(i).click();
                page.waitForTimeout(1_200);
                confirmAnyModal();
                return;
            }
        }
    }

    public boolean waitEnabled(String selector, int maxSeconds) {
        Locator l = page.locator(selector);
        for (int i = 0; i < maxSeconds; i++) {
            try { if (l.count() > 0 && l.first().isVisible() && l.first().isEnabled()) return true; }
            catch (Throwable ignored) {}
            page.waitForTimeout(1_000);
        }
        return false;
    }

    /** Submit the attempt for evaluation: click Submit, tick any ack box, confirm; if the
     *  action-input dialog appears, submit it too. */
    public void submitAttempt() {
        page.locator(SUBMIT).first().click();
        page.waitForTimeout(1_200);
        Locator ack = page.locator("#submitAttemptCheckbox, .modal.show input[type=checkbox]");
        if (ack.count() > 0) {
            try { ack.first().check(new Locator.CheckOptions().setForce(true)); } catch (Throwable ignored) {}
            page.waitForTimeout(400);
        }
        confirmAnyModal();
        page.waitForTimeout(1_500);
        Locator actionSubmit = page.locator(ACTION_INPUT_SUBMIT);
        if (actionSubmit.count() > 0 && actionSubmit.first().isVisible()) {
            actionSubmit.first().click();
            page.waitForTimeout(1_500);
        }
    }
}
