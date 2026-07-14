package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.pages.MoodleSettings;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Core Moodle admin settings (Grade category/item settings, role gate). These pages are SHARED,
 * site-wide config — not a throwaway per-scenario resource like a cloudlabs lab — so every field a
 * scenario touches is restored to its pre-scenario value in @After, leaving the site as it was found.
 */
public class MoodleSettingsSteps {
    private final TestContext ctx;
    private final MoodleSettings settings;
    private final Map<String, String> originalValues = new LinkedHashMap<>();
    private String currentSection;
    private MoodleSettings studentSettings;

    public MoodleSettingsSteps(TestContext ctx) {
        this.ctx = ctx;
        this.settings = new MoodleSettings(ctx.page);
    }

    @Given("I open the {string} settings page")
    public void openSettingsPage(String section) {
        currentSection = section;
        settings.open(section);
        assertTrue(settings.formLoaded(), "settings form did not load for section=" + section);
    }

    @When("I set the {string} field to {string} and save")
    public void setFieldAndSave(String fieldId, String value) {
        originalValues.putIfAbsent(fieldId, settings.getValue(fieldId));
        settings.setValue(fieldId, value);
        settings.save();
    }

    @Then("the {string} field is {string}")
    public void fieldIs(String fieldId, String expected) {
        settings.open(currentSection);   // reload fresh to confirm the value actually persisted
        String actual = settings.getValue(fieldId);
        assertEquals(actual, expected, "field " + fieldId + " on section " + currentSection);
    }

    @Then("the {string} field offers options {string}")
    public void fieldOffersOptions(String fieldId, String csvExpected) {
        List<String> actualOptions = settings.optionTexts(fieldId);
        for (String want : csvExpected.split(",")) {
            String w = want.trim();
            boolean present = actualOptions.stream().anyMatch(o -> o.trim().equalsIgnoreCase(w));
            assertTrue(present, "expected option '" + w + "' not found in " + actualOptions);
        }
    }

    /** Substring match — for values Moodle renders with a non-breaking space (e.g. "Site upload limit (50 MB)"). */
    @Then("the {string} field contains {string}")
    public void fieldContains(String fieldId, String expectedSubstring) {
        settings.open(currentSection);
        String actual = settings.getValue(fieldId).replace(' ', ' ');   // normalise nbsp
        assertTrue(actual.contains(expectedSubstring),
                "field " + fieldId + " value '" + actual + "' did not contain '" + expectedSubstring + "'");
    }

    // ---- new-course propagation, boundaries, config log (course-dependent deferred cases) ----

    @Then("the {string} field offers no option above {int}")
    public void fieldOffersNoOptionAbove(String fieldId, int max) {
        for (String opt : settings.optionTexts(fieldId)) {
            String t = opt.trim();
            if (t.matches("\\d+") && Integer.parseInt(t) > max) {
                throw new AssertionError("field " + fieldId + " unexpectedly offers option " + t + " (> " + max + ")");
            }
        }
    }

    @Then("the add-new-course form in category {string} shows {string} for the {string} field")
    public void addCourseFormShows(String categoryId, String expected, String courseFieldId) {
        settings.openAddCourseForm(categoryId);
        assertEquals(settings.getValue(courseFieldId), expected,
                "add-new-course " + courseFieldId + " did not reflect the site default");
    }

    @Then("the existing course {string} still shows {string} for the {string} field")
    public void existingCourseStillShows(String courseId, String expected, String courseFieldId) {
        settings.openEditCourseForm(courseId);
        assertEquals(settings.getValue(courseFieldId), expected,
                "existing course " + courseId + " " + courseFieldId + " changed when it should not have");
    }

    @Then("the {string} field is not {string}")
    public void fieldIsNot(String fieldId, String notExpected) {
        settings.open(currentSection);
        assertTrue(!settings.getValue(fieldId).equals(notExpected),
                "field " + fieldId + " was unexpectedly '" + notExpected + "'");
    }

    @Then("the config change log records a change to {string}")
    public void configLogRecordsChange(String settingName) {
        settings.openConfigLog();
        assertTrue(settings.configLogHasRecent(settingName, 40),
                "config change log has no recent entry for '" + settingName + "'");
    }

    // ---- Running Banner front-end display (SET-74/76): NuveTheme notification banner ----

    @Then("the notification banner shows {string} across Moodle")
    public void theNotificationBannerShows(String text) {
        // Load a normal front-end page (dashboard) and confirm the enabled banner's text renders.
        ctx.page.navigate(com.nuvepro.moodle.config.Settings.BASE_URL + "/my/",
                new com.microsoft.playwright.Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED).setTimeout(30_000));
        ctx.page.waitForTimeout(1_500);
        String body = ctx.page.locator("body").innerText();
        assertTrue(body.contains(text), "notification banner text '" + text + "' not found on the dashboard");
    }

    // ---- config-gated skips (precondition not available on this site) ----

    @Then("the case is skipped because {string}")
    public void theCaseIsSkippedBecause(String reason) {
        throw new org.testng.SkipException(reason);
    }

    // ---- role gate (SET-79) ----

    @Given("I am logged in as a student")
    public void iAmLoggedInAsAStudent() {
        ctx.studentContext = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(com.nuvepro.moodle.config.Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        ctx.studentPage = ctx.studentContext.newPage();
        ctx.studentPage.setDefaultTimeout(com.nuvepro.moodle.config.Settings.DEFAULT_TIMEOUT_MS);
        Auth.loginAsStudent(ctx.studentPage);
    }

    @When("I open the {string} settings page as that student")
    public void iOpenTheSettingsPageAsThatStudent(String section) {
        studentSettings = new MoodleSettings(ctx.studentPage);
        studentSettings.open(section);
    }

    @Then("access is denied")
    public void accessIsDenied() {
        assertTrue(studentSettings.hasAccessError() || !studentSettings.formLoaded(),
                "student unexpectedly saw the settings form render");
    }

    /** Leave shared site config exactly as this scenario found it. */
    @After("@settings")
    public void restoreOriginalValues() {
        if (originalValues.isEmpty() || currentSection == null) return;
        settings.open(currentSection);
        for (Map.Entry<String, String> e : originalValues.entrySet()) {
            settings.setValue(e.getKey(), e.getValue());
        }
        settings.save();
        originalValues.clear();
    }
}
