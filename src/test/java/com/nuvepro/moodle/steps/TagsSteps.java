package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.ActivityTags;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertTrue;

/**
 * Moodle tag steps — first batch: tag creation via the activity edit form (no Orgo).
 * Tag management (edit/delete/standard/combine) lives on the report-builder /tag/manage.php page
 * and is a later batch.
 */
public class TagsSteps {
    private final TestContext ctx;
    private final ActivityTags tags;
    private String lastTag;

    public TagsSteps(TestContext ctx) {
        this.ctx = ctx;
        this.tags = new ActivityTags(ctx.page);
    }

    private int cmid() {
        if (Settings.PLAYGROUND_CMID.isEmpty()) throw new SkipException("PLAYGROUND_CMID not set");
        return Integer.parseInt(Settings.PLAYGROUND_CMID);
    }

    @When("I open the activity edit form")
    public void iOpenTheActivityEditForm() {
        tags.openEdit(cmid());
    }

    @Then("the Tags field is available")
    public void theTagsFieldIsAvailable() {
        assertTrue(tags.tagsFieldPresent(), "Tags field not present on the activity form");
    }

    @When("I save the activity without changing tags")
    public void iSaveTheActivityWithoutChangingTags() {
        tags.save();
    }

    @Then("the activity is saved")
    public void theActivityIsSaved() {
        assertTrue(tags.savedOk(), "activity did not save (url=" + ctx.page.url() + ")");
    }

    @When("I add the tag {string}")
    public void iAddTheTag(String tag) {
        lastTag = tag;
        tags.addTag(tag);
        assertTrue(tags.chipPresent(tag), "tag chip did not appear after adding: " + tag);
    }

    @When("I save the activity")
    public void iSaveTheActivity() {
        tags.save();
    }

    @Then("the tag is present on the activity")
    public void theTagIsPresentOnTheActivity() {
        assertTrue(tags.tagPersisted(cmid(), lastTag), "tag not persisted on the activity: " + lastTag);
    }
}
