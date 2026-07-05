package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.GuidedLanding;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.regex.Pattern;

import static org.testng.Assert.assertTrue;

/**
 * Guided (practice project) lab steps — first batch: landing + admin + config presence checks
 * on an existing guided activity (GUIDED_CMID). No lab provisioning here.
 */
public class GuidedSteps {
    private final TestContext ctx;
    private GuidedLanding landing;
    private String sectionId;

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
}
