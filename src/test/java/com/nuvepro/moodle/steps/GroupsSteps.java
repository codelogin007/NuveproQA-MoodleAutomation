package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.pages.Groups;
import io.cucumber.java.After;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Groups module — course groups create/list/delete (G12/G14) and the cloudlabs groups filter
 * dropdown on Manage User Labs (G1). Groups are created with unique names and deleted at teardown.
 */
public class GroupsSteps {
    private final TestContext ctx;
    private final Groups groups;
    private String groupName;

    public GroupsSteps(TestContext ctx) {
        this.ctx = ctx;
        this.groups = new Groups(ctx.page);
    }

    private int courseId() {
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        return Integer.parseInt(Settings.COURSE_ID);
    }

    private int playgroundSectionId() {
        if (Settings.PLAYGROUND_CMID.isEmpty()) throw new SkipException("PLAYGROUND_CMID not set");
        ctx.page.navigate(Settings.BASE_URL + "/mod/cloudlabs/view.php?id=" + Settings.PLAYGROUND_CMID,
                new com.microsoft.playwright.Page.NavigateOptions()
                        .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED).setTimeout(45_000));
        ctx.page.waitForTimeout(3_000);
        var sec = ctx.page.locator("#np-ap-pl-sectionId, input[id*='sectionId']");
        return Integer.parseInt(sec.first().inputValue());
    }

    // ---- G12: create a group ----
    @When("admin creates a group")
    public void adminCreatesAGroup() {
        groupName = "AutoGroup" + System.currentTimeMillis();
        groups.createGroup(courseId(), groupName);
    }

    @Then("the group is listed in the course")
    public void theGroupIsListed() {
        assertTrue(groups.groupCount(courseId(), groupName) >= 1, "group not listed: " + groupName);
    }

    // ---- G14: duplicate group names — Moodle enforces UNIQUE group names per course, so the second
    // create is rejected. (The manual expects duplicates to be allowed; standard Moodle rejects them.)
    private boolean duplicateSucceeded;

    @When("admin creates a group then attempts a duplicate name")
    public void adminAttemptsDuplicateGroupName() {
        groupName = "AutoDup" + System.currentTimeMillis();
        assertTrue(groups.createGroupSucceeded(courseId(), groupName), "first group was not created: " + groupName);
        duplicateSucceeded = groups.createGroupSucceeded(courseId(), groupName);
    }

    @Then("the duplicate group name is rejected")
    public void theDuplicateGroupNameIsRejected() {
        assertFalse(duplicateSucceeded, "duplicate group name was accepted (Moodle should reject same-name groups in a course)");
        assertEquals(groups.groupCount(courseId(), groupName), 1, "expected exactly one group with that name");
    }

    // ---- G1: groups dropdown on Manage User Labs ----
    @When("admin creates a group and opens Manage User Labs")
    public void createGroupAndOpenManageLabs() {
        groupName = "AutoDD" + System.currentTimeMillis();
        groups.createGroup(courseId(), groupName);
        groups.openManageLabs(playgroundSectionId());
    }

    @Then("the groups dropdown is present and lists the group")
    public void theGroupsDropdownIsPresent() {
        assertTrue(groups.groupsDropdownPresent(), "#course_groups dropdown not present on Manage User Labs");
        assertTrue(groups.groupsDropdownHasGroup(groupName), "created group not offered in the groups dropdown: " + groupName);
    }

    @After("@groups")
    public void cleanupGroup() {
        if (groupName != null) {
            try { groups.deleteGroups(courseId(), groupName); } catch (Throwable ignored) {}
            groupName = null;
        }
    }
}
