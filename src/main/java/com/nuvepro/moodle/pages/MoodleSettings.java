package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Core Moodle admin settings pages (/admin/settings.php?section=...) — NOT the cloudlabs plugin.
 * Covers Course default settings, Grade category/item settings, Activity chooser settings, and the
 * NuveTheme "Running Banner" setting. One generic Page Object because every admin_settingpage form
 * shares the same structure (label + input/select/textarea inside .form-item) — field ids below were
 * read directly off the live site, not guessed.
 * NOTE: the save button here is a plain {@code <button class="btn btn-primary">Save changes</button>}
 * with NO id — unlike moodleform-based pages (e.g. AdminUsers' #id_submitbutton). Verified live: the
 * admin nav also has a "Search" submit button, so the selector must match on text, not just type=submit.
 */
public class MoodleSettings extends BasePage {
    public static final String SUBMIT = "button:has-text('Save changes')";

    public MoodleSettings(Page page) {
        super(page);
    }

    public void open(String section) {
        navigate("/admin/settings.php?section=" + section);
        page.waitForTimeout(800);
    }

    // ---- course forms (for verifying a site default propagates to NEW vs EXISTING courses) ----
    // The add-new-course / edit-course forms mirror the site defaults; field ids are the plain
    // moodleform ids (id_format, id_visible, id_numsections, id_lang, ...), not the s_moodlecourse_* ids.

    /** Open the "Add a new course" form under a category (fields pre-filled from the site defaults). */
    public void openAddCourseForm(String categoryId) {
        navigate("/course/edit.php?category=" + categoryId);
        page.locator("#id_fullname, #id_format").first().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(30_000));
    }

    /** Open an EXISTING course's edit-settings form by course id. */
    public void openEditCourseForm(String courseId) {
        navigate("/course/edit.php?id=" + courseId);
        page.locator("#id_fullname, #id_format").first().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(30_000));
    }

    // ---- config change log (Site admin > Reports > Config changes) ----

    public void openConfigLog() {
        navigate("/report/configlog/index.php");
        page.locator("table").first().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(30_000));
        page.waitForTimeout(500);
    }

    /** True if a recent config-log row mentions this setting name (columns: Plugin/Setting/New/Original). */
    public boolean configLogHasRecent(String settingName, int scanRows) {
        Locator rows = page.locator("table tbody tr");
        int n = Math.min(rows.count(), scanRows);
        for (int i = 0; i < n; i++) {
            if (rows.nth(i).innerText().contains(settingName)) return true;
        }
        return false;
    }

    /** True if a non-admin hit the Moodle permission wall instead of the settings form. */
    public boolean hasAccessError() {
        return page.locator(".errorbox, .alert-danger").count() > 0
                || page.locator("#adminsettings .form-item").count() == 0
                    && page.locator("body").innerText().toLowerCase().contains("error");
    }

    public boolean formLoaded() {
        return page.locator("#adminsettings .form-item, .form-item").count() > 0;
    }

    // ---- select fields (Moodle admin_setting_configselect) ----

    public String selectedText(String fieldId) {
        Locator sel = page.locator("#" + fieldId);
        return (String) sel.evaluate("e => e.options[e.selectedIndex] ? e.options[e.selectedIndex].text.trim() : ''");
    }

    public void selectByLabel(String fieldId, String visibleText) {
        page.locator("#" + fieldId).selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(visibleText));
    }

    public java.util.List<String> optionTexts(String fieldId) {
        return page.locator("#" + fieldId + " option").allInnerTexts();
    }

    // ---- text fields (Moodle admin_setting_configtext) ----

    public String textValue(String fieldId) {
        return page.locator("#" + fieldId).inputValue();
    }

    public void setText(String fieldId, String value) {
        Locator f = page.locator("#" + fieldId);
        try {
            f.fill(value, new Locator.FillOptions().setTimeout(3_000));
        } catch (RuntimeException notEditable) {
            // Some admin text fields are hidden by a hideIf dependency (e.g. the NuveTheme banner text is
            // hidden until the banner checkbox is on). A hidden field still submits, so set it via JS.
            f.evaluate("(el, v) => { el.value = v;"
                    + " el.dispatchEvent(new Event('input', {bubbles:true}));"
                    + " el.dispatchEvent(new Event('change', {bubbles:true})); }", value);
        }
    }

    // ---- checkboxes (Moodle admin_setting_configcheckbox: hidden default + real checkbox, same id) ----

    public boolean isChecked(String fieldId) {
        return page.locator("#" + fieldId).isChecked();
    }

    /**
     * Set a checkbox to an absolute state. Most Moodle admin checkboxes are visible and check()/uncheck()
     * work, but some (e.g. the NuveTheme banner toggle) are rendered as a styled control with the real
     * input hidden — for those, set the property deterministically via JS and fire the change event so
     * Moodle's hideIf dependencies update. NOTE: deliberately does NOT early-return on a pre-read of
     * isChecked() — right after open() that read can be stale, which previously caused a restore to skip
     * unchecking and leave the site-wide banner ON.
     */
    public void setChecked(String fieldId, boolean checked) {
        Locator cb = page.locator("#" + fieldId);
        boolean visible;
        try { visible = cb.isVisible(); } catch (RuntimeException e) { visible = false; }
        if (visible) {
            if (checked) cb.check(); else cb.uncheck();
        } else {
            cb.evaluate("(el, want) => { el.checked = want; el.removeAttribute('checked');"
                    + " el.dispatchEvent(new Event('change', {bubbles:true})); }", checked);
        }
    }

    public void save() {
        page.locator(SUBMIT).click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1_200);
    }

    // ---- generic dispatch: one step vocabulary for select / checkbox / text fields ----

    /** Read a field's current value regardless of control type ("Yes"/"No" for checkboxes). */
    public String getValue(String fieldId) {
        Locator ctrl = page.locator("#" + fieldId);
        String tag = (String) ctrl.evaluate("e => e.tagName");
        if ("SELECT".equals(tag)) return selectedText(fieldId);
        if ("checkbox".equals(ctrl.getAttribute("type"))) return isChecked(fieldId) ? "Yes" : "No";
        return textValue(fieldId);
    }

    /** Set a field's value regardless of control type ("Yes"/"No" for checkboxes) and save. */
    public void setValue(String fieldId, String value) {
        Locator ctrl = page.locator("#" + fieldId);
        String tag = (String) ctrl.evaluate("e => e.tagName");
        if ("SELECT".equals(tag)) {
            selectByLabel(fieldId, value);
        } else if ("checkbox".equals(ctrl.getAttribute("type"))) {
            setChecked(fieldId, "Yes".equalsIgnoreCase(value));
        } else {
            setText(fieldId, value);
        }
    }
}
