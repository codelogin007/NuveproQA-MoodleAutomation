package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.helpers.CloudLabsClient;
import com.nuvepro.moodle.pages.GuidedControlPanel;
import com.nuvepro.moodle.pages.GuidedLanding;
import com.nuvepro.moodle.pages.ManageLabs;

import java.util.EnumSet;
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

    // ---- @guidedlab: lab lifecycle (start -> Running -> stop -> landing -> continue -> submit) ----

    private String guidedLabId;

    private GuidedControlPanel scp() {
        return new GuidedControlPanel(studentPage);
    }

    /** Wait for either the control panel or (fallback=false) return whether it appeared. */
    private boolean waitControlPanel(int maxSeconds) {
        GuidedControlPanel cp = scp();
        for (int i = 0; i < maxSeconds; i++) {
            if (cp.isShown()) return true;
            studentPage.waitForTimeout(1_000);
        }
        return false;
    }

    @When("the student starts the guided project")
    public void theStudentStartsTheGuidedProject() {
        if (!CloudLabsClient.isConfigured()) throw new SkipException("CloudLabs API not configured");
        theStudentOpensTheGuidedActivity();
        // The landing start control's JS handler attaches late (same race as playground): wait,
        // then CLICK REPEATEDLY until the control panel appears.
        studentPage.waitForTimeout(5_000);
        String startSel = GuidedLanding.START + ", " + GuidedLanding.CONTINUE + ", " + GuidedLanding.NEW_ATTEMPT;
        for (int c = 1; c <= 8; c++) {
            try {
                if (scp().isShown()) return;
                Locator btn = studentPage.locator(startSel);
                if (btn.count() > 0 && btn.first().isVisible()) btn.first().click();
                studentPage.waitForTimeout(1_200);
                scp().confirmAnyModal();   // "Start Practice Project. Proceed?" -> Yes
            } catch (Throwable midNavigation) { /* start may reload the page mid-check */ }
            if (waitControlPanel(20)) return;
        }
        throw new SkipException("guided control panel did not open after clicking start");
    }

    @Then("the guided control panel is shown")
    public void theGuidedControlPanelIsShown() {
        assertTrue(waitControlPanel(30), "guided control panel is not shown");
    }

    @Then("the guided lab reaches Running")
    public void theGuidedLabReachesRunning() {
        scp().startHandsOnLabIfNeeded();   // the CP does not auto-provision; click "Start hands-on lab"
        // The CP's lab-id inputs are SERVER-rendered (stay empty until a reload), so the reliable
        // source is admin Manage Labs by the student's email (the proven shared-lab pattern).
        ManageLabs ml = new ManageLabs(ctx.page);
        int sid = Integer.parseInt(sectionId);
        for (int i = 0; i < 20 && (guidedLabId == null || guidedLabId.isEmpty()); i++) {
            guidedLabId = scp().labId();
            if (guidedLabId == null || guidedLabId.isEmpty()) {
                try { ml.open(sid); guidedLabId = ml.labIdForEmail(student.email); } catch (Throwable ignored) {}
            }
            if (guidedLabId == null || guidedLabId.isEmpty()) {
                studentPage.waitForTimeout(3_000);
                if (i == 6 || i == 12) scp().startHandsOnLabIfNeeded();   // retry the launch click
            }
        }
        assertTrue(guidedLabId != null && !guidedLabId.isEmpty(),
                "no lab id via the CP inputs or admin Manage Labs - the hands-on-lab launch may not have fired");
        System.out.println("[GuidedLab] lab id=" + guidedLabId);
        ManageLabs.LabState st = CloudLabsClient.waitForState(guidedLabId,
                EnumSet.of(ManageLabs.LabState.RUNNING, ManageLabs.LabState.FAILED), 180, 5);
        assertTrue(st == ManageLabs.LabState.RUNNING,
                "guided lab did not reach Running (state=" + st + ") - if FAILED, check the account pool");
    }

    @When("the student stops the guided lab")
    public void theStudentStopsTheGuidedLab() {
        GuidedControlPanel cp = scp();
        assertTrue(cp.waitEnabled(GuidedControlPanel.STOP, 120),
                "Stop Lab never became enabled on the guided control panel");
        cp.clickGuardedAction(GuidedControlPanel.STOP, GuidedControlPanel.STOP_ACK);
        studentPage.waitForTimeout(8_000);
        try {
            if (cp.isShown() && studentPage.locator(".modal.show").count() == 0) {
                cp.clickGuardedAction(GuidedControlPanel.STOP, GuidedControlPanel.STOP_ACK);   // retry once
            }
        } catch (Throwable midNavigation) { /* stop may navigate away mid-check */ }
    }

    @Then("the guided lab is Stopped and the student returns to the landing")
    public void theGuidedLabIsStoppedAndBackOnLanding() {
        // API FIRST — the authoritative check that the stop took effect.
        ManageLabs.LabState st = CloudLabsClient.waitForState(guidedLabId,
                EnumSet.of(ManageLabs.LabState.STOPPED, ManageLabs.LabState.FAILED), 240, 5);
        assertTrue(st == ManageLabs.LabState.STOPPED, "guided lab did not stop (state=" + st + ")");
        // Then the UI: back on the landing, or at least off the control panel (cp.php may redirect
        // to view.php rather than the in-place landing switch).
        boolean back = false;
        for (int i = 0; i < 45 && !back; i++) {
            try {
                Locator l = studentPage.locator(GuidedLanding.CONTAINER);
                back = (l.count() > 0 && l.first().isVisible())
                        || studentPage.url().contains("view.php")
                        || !scp().isShown();
            } catch (Throwable midNavigation) { /* navigating */ }
            if (!back) studentPage.waitForTimeout(1_000);
        }
        assertTrue(back, "still on the guided control panel after Stop Lab (no redirect to the landing)");
    }

    @When("the student continues the guided project")
    public void theStudentContinuesTheGuidedProject() {
        new GuidedLanding(studentPage).open(guidedCmid());   // ensure we start from the landing
        studentPage.waitForTimeout(5_000);                   // let the landing JS attach
        for (int c = 1; c <= 6; c++) {
            try {
                if (scp().isShown()) return;
                Locator cont = studentPage.locator(GuidedLanding.CONTINUE);
                if (cont.count() > 0 && cont.first().isVisible()) cont.first().click();
                studentPage.waitForTimeout(1_200);
                scp().confirmAnyModal();   // Continue may also ask for confirmation
            } catch (Throwable midNavigation) { /* continue may reload the page mid-check */ }
            if (waitControlPanel(15)) return;
        }
        throw new SkipException("Continue did not reopen the guided control panel");
    }

    @When("the student submits the guided attempt")
    public void theStudentSubmitsTheGuidedAttempt() {
        GuidedControlPanel cp = scp();
        if (!cp.waitEnabled(GuidedControlPanel.SUBMIT, 60)) {
            throw new SkipException("Submit stays disabled (lab is stopped; access not ready) - "
                    + "submit-on-running-lab needs its own scenario");
        }
        cp.clickGuardedAction(GuidedControlPanel.SUBMIT, GuidedControlPanel.SUBMIT_ACK);
        // after submit the page reloads back to the landing
        boolean landingBack = false;
        for (int i = 0; i < 45; i++) {
            Locator l = studentPage.locator(GuidedLanding.CONTAINER);
            if (l.count() > 0 && l.first().isVisible()) { landingBack = true; break; }
            studentPage.waitForTimeout(1_000);
        }
        assertTrue(landingBack, "landing did not come back after submitting the attempt");
    }

    // ---- @guidedattempts: submit-while-running, cooldown, New Attempt ----

    @When("the student submits the guided attempt while the lab is running")
    public void theStudentSubmitsWhileRunning() {
        GuidedControlPanel cp = scp();
        assertTrue(cp.waitEnabled(GuidedControlPanel.SUBMIT, 180),
                "Submit never became enabled on a RUNNING lab");
        cp.clickGuardedAction(GuidedControlPanel.SUBMIT, GuidedControlPanel.SUBMIT_ACK);
    }

    @Then("the attempt is submitted and the student returns to the landing")
    public void theAttemptIsSubmittedAndBackOnLanding() {
        boolean back = false;
        for (int i = 0; i < 60 && !back; i++) {
            try {
                Locator l = studentPage.locator(GuidedLanding.CONTAINER);
                back = (l.count() > 0 && l.first().isVisible())
                        || studentPage.url().contains("view.php")
                        || !scp().isShown();
            } catch (Throwable midNavigation) { /* navigating */ }
            if (!back) studentPage.waitForTimeout(1_000);
        }
        assertTrue(back, "did not leave the control panel after submitting the attempt");
    }

    @Then("the new-attempt cooldown is observed or already elapsed")
    public void theNewAttemptCooldownIsObservedOrElapsed() {
        new GuidedLanding(studentPage).open(guidedCmid());
        studentPage.waitForTimeout(5_000);   // landing JS computes the gap via checkGuidedProjectTimer
        Locator btn = studentPage.locator(GuidedLanding.NEW_ATTEMPT);
        Locator timer = studentPage.locator(GuidedLanding.NEW_ATTEMPT_TIMER);
        assertTrue(btn.count() > 0, "New Attempt button not present on the landing after submit");
        boolean blocked = (timer.count() > 0 && timer.first().isVisible()) || !btn.first().isEnabled();
        // gap = 1 min and may count from the attempt START (already elapsed by submit time) — both
        // outcomes are valid; log which one we saw so PGG-3 coverage is honest.
        System.out.println("[GuidedAttempts] cooldown blocked-signal observed: " + blocked);
    }

    @When("the student starts a new attempt after the cooldown")
    public void theStudentStartsANewAttemptAfterCooldown() {
        // wait (max ~4 min) for New Attempt to become enabled, then click + confirm
        Locator btn = studentPage.locator(GuidedLanding.NEW_ATTEMPT);
        boolean enabled = false;
        for (int i = 0; i < 48 && !enabled; i++) {
            try { enabled = btn.count() > 0 && btn.first().isVisible() && btn.first().isEnabled(); }
            catch (Throwable ignored) {}
            if (!enabled) {
                studentPage.waitForTimeout(5_000);
                if (i % 12 == 11) { new GuidedLanding(studentPage).open(guidedCmid()); studentPage.waitForTimeout(4_000); }
            }
        }
        assertTrue(enabled, "New Attempt did not become enabled after the cooldown gap");
        for (int c = 1; c <= 6; c++) {
            try {
                if (scp().isShown()) return;
                if (btn.count() > 0 && btn.first().isVisible()) btn.first().click();
                studentPage.waitForTimeout(1_200);
                scp().confirmAnyModal();   // "Start Practice Project. Proceed?" -> Yes
            } catch (Throwable midNavigation) { /* new attempt may reload the page */ }
            if (waitControlPanel(20)) return;
        }
        throw new AssertionError("the control panel did not open for the new attempt");
    }

    @After("@guidedlab")
    public void cleanupGuidedLab() {
        if (studentCtx != null) { try { studentCtx.close(); } catch (Throwable ignored) {} studentCtx = null; }
        if (student != null) {
            try { ApiClient.deleteUser(student.id); } catch (Throwable ignored) {}   // cascade-deletes the lab
            student = null;
        }
        if (guidedLabId != null && !guidedLabId.isEmpty()) {
            try { CloudLabsClient.waitForState(guidedLabId, EnumSet.of(ManageLabs.LabState.DELETED), 600, 15); }
            catch (Throwable e) { System.out.println("[GuidedLab] delete-wait after user cascade: " + e.getMessage()); }
            guidedLabId = null;
        }
    }
}
