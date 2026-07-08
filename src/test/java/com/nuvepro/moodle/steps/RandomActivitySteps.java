package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Randomization Activity (mod_randomactivity) — batch 1. Add-form fields + name validation (no save),
 * and one create-and-delete (a real activity is created, then removed via UI in @After because the
 * WS token lacks core_course_delete_modules). Delete: /course/mod.php?sesskey=..&delete=cmid -> Yes.
 */
public class RandomActivitySteps {
    private final TestContext ctx;
    private String createdCmid;
    private String sesskey;

    public RandomActivitySteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private void openAddForm() {
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        ctx.page.navigate(Settings.BASE_URL + "/course/modedit.php?add=randomactivity&type=&course="
                        + Settings.COURSE_ID + "&section=0&return=0&sr=0",
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_000);
        sesskey = (String) ctx.page.evaluate("() => (window.M && M.cfg && M.cfg.sesskey) || ''");
    }

    @When("admin opens a new randomization activity form")
    public void adminOpensANewRandomizationActivityForm() {
        openAddForm();
    }

    @Then("the randomization activity form shows the name and its options")
    public void theFormShowsFields() {
        assertTrue(ctx.page.locator("#id_name").count() > 0, "name field missing");
        assertTrue(ctx.page.locator("[name='dynamicdisplay']").count() > 0, "dynamicdisplay missing");
        assertTrue(ctx.page.locator("[name^='duedate']").count() > 0, "duedate missing");
        assertTrue(ctx.page.locator("[name='completiontrackactivity']").count() > 0, "completiontrack missing");
    }

    @Then("the activity name field accepts alphanumeric and special characters")
    public void theNameFieldAcceptsSpecialChars() {
        String val = "AutoRand 42 A1!@#-_";
        Locator name = ctx.page.locator("#id_name");
        name.fill(val);                          // client-side only; no save
        assertEquals(name.inputValue(), val, "name field did not accept the input");
    }

    @Then("saving without a name is blocked by the required validation")
    public void savingWithoutNameIsBlocked() {
        ctx.page.locator("#id_name").fill("");
        ctx.page.locator("#id_submitbutton2, #id_submitbutton").first().click();
        ctx.page.waitForTimeout(1_500);
        // required rule (client) blocks submission -> still on the edit form + an error shown
        assertTrue(ctx.page.url().contains("modedit.php"),
                "form was submitted despite the empty required name");
        String body = ctx.page.locator("body").innerText().toLowerCase();
        assertTrue(body.contains("supply a value") || body.contains("required")
                        || ctx.page.locator("[id^='id_error_name'], .error").count() > 0,
                "no required-field validation shown for the empty name");
    }

    @Then("admin can create a randomization activity and it appears in the course")
    public void adminCanCreateAndItAppears() {
        long s = System.currentTimeMillis();
        String name = "AutoRand" + s;
        ctx.page.locator("#id_name").fill(name + " A1!@#");
        ctx.page.locator("#id_submitbutton2, #id_submitbutton").first().click();
        ctx.page.waitForTimeout(3_000);
        ctx.page.navigate(Settings.BASE_URL + "/course/view.php?id=" + Settings.COURSE_ID,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(2_500);
        Locator link = ctx.page.locator("a:has-text('" + name + "')");
        assertTrue(link.count() > 0, "created randomization activity not found in the course");
        Matcher m = Pattern.compile("id=(\\d+)").matcher(link.first().getAttribute("href"));
        if (m.find()) createdCmid = m.group(1);
        if (sesskey == null || sesskey.isEmpty())
            sesskey = (String) ctx.page.evaluate("() => (window.M && M.cfg && M.cfg.sesskey) || ''");
    }

    @After("@randomactivity")
    public void cleanup() {
        if (createdCmid != null && sesskey != null && !sesskey.isEmpty()) {
            try {
                ctx.page.navigate(Settings.BASE_URL + "/course/mod.php?sesskey=" + sesskey
                                + "&sr=0&delete=" + createdCmid,
                        new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                                .setTimeout(45_000));
                ctx.page.waitForTimeout(1_500);
                Locator yes = ctx.page.locator("button:has-text('Yes'), input[value='Yes'], "
                        + "button:has-text('Delete'), #region-main .btn-primary");
                if (yes.count() > 0) { yes.first().click(); ctx.page.waitForTimeout(1_800); }
            } catch (Throwable ignored) {}
            createdCmid = null;
        }
    }
}
