package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.pages.GuidedLanding;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.regex.Pattern;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Guided (practice project) lab steps — first batch: landing + admin + config presence checks
 * on an existing guided activity (GUIDED_CMID). No lab provisioning here.
 */
public class GuidedSteps {
    private final TestContext ctx;
    private GuidedLanding landing;
    private String sectionId;
    private ApiClient.SeededUser student;
    private BrowserContext studentCtx;
    private Page studentPage;

    public GuidedSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private int guidedCmid() {
        if (Settings.GUIDED_CMID.isEmpty()) throw new SkipException("GUIDED_CMID not set");
        return Integer.parseInt(Settings.GUIDED_CMID);
    }

    @Given("I open the guided activity")
    public void iOpenTheGuidedActivity() {
        landing = new GuidedLanding(ctx.page);
        landing.open(guidedCmid());
        sectionId = landing.sectionId();
    }

    @Then("the guided landing offers a start option")
    public void theGuidedLandingOffersAStartOption() {
        assertTrue(landing.hasStartOption(),
                "landing has no Start / Continue / New Attempt option");
    }

    @Then("the guided landing shows the project dates")
    public void theGuidedLandingShowsTheProjectDates() {
        String details = landing.detailsText().toLowerCase();
        assertTrue(details.contains("date") || Pattern.compile("\\d{1,2}\\s*\\w{3}").matcher(details).find()
                        || details.contains("time"),
                "landing details show no dates/time: " + details);
    }

    @Then("admin sees the guided manage buttons")
    public void adminSeesTheGuidedManageButtons() {
        assertTrue(landing.adminManageButtonsPresent(),
                "admin User Labs / User Guided Project buttons not present");
    }

    @When("I open the User Guided Project page")
    public void iOpenTheUserGuidedProjectPage() {
        ctx.page.navigate(Settings.BASE_URL + "/mod/cloudlabs/userguidedprojects.php?sectionid=" + sectionId,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(3_000);
    }

    @Then("the participants progress table is shown with date columns")
    public void theParticipantsProgressTableIsShown() {
        String tbl = "#np-ap-manage-user-guided-projects";
        assertTrue(ctx.page.locator(tbl).count() > 0, "participants guided-projects table not found");
        // Columns are defined as <th data-field="..."> in user-guided-projects.mustache.
        boolean userCol = ctx.page.locator(tbl + " th[data-field='firstname'], " + tbl + " th[data-field='email']").count() > 0;
        boolean dateCols = ctx.page.locator(tbl + " th[data-field='startdate'], " + tbl + " th[data-field='completeddate']").count() > 0;
        assertTrue(userCol, "participants table missing the user (firstname/email) column");
        assertTrue(dateCols, "participants table missing the start/completed date columns");
    }

    @When("I open the guided User Labs page")
    public void iOpenTheGuidedUserLabsPage() {
        ctx.page.navigate(Settings.BASE_URL + "/mod/cloudlabs/managelabs.php?sectionid=" + sectionId,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(3_000);
    }

    @Then("the guided labs are listed")
    public void theGuidedLabsAreListed() {
        assertTrue(ctx.page.locator("#np-ap-manage-lab-table").count() > 0,
                "guided Manage Labs table not found");
    }

    // ---- config (activity form) ----

    @When("I open a new guided activity form")
    public void iOpenANewGuidedActivityForm() {
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?add=cloudlabs&type=&course="
                        + Settings.COURSE_ID + "&section=0&return=0&sr=0",
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        // select the guided lab type so the guided-only fields (hideIf labtype ne guided) show
        var guided = ctx.page.locator("input[name='labtype'][value='guided']");
        if (guided.count() > 0 && !guided.first().isChecked()) {
            guided.first().check();
            ctx.page.waitForTimeout(1_500);
        }
    }

    @Then("the guided script URL and start and end date fields are present")
    public void theGuidedFormFieldsArePresent() {
        // guided script/progress-check URL (gated by practiceprojecttype=withprogresstracking, default)
        boolean scriptUrl = ctx.page.locator("#id_guidedprogresscheckurl, [name='guidedprogresscheckurl']").count() > 0;
        boolean startDate = ctx.page.locator("[id^='id_submissionfromdate']").count() > 0;
        boolean dueDate = ctx.page.locator("[id^='id_submissionduedate']").count() > 0;
        assertTrue(scriptUrl, "guided script/progress-check URL field not present");
        assertTrue(startDate && dueDate, "start/end date fields not present (start=" + startDate + " due=" + dueDate + ")");
    }

    // ---- CGAP-PGG-13/14: student role split ----

    @Given("a student is enrolled in the course")
    public void aStudentIsEnrolledInTheCourse() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        // The guided activity may live in a DIFFERENT course than COURSE_ID — enrol the student
        // into the guided activity's OWN course (read from the admin page's course-<id> body class).
        long courseId = guidedCourseId();
        student = ApiClient.createUser(System.currentTimeMillis());
        ApiClient.enrolUser(student.id, courseId, 5);
    }

    /** Course id of the guided activity, from the body class (course-<id>) on the admin's landing. */
    private long guidedCourseId() {
        landing = new GuidedLanding(ctx.page);
        landing.open(guidedCmid());
        String bodyClass = ctx.page.locator("body").getAttribute("class");
        java.util.regex.Matcher m = Pattern.compile("course-(\\d+)").matcher(bodyClass == null ? "" : bodyClass);
        if (!m.find()) throw new SkipException("could not determine the guided activity's course id");
        sectionId = landing.sectionId();
        return Long.parseLong(m.group(1));
    }

    @When("the student opens the guided activity")
    public void theStudentOpensTheGuidedActivity() {
        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        studentPage = studentCtx.newPage();
        studentPage.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(studentPage, student.username, student.password);
        new GuidedLanding(studentPage).open(guidedCmid());
    }

    @Then("the student does not see the guided admin manage buttons")
    public void theStudentDoesNotSeeAdminManageButtons() {
        assertTrue(studentPage.locator(GuidedLanding.CONTAINER).count() > 0,
                "guided landing did not load for the student");
        assertTrue(studentPage.locator(GuidedLanding.USER_LABS).count() == 0
                        && studentPage.locator(GuidedLanding.USER_GUIDED).count() == 0,
                "student sees admin manage buttons (User Labs / User Guided Project)");
    }

    @Then("the student cannot open the User Guided Project admin page")
    public void theStudentCannotOpenTheAdminPage() {
        String sid = studentPage.locator(GuidedLanding.H_SECTION_ID).count() > 0
                ? studentPage.locator(GuidedLanding.H_SECTION_ID).first().inputValue() : sectionIdFromAdmin();
        studentPage.navigate(Settings.BASE_URL + "/mod/cloudlabs/userguidedprojects.php?sectionid=" + sid,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        studentPage.waitForTimeout(2_000);
        assertTrue(studentPage.locator("#np-ap-manage-user-guided-projects").count() == 0,
                "student can see the admin participants table (capability not enforced)");
    }

    private String sectionIdFromAdmin() {
        landing = new GuidedLanding(ctx.page);
        landing.open(guidedCmid());
        return landing.sectionId();
    }

    // ---- CGAP-PGG-16/17: config toggles (on the add form; nothing is saved) ----

    private boolean visibleOnForm(String selector) {
        Locator l = ctx.page.locator(selector);
        return l.count() > 0 && l.first().isVisible();
    }

    private void pickRadio(String name, String value) {
        ctx.page.locator("input[type=radio][name='" + name + "'][value='" + value + "']").first()
                .check(new Locator.CheckOptions().setForce(true));
        ctx.page.waitForTimeout(800);   // let the hideIf rules re-evaluate
    }

    @Then("toggling progress tracking shows and hides the progress-check URL field")
    public void togglingProgressTracking() {
        String urlField = "[name='guidedprogresscheckurl']";
        pickRadio("practiceprojecttype", "withprogresstracking");
        assertTrue(visibleOnForm(urlField), "progress-check URL not shown for withprogresstracking");
        pickRadio("practiceprojecttype", "withoutprogresstracking");
        assertFalse(visibleOnForm(urlField), "progress-check URL still shown for withoutprogresstracking");
    }

    @Then("switching the guide content source toggles the git URL field")
    public void switchingGuideContentSource() {
        String gitField = "[name='guidecontentgiturl']";
        pickRadio("guidecontentsource", "git");
        assertTrue(visibleOnForm(gitField), "git URL field not shown for content source = git");
        pickRadio("guidecontentsource", "manual");
        assertFalse(visibleOnForm(gitField), "git URL field still shown for content source = manual");
    }

    // ---- CGAP-PGG-15: due date before start date is rejected (validation error -> nothing saved) ----

    @When("admin sets the guided due date before the start date")
    public void adminSetsDueBeforeStart() {
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?update=" + guidedCmid(),
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        Locator expand = ctx.page.locator("a.collapseexpand, .collapsible-actions a");
        if (expand.count() > 0) { try { expand.first().click(); ctx.page.waitForTimeout(600); } catch (Throwable ignored) {} }
        // enable both dates if they have enable checkboxes
        for (String f : new String[]{"submissionfromdate", "submissionduedate"}) {
            Locator en = ctx.page.locator("#id_" + f + "_enabled");
            if (en.count() > 0 && !en.first().isChecked()) en.first().check(new Locator.CheckOptions().setForce(true));
        }
        ctx.page.waitForTimeout(400);
        // due-year = from-year - 1  => guaranteed due < from
        String fromYear = ctx.page.locator("#id_submissionfromdate_year").inputValue();
        ctx.page.selectOption("#id_submissionduedate_year", String.valueOf(Integer.parseInt(fromYear) - 1));
        ctx.page.locator("#id_submitbutton").click();
        ctx.page.waitForTimeout(2_500);
    }

    @Then("the due-date validation error is shown")
    public void theDueDateValidationErrorIsShown() {
        // Validation failure keeps us on modedit and shows the error; nothing is saved.
        assertTrue(ctx.page.url().contains("modedit.php"),
                "form was ACCEPTED with due date before start date (left modedit) - validation missing!");
        boolean err = ctx.page.locator("[id^='id_error_submissionduedate']").count() > 0
                || ctx.page.locator("body").innerText().contains("due date before the from date");
        assertTrue(err, "no due-date validation error shown");
    }

    @After("@guidedgaps")
    public void cleanupGuidedGaps() {
        if (studentCtx != null) { try { studentCtx.close(); } catch (Throwable ignored) {} studentCtx = null; }
        if (student != null) { try { ApiClient.deleteUser(student.id); } catch (Throwable ignored) {} student = null; }
    }
}
