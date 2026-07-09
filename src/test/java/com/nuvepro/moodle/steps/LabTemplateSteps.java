package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertTrue;

/**
 * Lab Templates (cloudlabs) — batch 1, no persistence. List page (#np-ap-lt-labtable), add-form
 * config fields, and the site-admin-only gate (labtemplates.php: require_login + is_siteadmin).
 */
public class LabTemplateSteps {
    private static final String LIST = "/mod/cloudlabs/labtemplates.php";
    private static final String ADD = "/mod/cloudlabs/addlabtemplate.php";
    private static final String TABLE = "#np-ap-lt-labtable";

    private final TestContext ctx;
    private ApiClient.SeededUser student;
    private BrowserContext studentCtx;

    public LabTemplateSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private void open(Page p, String path) {
        p.navigate(Settings.BASE_URL + path,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        p.waitForTimeout(3_000);
    }

    @When("admin opens the lab templates list")
    public void adminOpensTheLabTemplatesList() {
        open(ctx.page, LIST);
    }

    @Then("the lab templates table lists templates with plan and team columns")
    public void theLabTemplatesTableListsTemplates() {
        assertTrue(ctx.page.locator(TABLE).count() > 0, "lab templates table not present");
        String headers = ctx.page.locator(TABLE + " thead").count() > 0
                ? ctx.page.locator(TABLE + " thead").first().innerText().toLowerCase()
                : ctx.page.locator(TABLE).first().innerText().toLowerCase();
        assertTrue(headers.contains("plan") && headers.contains("team"),
                "lab templates table missing Plan/Team columns (headers=" + headers.replace("\n", " ") + ")");
        assertTrue(ctx.page.locator(TABLE + " tbody tr").count() > 0, "no lab templates listed");
    }

    @When("admin opens the add lab template form")
    public void adminOpensTheAddLabTemplateForm() {
        open(ctx.page, ADD);
    }

    @Then("the add lab template form shows the configuration fields")
    public void theAddFormShowsFields() {
        assertTrue(ctx.page.locator("[name='resource_type']").count() > 0, "resource_type field missing");
        assertTrue(ctx.page.locator("[name='createIntervalDuration'], [name^='createInterval']").count() > 0
                        || ctx.page.locator("[name^='startInterval']").count() > 0,
                "duration/interval fields missing on the add lab template form");
        assertTrue(ctx.page.locator("#id_submitbutton, input[type=submit], button[type=submit]").count() > 0,
                "save button missing on the add lab template form");
    }

    @Then("a student cannot open the lab templates list")
    public void aStudentCannotOpenTheLabTemplatesList() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        student = ApiClient.createUser(System.currentTimeMillis());
        if (!Settings.COURSE_ID.isEmpty())
            ApiClient.enrolUser(student.id, Long.parseLong(Settings.COURSE_ID), 5);
        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page sp = studentCtx.newPage();
        sp.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(sp, student.username, student.password);
        open(sp, LIST);
        assertTrue(sp.locator(TABLE).count() == 0,
                "DEFECT: a non-site-admin can see the lab templates table");
        // labtemplates.php just `die`s for a non-site-admin -> a BLANK page (no message). Absence of
        // the table + an empty/short body both confirm the block.
        String body = sp.locator("body").innerText().trim().toLowerCase();
        assertTrue(body.length() < 60 || body.contains("permission") || body.contains("not allowed")
                        || body.contains("cannot") || body.contains("denied") || body.contains("error")
                        || sp.url().contains("login"),
                "student was not blocked from the lab templates list (body=" + body.substring(0, Math.min(60, body.length())) + ")");
    }

    @After("@labtemplates")
    public void cleanup() {
        if (studentCtx != null) { try { studentCtx.close(); } catch (Throwable ignored) {} studentCtx = null; }
        if (student != null) { try { ApiClient.deleteUser(student.id); } catch (Throwable ignored) {} student = null; }
    }
}
