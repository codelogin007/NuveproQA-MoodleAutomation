package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.ActivityTags;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertEquals;
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
    private int chipsBefore;

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

    // ---- T2-T5: length boundaries ----
    @When("I add a tag of {int} characters")
    public void iAddATagOfNCharacters(int n) {
        lastTag = genLenTag(n);
        tags.addTag(lastTag);
        assertTrue(tags.chipPresent(lastTag), "tag chip not shown for a " + n + "-character tag");
    }

    /** Distinct, exact-length tag: short -> "z*"; else "t<n>" padded with 'y' to length n. */
    private String genLenTag(int n) {
        if (n <= 3) return "z".repeat(Math.max(1, n));
        String p = "t" + n;
        return p.length() >= n ? p.substring(0, n) : p + "y".repeat(n - p.length());
    }

    // ---- T8-T12 use the existing "I add the tag" step ----

    // ---- T13: blank tag ----
    @When("I add a blank tag")
    public void iAddABlankTag() {
        tags.expandTags();
        chipsBefore = tags.chipCount();
        tags.typeAndEnter("");
    }

    @Then("no tag is added")
    public void noTagIsAdded() {
        assertEquals(tags.chipCount(), chipsBefore, "a blank tag was unexpectedly added");
    }

    // ---- T14/T15: enter without chip assertion (trimmed / comma-split) ----
    @When("I enter the tag {string}")
    public void iEnterTheTag(String tag) {
        lastTag = tag.trim();
        tags.typeAndEnter(tag);
    }

    @Then("the trimmed tag {string} is present on the activity")
    public void theTrimmedTagIsPresent(String tag) {
        assertTrue(tags.tagPersisted(cmid(), tag), "trimmed tag not persisted: " + tag);
    }

    @Then("both tags {string} and {string} are present on the activity")
    public void bothTagsArePresent(String a, String b) {
        tags.openEdit(cmid());
        tags.expandTags();
        assertTrue(tags.chipPresent(a), "first comma-separated tag not present: " + a);
        assertTrue(tags.chipPresent(b), "second comma-separated tag not present: " + b);
    }
}
