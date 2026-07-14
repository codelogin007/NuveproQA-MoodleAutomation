package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Admin course + activity authoring, used to build the Sequential Activity Completion fixture:
 * create a course (completion defaults on), add URL activities (a lightweight module that needs only
 * a name + external URL — no rich-text editor), set an activity's completion to "students manually
 * mark as done", and restrict one activity on another's completion via the availabilityconditionsjson
 * hidden field (robust vs. driving the JS restrict-access builder). Selectors verified live.
 */
public class CourseAdmin extends BasePage {
    public CourseAdmin(Page page) {
        super(page);
    }

    /** Create a course under category 1 and return its id. Completion tracking defaults to on. */
    public String createCourse(String fullName, String shortName) {
        navigate("/course/edit.php?category=1");
        page.locator("#id_fullname").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        page.locator("#id_fullname").fill(fullName);
        page.locator("#id_shortname").fill(shortName);
        page.locator("#id_saveanddisplay").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2_500);
        String id = param(page.url(), "id");
        if (id == null) throw new IllegalStateException("createCourse: no course id in URL " + page.url());
        return id;
    }

    /**
     * Add a URL activity to section 1. If {@code manualCompletion} the activity gets "students can
     * manually mark as done"; if {@code availabilityJson} is non-null it is set as the restrict-access
     * condition. Returns the new activity's cmid.
     */
    public String addUrlActivity(String courseId, String name, boolean manualCompletion, String availabilityJson) {
        navigate("/course/modedit.php?add=url&type=&course=" + courseId + "&section=1&return=0&sr=0");
        page.locator("#id_name").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        expandAll();
        page.locator("#id_name").fill(name);
        page.locator("#id_externalurl").fill("https://example.com");
        if (manualCompletion) {
            // The Activity completion radios live in a (possibly collapsed) fieldset; set via JS so
            // visibility never blocks it. id_completion_1 = "Students can manually mark as complete".
            page.locator("#id_completion_1").evaluate(
                    "el => { el.checked = true; el.dispatchEvent(new Event('change', {bubbles:true})); }");
        }
        if (availabilityJson != null) {
            page.locator("[name='availabilityconditionsjson']").evaluate("(el, v) => { el.value = v; }", availabilityJson);
        }
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2_000);
        String cmid = param(page.url(), "id");
        if (cmid == null) {
            throw new IllegalStateException("addUrlActivity '" + name + "' did not save (stayed on "
                    + page.url() + ") — likely a validation error");
        }
        return cmid;
    }

    /** availabilityconditionsjson for "activity {cmid} must be marked complete" (e=1), shown greyed. */
    public static String requireCompleteJson(String dependsOnCmid) {
        return "{\"op\":\"&\",\"c\":[{\"type\":\"completion\",\"cm\":" + dependsOnCmid + ",\"e\":1}],\"showc\":[true]}";
    }

    /** Activity 1 completion condition for the grade-based sequential cases. */
    public enum GradeCompletion { ANY_GRADE, PASSING_GRADE }

    /**
     * Add an Assignment whose completion is grade-based (automatic completion + "require grade", and for
     * PASSING_GRADE also "require passing grade" with the given pass mark). Optionally restrict-access
     * via availabilityJson. Completion sub-options are set via JS (they live in a collapsed fieldset).
     * Returns the new activity's cmid.
     */
    public String addGradedAssignment(String courseId, String name, GradeCompletion cond,
                                      int passMark, String availabilityJson) {
        navigate("/course/modedit.php?add=assign&type=&course=" + courseId + "&section=1&return=0&sr=0");
        page.locator("#id_name").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        page.locator("#id_name").fill(name);
        // automatic completion + require grade
        page.locator("#id_completion_2").evaluate(
                "el => { el.checked = true; el.dispatchEvent(new Event('change', {bubbles:true})); }");
        page.locator("#id_completionusegrade").evaluate(
                "el => { el.checked = true; el.dispatchEvent(new Event('change', {bubbles:true})); }");
        if (cond == GradeCompletion.PASSING_GRADE) {
            page.locator("#id_completionpassgrade_1").evaluate(
                    "el => { el.checked = true; el.dispatchEvent(new Event('change', {bubbles:true})); }");
            page.locator("#id_gradepass").evaluate("(el, v) => { el.value = v; }", String.valueOf(passMark));
        }
        if (availabilityJson != null) {
            page.locator("[name='availabilityconditionsjson']").evaluate("(el, v) => { el.value = v; }", availabilityJson);
        }
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2_000);
        String cmid = param(page.url(), "id");
        if (cmid == null) {
            throw new IllegalStateException("addGradedAssignment '" + name + "' did not save (stayed on "
                    + page.url() + ")");
        }
        return cmid;
    }

    /**
     * Grade a student on an Assignment THROUGH the assignment grader (not the gradebook). A gradebook
     * override does NOT satisfy the assignment's "require grade" completion (that checks for a real assign
     * grade), so we drive the grader form: navigate to action=grader for the user, fill #id_grade, save.
     * The grader is an async panel, so we wait for #id_grade to render.
     */
    public void gradeAssignment(String assignmentCmid, long userId, double grade) {
        navigate("/mod/assign/view.php?id=" + assignmentCmid + "&action=grader&userid=" + userId);
        Locator gradeInput = page.locator("#id_grade");
        gradeInput.waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
        gradeInput.fill(String.valueOf(grade));
        Locator save = page.locator("#id_savechanges");
        if (save.count() == 0) save = page.locator("button:has-text('Save changes'), input[value='Save changes']");
        save.first().click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(2_500);
    }

    /** Delete a course (best-effort), confirming the Moodle prompt. */
    public void deleteCourse(String courseId) {
        navigate("/course/delete.php?id=" + courseId);
        page.waitForTimeout(1_200);
        Locator confirm = page.locator("button:has-text('Delete'), input[value='Delete'], "
                + "#region-main .btn-primary, button:has-text('Continue')");
        if (confirm.count() > 0 && confirm.first().isVisible()) {
            confirm.first().click();
            page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
            page.waitForTimeout(2_500);
        }
    }

    private void expandAll() {
        Locator exp = page.locator("a.collapseexpand[aria-expanded='false'], a:has-text('Expand all')");
        try {
            if (exp.count() > 0) { exp.first().click(); page.waitForTimeout(500); }
        } catch (RuntimeException ignored) {}
    }

    private static String param(String url, String key) {
        Matcher m = Pattern.compile("[?&]" + key + "=([0-9]+)").matcher(url);
        return m.find() ? m.group(1) : null;
    }
}
