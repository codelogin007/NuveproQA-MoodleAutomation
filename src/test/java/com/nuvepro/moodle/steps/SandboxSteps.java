package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertTrue;

/**
 * Sandbox landing (cloudlabs playground/sandbox, view.php?id=PLAYGROUND_CMID) — batch 1, no
 * provisioning. Verifies the Lab Details block (type/status/created/sandbox id), the duration
 * (alloted/consumed/remaining) and the description + Read More. All presence on the rendered landing.
 */
public class SandboxSteps {
    private final TestContext ctx;

    public SandboxSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    private String body() {
        return ctx.page.locator("#region-main, body").first().innerText();
    }

    @When("admin opens the sandbox activity")
    public void adminOpensTheSandboxActivity() {
        if (Settings.PLAYGROUND_CMID.isEmpty()) throw new SkipException("PLAYGROUND_CMID not set");
        ctx.page.navigate(Settings.BASE_URL + "/mod/cloudlabs/view.php?id=" + Settings.PLAYGROUND_CMID,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        ctx.page.waitForTimeout(4_000);
    }

    @Then("the sandbox landing shows lab type status created date and sandbox id")
    public void theSandboxLandingShowsLabDetails() {
        String b = body();
        assertTrue(b.contains("Lab Type"), "'Lab Type' not shown");
        assertTrue(b.contains("Sandbox"), "lab type is not Sandbox");
        assertTrue(b.contains("Lab Status"), "'Lab Status' not shown");
        assertTrue(b.contains("Lab Created") || b.contains("Created"), "'Lab Created' date not shown");
        assertTrue(b.contains("Sandbox ID"), "'Sandbox ID' not shown");
    }

    @Then("the sandbox landing shows the alloted consumed and remaining duration")
    public void theSandboxLandingShowsDuration() {
        String b = body();
        assertTrue(b.contains("Duration"), "duration section not shown");
        assertTrue(b.contains("Alloted") || b.contains("Allotted"), "alloted duration not shown");
        assertTrue(b.contains("Consumed"), "consumed duration not shown");
        assertTrue(b.contains("Remaining"), "remaining duration not shown");
    }

    @Then("the sandbox landing shows a description section and a Read More control")
    public void theSandboxLandingShowsDescription() {
        assertTrue(body().contains("Description"), "description section not shown on the sandbox landing");
        assertTrue(ctx.page.locator("#read-more-btn, .read-more, a:has-text('Read More'), "
                        + "button:has-text('Read More')").count() > 0,
                "Read More control not present on the sandbox landing");
    }
}
