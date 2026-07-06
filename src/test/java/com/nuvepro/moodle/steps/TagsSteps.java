package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.ActivityTags;
import com.nuvepro.moodle.pages.TagManage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Moodle tag steps — first batch: tag creation via the activity edit form (no Orgo).
 * Tag management (edit/delete/standard/combine) lives on the report-builder /tag/manage.php page
 * and is a later batch.
 */
public class TagsSteps {
    private final TestContext ctx;
    private final ActivityTags tags;
    private final TagManage manage;
    private String lastTag;
    private int chipsBefore;
    private String tagA;
    private String tagB;
    private String tagC;
    private String tagRenamed;

    public TagsSteps(TestContext ctx) {
        this.ctx = ctx;
        this.tags = new ActivityTags(ctx.page);
        this.manage = new TagManage(ctx.page);
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

    // ---- tag management (/tag/manage.php) : T16 rename, T18 delete, T28 multi-delete ----

    @Given("a new standard tag")
    public void aNewStandardTag() {
        manage.open();
        tagA = "aatst" + System.currentTimeMillis();
        manage.addStandardTag(tagA);
        manage.open();
        assertTrue(manage.tagExists(tagA), "standard tag was not created: " + tagA);
    }

    @Given("two new standard tags")
    public void twoNewStandardTags() {
        manage.open();
        long s = System.currentTimeMillis();
        tagA = "aatst" + s + "a";
        tagB = "aatst" + s + "b";
        manage.addStandardTag(tagA);
        manage.addStandardTag(tagB);
        manage.open();
        assertTrue(manage.tagExists(tagA) && manage.tagExists(tagB), "two standard tags were not created");
    }

    @When("admin renames the tag")
    public void adminRenamesTheTag() {
        tagRenamed = tagA + "r";
        manage.renameTag(tagA, tagRenamed);
        manage.open();
    }

    @Then("the renamed tag exists and the old name does not")
    public void theRenamedTagExistsAndTheOldNameDoesNot() {
        assertTrue(manage.tagExists(tagRenamed), "renamed tag not found: " + tagRenamed);
        assertFalse(manage.tagExists(tagA), "old tag name still present: " + tagA);
        manage.selectTag(tagRenamed);           // cleanup
        manage.deleteSelected();
    }

    @When("admin deletes the tag")
    public void adminDeletesTheTag() {
        manage.selectTag(tagA);
        manage.deleteSelected();
        manage.open();
    }

    @Then("the tag no longer exists")
    public void theTagNoLongerExists() {
        assertFalse(manage.tagExists(tagA), "tag still exists after delete: " + tagA);
    }

    @When("admin deletes both tags")
    public void adminDeletesBothTags() {
        manage.selectTag(tagA);
        manage.selectTag(tagB);
        manage.deleteSelected();
        manage.open();
    }

    @Then("neither tag exists")
    public void neitherTagExists() {
        assertFalse(manage.tagExists(tagA) || manage.tagExists(tagB), "a tag still exists after multi-delete");
    }

    // ---- combine (T24 two, T25 three, T29 single) ----

    @Given("three new standard tags")
    public void threeNewStandardTags() {
        manage.open();
        long s = System.currentTimeMillis();
        tagA = "aatst" + s + "a";
        tagB = "aatst" + s + "b";
        tagC = "aatst" + s + "c";
        manage.addStandardTag(tagA);
        manage.addStandardTag(tagB);
        manage.addStandardTag(tagC);
        manage.open();
        assertTrue(manage.tagExists(tagA) && manage.tagExists(tagB) && manage.tagExists(tagC),
                "three standard tags were not created");
    }

    @When("admin combines the two tags keeping the first")
    public void adminCombinesTheTwoTagsKeepingTheFirst() {
        manage.selectTag(tagA);
        manage.selectTag(tagB);
        manage.combineSelected(tagA);
        manage.open();
    }

    @Then("only the surviving tag remains")
    public void onlyTheSurvivingTagRemains() {
        assertTrue(manage.tagExists(tagA), "surviving tag missing after combine: " + tagA);
        assertFalse(manage.tagExists(tagB), "merged tag still present after combine: " + tagB);
        manage.selectTag(tagA);           // cleanup
        manage.deleteSelected();
    }

    @When("admin combines the three tags keeping the first")
    public void adminCombinesTheThreeTagsKeepingTheFirst() {
        manage.selectTag(tagA);
        manage.selectTag(tagB);
        manage.selectTag(tagC);
        manage.combineSelected(tagA);
        manage.open();
    }

    @Then("only the first of the three tags remains")
    public void onlyTheFirstOfTheThreeTagsRemains() {
        assertTrue(manage.tagExists(tagA), "surviving tag missing: " + tagA);
        assertFalse(manage.tagExists(tagB) || manage.tagExists(tagC), "a merged tag still present");
        manage.selectTag(tagA);           // cleanup
        manage.deleteSelected();
    }

    @When("admin tries to combine a single selected tag")
    public void adminTriesToCombineASingleSelectedTag() {
        manage.selectTag(tagA);
        try { manage.combineSelected(tagA); } catch (Throwable ignored) {}
        manage.open();
    }

    @Then("the single tag is unchanged")
    public void theSingleTagIsUnchanged() {
        assertTrue(manage.tagExists(tagA), "single-tag combine lost the tag: " + tagA);
        manage.selectTag(tagA);           // cleanup
        manage.deleteSelected();
    }
}
