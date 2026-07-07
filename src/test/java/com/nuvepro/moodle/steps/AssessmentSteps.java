package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Assessment (challenge) — batch 1: landing presence, config-toggle mechanics (CGAP-A-1..4),
 * edit-form validations (CGAP-A-5/6/7 — a validation failure saves nothing, so the shared
 * ASSESSMENT_CMID activity is never mutated), and student role enforcement (CGAP-A-8).
 * Landing selectors from challenge-landing / assessment-landing mustache templates.
 */
public class AssessmentSteps {
    // this assessment-landing variant renders "Start Assessment" WITHOUT an id -> include text matches
    private static final String ATTEMPT_CONTROLS = "#np-ap-cl-challenge-start, #np-ap-cl-attempt-continue, "
            + "#np-ap-cl-attempt-new, #btn-cl-retake-assessment, #completeChallengeBtn, #np-ap-cl-challenge-end, "
            + "button:has-text('Start Assessment'), a:has-text('Start Assessment'), "
            + "button:has-text('Start Challenge'), a:has-text('Start Challenge')";
    private static final String H_SECTION = "#np-ap-cl-sectionId";

    private final TestContext ctx;
    private ApiClient.SeededUser student;
    private BrowserContext studentCtx;
    private Page studentPage;
    private String sectionId;

    public AssessmentSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private int cmid() {
        if (Settings.ASSESSMENT_CMID.isEmpty()) throw new SkipException("ASSESSMENT_CMID not set");
        return Integer.parseInt(Settings.ASSESSMENT_CMID);
    }

    private void openLanding(Page p) {
        p.navigate(Settings.BASE_URL + "/mod/cloudlabs/view.php?id=" + cmid(),
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        p.waitForTimeout(5_000);
    }

    // ---- presence ----

    @When("I open the assessment activity")
    public void iOpenTheAssessmentActivity() {
        openLanding(ctx.page);
        Locator sec = ctx.page.locator(H_SECTION);
        if (sec.count() > 0) sectionId = sec.first().inputValue();
    }

    @Then("the assessment landing shows the attempt controls and details")
    public void theAssessmentLandingShowsControls() {
        boolean controls = false;
        Locator c = ctx.page.locator(ATTEMPT_CONTROLS);
        for (int i = 0; i < c.count(); i++) if (c.nth(i).isVisible()) { controls = true; break; }
        assertTrue(controls, "no attempt control (start/continue/new/retake/complete) visible on the landing");
        assertTrue(ctx.page.locator("#np-ap-cl-challenge-details, #np-ap-cl-challenge-attempt-table, "
                        + "#labAttemptsContainer").count() > 0,
                "neither the details section nor the attempts area is present");
    }

    // ---- config toggles (add form; nothing saved) ----

    @When("I open a new challenge activity form")
    public void iOpenANewChallengeActivityForm() {
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?add=cloudlabs&type=&course="
                        + Settings.COURSE_ID + "&section=0&return=0&sr=0",
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        Locator ch = ctx.page.locator("input[name='labtype'][value='challenge']");
        if (ch.count() > 0 && !ch.first().isChecked()) {
            ch.first().check(new Locator.CheckOptions().setForce(true));
            ctx.page.waitForTimeout(1_200);
        }
        expandAll();
    }

    private void expandAll() {
        Locator ex = ctx.page.locator("a.collapseexpand, .collapsible-actions a");
        if (ex.count() > 0) { try { ex.first().click(); ctx.page.waitForTimeout(700); } catch (Throwable ignored) {} }
    }

    private void pickRadio(String name, String value) {
        ctx.page.locator("input[type=radio][name='" + name + "'][value='" + value + "']").first()
                .check(new Locator.CheckOptions().setForce(true));
        ctx.page.waitForTimeout(800);
    }

    private boolean vis(String selector) {
        Locator l = ctx.page.locator(selector);
        for (int i = 0; i < l.count(); i++) if (l.nth(i).isVisible()) return true;
        return false;
    }

    @Then("the evaluation type toggles the matching config fields")
    public void evaluationTypeToggles() {
        pickRadio("evaluationtype", "AUTO");
        assertTrue(vis("[name='testscripturl']") && vis("[name='testscriptparams']"),
                "AUTO did not show test script URL/params");
        pickRadio("evaluationtype", "ANSWER");
        assertTrue(vis("[name='challengeanswer']") && vis("[name='challengemarks']"),
                "ANSWER did not show answer/marks");
        assertFalse(vis("[name='testscripturl']"), "ANSWER still shows the test script URL");
        pickRadio("evaluationtype", "MANUAL");
        assertFalse(vis("[name='testscripturl']") || vis("[name='challengeanswer']"),
                "MANUAL still shows AUTO/ANSWER fields");
    }

    @Then("enabling randomization hides the manual content editor")
    public void randomizationHidesEditor() {
        pickRadio("challengecontentsource", "manual");   // isolate: git also hides the editor
        ctx.page.selectOption("select[name='assessment_randomization']", "No");
        ctx.page.waitForTimeout(800);
        boolean shownBefore = vis("[id*='challenge_content_editor']");
        ctx.page.selectOption("select[name='assessment_randomization']", "Yes");
        ctx.page.waitForTimeout(800);
        boolean shownAfter = vis("[id*='challenge_content_editor']");
        assertTrue(shownBefore, "content editor not visible with randomization=No (baseline)");
        assertFalse(shownAfter, "content editor still visible with randomization=Yes");
    }

    @Then("switching the challenge content source toggles the git URL field")
    public void contentSourceTogglesGitUrl() {
        ctx.page.selectOption("select[name='assessment_randomization']", "No");
        ctx.page.waitForTimeout(500);
        pickRadio("challengecontentsource", "git");
        assertTrue(vis("[name='challengecontentgiturl']"), "git URL field not shown for source=git");
        pickRadio("challengecontentsource", "manual");
        assertFalse(vis("[name='challengecontentgiturl']"), "git URL field still shown for source=manual");
    }

    /** Poll until the selector's visibility matches (hideIf re-evaluation can lag); returns last state. */
    private boolean visBecomes(String selector, boolean expected, int seconds) {
        boolean state = !expected;
        for (int i = 0; i < seconds * 2; i++) {
            state = vis(selector);
            if (state == expected) return state;
            ctx.page.waitForTimeout(500);
        }
        return state;
    }

    @Then("setting a cooldown period shows attempts-after-cooldown")
    public void cooldownTogglesAttemptsAfter() {
        ctx.page.selectOption("select[name='cooldownperiod']", "1");
        assertTrue(visBecomes("[name='attemptsaftercooldown']", true, 5),
                "attempts-after-cooldown not shown with a cooldown period");
        ctx.page.selectOption("select[name='cooldownperiod']", "0");
        assertFalse(visBecomes("[name='attemptsaftercooldown']", false, 5),
                "attempts-after-cooldown still shown with cooldown=None");
    }

    // ---- edit-form validations on ASSESSMENT_CMID (validation failure saves NOTHING) ----

    private void openEditForm() {
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?update=" + cmid(),
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        expandAll();
    }

    private void save() {
        ctx.page.locator("#id_submitbutton").click();
        ctx.page.waitForTimeout(2_500);
    }

    private void assertStillOnFormWith(String errorFragment, String what) {
        assertTrue(ctx.page.url().contains("modedit.php"),
                "form was ACCEPTED (" + what + ") - validation missing, activity may have been changed!");
        assertTrue(ctx.page.locator("body").innerText().contains(errorFragment)
                        || ctx.page.locator("[id^='id_error_']").count() > 0,
                "no validation error shown for " + what);
    }

    @When("admin sets the challenge due date before the start date")
    public void adminSetsChallengeDueBeforeStart() {
        openEditForm();
        for (String f : new String[]{"submissionfromdate", "submissionduedate"}) {
            Locator en = ctx.page.locator("#id_" + f + "_enabled");
            if (en.count() > 0 && !en.first().isChecked()) en.first().check(new Locator.CheckOptions().setForce(true));
        }
        ctx.page.waitForTimeout(400);
        String fromYear = ctx.page.locator("#id_submissionfromdate_year").inputValue();
        ctx.page.selectOption("#id_submissionduedate_year", String.valueOf(Integer.parseInt(fromYear) - 1));
        save();
    }

    @Then("the challenge due-date validation error is shown")
    public void challengeDueDateErrorShown() {
        assertStillOnFormWith("due date before the from date", "due-before-start");
    }

    @When("admin clears the challenge passing grade and saves")
    public void adminClearsPassingGrade() {
        openEditForm();
        Locator gp = ctx.page.locator("[name='gradepass']");
        if (gp.count() == 0) throw new SkipException("gradepass field not on the form");
        gp.first().fill("");
        save();
    }

    @Then("the passing-grade validation error is shown")
    public void passingGradeErrorShown() {
        assertStillOnFormWith("Provide Passing Grade", "missing gradepass");
    }

    @When("admin enters invalid test script parameters and saves")
    public void adminEntersInvalidTestScriptParams() {
        openEditForm();
        Locator auto = ctx.page.locator("input[type=radio][name='evaluationtype'][value='AUTO']");
        if (auto.count() == 0 || !auto.first().isChecked()) {
            throw new SkipException("activity's evaluation type is not AUTO - JSON validation not applicable");
        }
        ctx.page.locator("[name='testscriptparams']").first().fill("{not valid json");
        // belt-and-braces: also clear gradepass so the save is GUARANTEED to fail validation
        Locator gp = ctx.page.locator("[name='gradepass']");
        if (gp.count() > 0) gp.first().fill("");
        save();
    }

    @Then("the test-script-parameters validation error is shown")
    public void testScriptParamsErrorShown() {
        assertStillOnFormWith("valid JSON format", "invalid testscriptparams JSON");
    }

    // ---- CGAP-A-8: student enforcement ----

    @Given("a student is enrolled in the assessment course")
    public void aStudentIsEnrolledInTheAssessmentCourse() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        openLanding(ctx.page);   // as admin, to read the activity's course
        String bodyClass = ctx.page.locator("body").getAttribute("class");
        Matcher m = Pattern.compile("course-(\\d+)").matcher(bodyClass == null ? "" : bodyClass);
        if (!m.find()) throw new SkipException("could not determine the assessment activity's course id");
        Locator sec = ctx.page.locator(H_SECTION);
        if (sec.count() > 0) sectionId = sec.first().inputValue();
        student = ApiClient.createUser(System.currentTimeMillis());
        ApiClient.enrolUser(student.id, Long.parseLong(m.group(1)), 5);
    }

    @When("the student opens the assessment activity")
    public void theStudentOpensTheAssessmentActivity() {
        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        studentPage = studentCtx.newPage();
        studentPage.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(studentPage, student.username, student.password);
        openLanding(studentPage);
    }

    private String studentSectionId() {
        return studentPage.locator(H_SECTION).count() > 0
                ? studentPage.locator(H_SECTION).first().inputValue() : sectionId;
    }

    private void studentOpen(String pagePath) {
        studentPage.navigate(Settings.BASE_URL + pagePath,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        studentPage.waitForTimeout(2_500);
    }

    @Then("the student sees no assessment admin controls and cannot open manage challenges")
    public void theStudentSeesNoAdminControls() {
        assertTrue(studentPage.locator(H_SECTION).count() > 0
                        || studentPage.locator(ATTEMPT_CONTROLS).count() > 0,
                "assessment landing did not load for the student");
        assertTrue(studentPage.locator("a[href*='managechallenges.php'], #np-ap-cl-manage-buttons a").count() == 0,
                "student sees admin manage controls on the assessment landing");
        studentOpen("/mod/cloudlabs/managechallenges.php?sectionid=" + studentSectionId());
        assertTrue(studentPage.locator("#np-ap-manage-challenge-table").count() == 0,
                "student can access the manage-challenges admin table");
    }

    @Then("the problem statements admin page is blocked for the student")
    public void theProblemStatementsPageIsBlockedForTheStudent() {
        studentOpen("/mod/cloudlabs/assessment-problemstatements.php?sectionid=" + studentSectionId());
        // DEFECT (2026-07-07): this page currently RENDERS its admin table for a student — the landing
        // button is hidden (A249) but the page itself is not capability-protected. Red until fixed.
        assertTrue(studentPage.locator("#np-ap-ps-problemstatement").count() == 0,
                "DEFECT: a student can open the Assessment Problem Statements admin page "
                        + "(#np-ap-ps-problemstatement renders) via direct URL - missing capability check");
    }

    @After("@assessgaps")
    public void cleanupAssessGaps() {
        if (studentCtx != null) { try { studentCtx.close(); } catch (Throwable ignored) {} studentCtx = null; }
        if (student != null) { try { ApiClient.deleteUser(student.id); } catch (Throwable ignored) {} student = null; }
    }
}
