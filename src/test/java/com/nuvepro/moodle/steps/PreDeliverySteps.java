package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Pre-Delivery Lab Readiness Checklist (lab_readiness.php?id=cmid). Batch 1 = no provisioning:
 * role gate (mod/cloudlabs:viewlabreadiness), progress-bar mechanics (#labProgressBar -> 100%
 * bg-success), env-type field toggle, mandatory support-engineer validation (alert -> save blocked).
 * NONE of these perform a successful save, so the shared activity is never mutated. Selectors from
 * lab_readiness.mustache / amd/src/lab_readiness.js.
 */
public class PreDeliverySteps {
    private static final String FORM = "#labReadinessForm";
    private static final String PROGRESS = "#labProgressBar";
    private static final String ITEM = ".checklist-item";

    private final TestContext ctx;
    private ApiClient.SeededUser student;
    private BrowserContext studentCtx;

    public PreDeliverySteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private int cmid() {
        if (Settings.ASSESSMENT_CMID.isEmpty()) throw new SkipException("ASSESSMENT_CMID not set");
        return Integer.parseInt(Settings.ASSESSMENT_CMID);
    }

    private void openReadiness(Page p) {
        p.navigate(Settings.BASE_URL + "/mod/cloudlabs/lab_readiness.php?id=" + cmid(),
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        p.waitForTimeout(2_500);
    }

    @When("admin opens the lab readiness page")
    public void adminOpensTheLabReadinessPage() {
        openReadiness(ctx.page);
    }

    @Then("the lab readiness checklist form is shown")
    public void theLabReadinessFormIsShown() {
        assertTrue(ctx.page.locator(FORM).count() > 0, "lab readiness form not shown for admin");
        assertTrue(ctx.page.locator(ITEM).count() > 0, "no checklist items rendered");
        assertTrue(ctx.page.locator(PROGRESS).count() > 0, "progress bar not present");
    }

    @Then("a student cannot open the lab readiness page")
    public void aStudentCannotOpenTheLabReadinessPage() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        // enrol into the activity's OWN course (from the course-<id> body class on the admin page)
        String bodyClass = ctx.page.locator("body").getAttribute("class");
        Matcher m = Pattern.compile("course-(\\d+)").matcher(bodyClass == null ? "" : bodyClass);
        if (!m.find()) throw new SkipException("could not determine the activity's course id");
        student = ApiClient.createUser(System.currentTimeMillis());
        ApiClient.enrolUser(student.id, Long.parseLong(m.group(1)), 5);

        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page sp = studentCtx.newPage();
        sp.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(sp, student.username, student.password);
        openReadiness(sp);
        assertTrue(sp.locator(FORM).count() == 0,
                "DEFECT: a student can open the Lab Readiness checklist (form rendered)");
        String body = sp.locator("body").innerText().toLowerCase();
        assertTrue(body.contains("permission") || body.contains("not allowed") || body.contains("cannot"),
                "student was not shown a permission error on the lab readiness page");
    }

    @Then("checking all visible checklist items sets the progress bar to 100 percent and green")
    public void checkingAllItemsDrivesProgressTo100Green() {
        // pure client-side: tick every VISIBLE checkbox + fire change; DO NOT save (no persistence)
        ctx.page.evaluate("() => { document.querySelectorAll('.checklist-item').forEach(c => {"
                + " if (c.offsetParent && !c.checked) { c.checked = true;"
                + " c.dispatchEvent(new Event('change', {bubbles:true})); } }); }");
        ctx.page.waitForTimeout(1_000);
        Locator bar = ctx.page.locator(PROGRESS);
        String txt = bar.first().innerText().trim();
        String cls = bar.first().getAttribute("class");
        assertTrue(txt.contains("100"), "progress bar did not reach 100% (was '" + txt + "')");
        assertTrue(cls != null && cls.contains("bg-success"),
                "progress bar not green (bg-success) at 100% (class=" + cls + ")");
    }

    @Then("switching the environment type toggles the VM and Account fields")
    public void switchingEnvTypeTogglesFields() {
        Locator vm = ctx.page.locator(".vm-field");
        Locator account = ctx.page.locator(".account-field");
        if (ctx.page.locator("input[name='envtype'][value='account']").count() == 0)
            throw new SkipException("envtype radios not present");
        ctx.page.locator("input[name='envtype'][value='account']").first()
                .check(new Locator.CheckOptions().setForce(true));
        ctx.page.waitForTimeout(700);
        assertTrue(account.first().isVisible(), "account fields not shown for envtype=account");
        assertFalse(vm.first().isVisible(), "vm fields still shown for envtype=account");
        ctx.page.locator("input[name='envtype'][value='vm']").first()
                .check(new Locator.CheckOptions().setForce(true));
        ctx.page.waitForTimeout(700);
        assertTrue(vm.first().isVisible(), "vm fields not shown for envtype=vm");
        assertFalse(account.first().isVisible(), "account fields still shown for envtype=vm");
    }

    @Then("saving with an empty support engineer name is blocked by an alert")
    public void savingWithEmptyNameIsBlocked() {
        // support name empty -> the save handler alert()s and returns BEFORE form.submit() (no persist).
        final String[] alertMsg = {null};
        Consumer<Dialog> handler = d -> { alertMsg[0] = d.message(); d.accept(); };
        ctx.page.onDialog(handler);
        try {
            Locator name = ctx.page.locator("input[name='supportname'], #supportName");
            if (name.count() > 0) name.first().fill("");
            String urlBefore = ctx.page.url();
            ctx.page.locator("#saveChecklistBtn").click();
            ctx.page.waitForTimeout(1_500);
            assertTrue(alertMsg[0] != null && alertMsg[0].toLowerCase().contains("support engineer"),
                    "expected the 'Support Engineer Name' validation alert (got: " + alertMsg[0] + ")");
            assertTrue(ctx.page.url().contains("lab_readiness.php") && ctx.page.locator(FORM).count() > 0,
                    "form appears to have submitted despite the empty-name validation");
        } finally {
            ctx.page.offDialog(handler);
        }
    }

    @After("@predelivery")
    public void cleanup() {
        if (studentCtx != null) { try { studentCtx.close(); } catch (Throwable ignored) {} studentCtx = null; }
        if (student != null) { try { ApiClient.deleteUser(student.id); } catch (Throwable ignored) {} student = null; }
    }
}
