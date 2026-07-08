package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Cloudlabs config sheets: Content_Integration_Git_URL (descriptionsource manual/git ->
 * introeditor vs descriptiongiturl) and Difficulty_Level_Configuration (activity difficulty_level
 * select + the admin "Difficulty Level Settings" on Cloud Server Settings). No provisioning, NO save.
 */
public class CloudLabsConfigSteps {
    private final TestContext ctx;

    public CloudLabsConfigSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private void openAddForm() {
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?add=cloudlabs&type=&course="
                        + Settings.COURSE_ID + "&section=0&return=0&sr=0",
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        Locator ex = ctx.page.locator("a.collapseexpand, .collapsible-actions a");
        if (ex.count() > 0) { try { ex.first().click(); ctx.page.waitForTimeout(600); } catch (Throwable ignored) {} }
    }

    private void pickRadio(String name, String value) {
        ctx.page.locator("input[type=radio][name='" + name + "'][value='" + value + "']").first()
                .check(new Locator.CheckOptions().setForce(true));
        ctx.page.waitForTimeout(700);
    }

    private boolean visible(String selector) {
        Locator l = ctx.page.locator(selector);
        return l.count() > 0 && l.first().isVisible();
    }

    @When("I open a new cloudlabs activity form")
    public void iOpenANewCloudlabsActivityForm() {
        openAddForm();
    }

    @Then("the description source toggles the git URL field")
    public void theDescriptionSourceTogglesGitUrl() {
        if (ctx.page.locator("input[type=radio][name='descriptionsource']").count() == 0)
            throw new SkipException("descriptionsource not present on this form");
        pickRadio("descriptionsource", "git");
        assertTrue(visible("[name='descriptiongiturl']"), "git URL field not shown for descriptionsource=git");
        pickRadio("descriptionsource", "manual");
        assertFalse(visible("[name='descriptiongiturl']"), "git URL field still shown for descriptionsource=manual");
    }

    @When("I open a new challenge activity form for config")
    public void iOpenANewChallengeActivityForm() {
        openAddForm();
        Locator ch = ctx.page.locator("input[name='labtype'][value='challenge']");
        if (ch.count() > 0 && !ch.first().isChecked()) {
            ch.first().check(new Locator.CheckOptions().setForce(true));
            ctx.page.waitForTimeout(1_200);
        }
    }

    @Then("the difficulty level select offers the configured levels and defaults to Beginner")
    public void theDifficultyLevelSelectOffersLevels() {
        Locator dl = ctx.page.locator("select[name='difficulty_level']");
        assertTrue(dl.count() > 0 && dl.first().isVisible(), "difficulty_level select not shown for a challenge");
        int opts = ctx.page.locator("select[name='difficulty_level'] option").count();
        assertTrue(opts >= 2, "difficulty_level has too few options (" + opts + ")");
        String all = dl.first().innerText();
        assertTrue(all.contains("Beginner"), "difficulty levels do not include Beginner (got: " + all.replace("\n", " ") + ")");
        // default is Beginner (mod_form setDefault)
        String selected = (String) dl.first().evaluate("e => e.options[e.selectedIndex] ? e.options[e.selectedIndex].text.trim() : ''");
        assertEquals(selected, "Beginner", "difficulty level did not default to Beginner");
    }

    @When("admin opens the cloud server settings")
    public void adminOpensTheCloudServerSettings() {
        ctx.page.navigate(Settings.BASE_URL + "/admin/settings.php?section=modcloudlabsgeneralsettings",
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_500);
    }

    @Then("the difficulty level settings section is present")
    public void theDifficultyLevelSettingsSectionIsPresent() {
        assertTrue(ctx.page.locator("[id='id_s_cloudlabs_difficulty_levels'], [name='s_cloudlabs_difficulty_levels']").count() > 0,
                "difficulty_levels admin field not present on Cloud Server Settings");
        assertTrue(ctx.page.locator("body").innerText().contains("Difficulty Level Settings"),
                "'Difficulty Level Settings' heading not present");
    }
}
