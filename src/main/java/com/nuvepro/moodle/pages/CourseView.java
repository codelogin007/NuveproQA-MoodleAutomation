package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * The student's view of a course page (course/view.php) — used to verify sequential activity
 * completion: whether an activity is access-restricted, its restriction message, whether it is
 * marked complete, and the "Mark as done" manual-completion toggle. Selectors verified live.
 */
public class CourseView extends BasePage {
    public CourseView(Page page) {
        super(page);
    }

    public void open(String courseId) {
        navigate("/course/view.php?id=" + courseId);
        page.waitForTimeout(2_000);
    }

    /** The activity list item whose text contains {@code name}. */
    private Locator activity(String name) {
        return page.locator("li.activity, .activity-item")
                .filter(new Locator.FilterOptions().setHasText(name)).first();
    }

    /** True if the activity shows a restrict-access "Not available unless" message. */
    public boolean isRestricted(String name) {
        Locator info = activity(name).locator(".availabilityinfo");
        if (info.count() == 0) return false;
        return info.first().innerText().toLowerCase().contains("not available");
    }

    /** The restriction message text (empty if none). */
    public String restrictionMessage(String name) {
        Locator info = activity(name).locator(".availabilityinfo");
        return info.count() > 0 ? info.first().innerText().trim() : "";
    }

    /** True once the activity is marked complete (the manual toggle reads "Done", not "Mark as done"). */
    public boolean isCompleted(String name) {
        Locator toggle = activity(name).locator("[data-action='toggle-manual-completion']");
        if (toggle.count() > 0) {
            String t = toggle.first().innerText().trim().toLowerCase();
            String pressed = toggle.first().getAttribute("aria-pressed");
            return "true".equals(pressed) || (t.equals("done") || t.contains("done") && !t.contains("mark"));
        }
        // fall back to a completion-state indicator on the activity
        return activity(name).locator(".completion-dropdown .badge-success, "
                + "[data-region='completion-info'] .badge-success").count() > 0;
    }

    /** Click the activity's "Mark as done" manual-completion toggle. */
    public void markDone(String name) {
        Locator toggle = activity(name).locator("[data-action='toggle-manual-completion']");
        toggle.first().click();
        page.waitForTimeout(1_500);   // AJAX toggle
    }

    /** True if the activity is present and has a working view link (i.e. accessible/enabled). */
    public boolean isAccessible(String name) {
        return activity(name).count() > 0 && !isRestricted(name);
    }
}
