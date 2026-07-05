package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.PlaywrightAssertions;
import com.microsoft.playwright.options.LoadState;

/**
 * Playground (sandbox) LANDING page — first screen of .../mod/cloudlabs/view.php?id=&lt;cmid&gt;.
 * Selectors from Activity\cloudlabs\templates\playground-landing.mustache.
 * Mirrors pages/playground_landing.py.
 */
public class PlaygroundLanding extends BasePage {
    public static final String CONTAINER = ".playground-landing-panel";
    public static final String START_SANDBOX = "#np-ap-pl-playground-start";
    public static final String CONTINUE_LAB = ".np-ap-pl-continue";
    public static final String LAB_STATUS = "#lab-status-pl";
    public static final String MANAGE_LABS = "a[href*='managelabs.php']";
    public static final String H_SECTION_ID = "#np-ap-pl-sectionId";
    public static final String H_LAB_ID = "#np-ap-pl-labid";
    public static final String H_COURSE_ID = "#np-ap-pl-course-id";

    public PlaygroundLanding(Page page) {
        super(page);
    }

    public void open(int cmid) {
        // The test instance intermittently stalls on view.php; a fresh load recovers it, so retry once.
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                navigate("/mod/cloudlabs/view.php?id=" + cmid);
                page.locator(CONTAINER).first().waitFor(new Locator.WaitForOptions()
                        .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(45_000));
                return;
            } catch (Throwable e) {
                if (attempt == 2) throw e;
            }
        }
    }

    public boolean isLoaded() {
        return page.locator(CONTAINER).count() > 0;
    }

    public boolean hasActiveLab() {
        return page.locator(CONTINUE_LAB).count() > 0;
    }

    public String labStatusText() {
        return page.locator(LAB_STATUS).innerText().trim();
    }

    /** Click 'Start Sandbox' and confirm. WARNING: provisions a real cloud lab. */
    public void startSandbox(boolean confirm) {
        page.locator(START_SANDBOX).click();
        if (confirm) confirmModal();
    }

    public void continueLab() {
        page.locator(CONTINUE_LAB).click();
    }

    /** Reach the control panel regardless of lab state (Start Sandbox or Continue). */
    public void openLabConsole() {
        Locator start = page.locator(START_SANDBOX);
        if (start.count() > 0 && start.isVisible()) {
            startSandbox(true);
        } else if (page.locator(CONTINUE_LAB).count() > 0) {
            continueLab();
        } else {
            throw new AssertionError("Neither 'Start Sandbox' nor 'Continue' control is present");
        }
    }

    public void openManageLabs() {
        page.locator(MANAGE_LABS).click();
    }

    public String hiddenSectionId() {
        return hiddenInputValue(H_SECTION_ID);
    }

    public String hiddenCourseId() {
        return hiddenInputValue(H_COURSE_ID);
    }

    public void expectLoaded() {
        PlaywrightAssertions.assertThat(page.locator(CONTAINER)).isVisible();
    }

    public static final String DETAILS = "#np-ap-cl-challenge-details";

    public boolean hasStartSandbox() {
        Locator s = page.locator(START_SANDBOX);
        return s.count() > 0 && s.first().isVisible();
    }

    /** Click Start Sandbox and then Cancel the confirmation (no lab is created). */
    public void startSandboxThenCancel() {
        page.locator(START_SANDBOX).click();
        cancelModal();
    }
}
