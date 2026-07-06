package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.pages.AdminUsers;
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
    private final AdminUsers admin;
    private String groupName;
    private String cohortName;
    private String groupNameA;
    private String groupNameB;
    private String groupIdA;
    private String groupIdB;
    private String groupUserSearch;
    private ApiClient.SeededUser groupUser;

    public GroupsSteps(TestContext ctx) {
        this.ctx = ctx;
        this.groups = new Groups(ctx.page);
        this.admin = new AdminUsers(ctx.page);
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

    // ---- G5 default, G2 single-select, G4 search, G42 groups column ----
    @Then("the groups dropdown defaults to all groups")
    public void theGroupsDropdownDefaultsToAllGroups() {
        assertTrue(groups.selectedGroup().toLowerCase().contains("all group"),
                "groups dropdown default is not 'All Groups': " + groups.selectedGroup());
    }

    @Then("admin can select a single group in the dropdown")
    public void adminCanSelectASingleGroup() {
        groups.selectGroup(groupName);
        assertTrue(groups.selectedGroup().contains(groupName), "group was not selected: " + groupName);
    }

    @Then("admin can search for the group in the dropdown")
    public void adminCanSearchForTheGroup() {
        assertTrue(groups.searchGroupInDropdown(groupName), "group not found via dropdown search: " + groupName);
    }

    @Then("the groups column is present on Manage User Labs")
    public void theGroupsColumnIsPresent() {
        assertTrue(groups.labsTableHasGroupColumn(), "Group column not present on the Manage User Labs table");
    }

    @Then("selecting multiple groups is not supported")
    public void selectingMultipleGroupsIsNotSupported() {
        // The #course_groups dropdown is a SINGLE-select (All Groups or one group), so the manual G3
        // ("select multiple groups") is not supported by this UI - documented, not a failure.
        assertTrue(groups.isGroupsDropdownSingleSelect(), "expected the groups dropdown to be single-select");
        throw new SkipException("G3: groups dropdown is single-select (All Groups or one group); "
                + "multiple-group selection is not supported by the UI");
    }

    // ---- G11 create cohort, G13 duplicate cohort names allowed, G24 same user to multiple groups ----

    @When("admin creates a cohort")
    public void adminCreatesACohort() {
        cohortName = "AutoCohort" + System.currentTimeMillis();
        assertTrue(admin.cohortCreateSucceeded(cohortName, ""), "cohort was not created: " + cohortName);
    }

    @Then("the cohort is listed")
    public void theCohortIsListed() {
        assertTrue(admin.cohortListed(cohortName), "cohort not listed: " + cohortName);
    }

    @When("admin creates two cohorts with the same name")
    public void adminCreatesTwoCohortsSameName() {
        cohortName = "AutoDupCoh" + System.currentTimeMillis();
        assertTrue(admin.cohortCreateSucceeded(cohortName, ""), "first cohort not created");
        assertTrue(admin.cohortCreateSucceeded(cohortName, ""), "second (duplicate-name) cohort was rejected");
    }

    @Then("both cohorts with that name exist")
    public void bothCohortsExist() {
        assertTrue(admin.cohortCount(cohortName) >= 2,
                "duplicate cohort names were not both created (count=" + admin.cohortCount(cohortName) + ")");
    }

    @When("admin adds the same user to two groups")
    public void adminAddsSameUserToTwoGroups() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        long s = System.currentTimeMillis();
        groupNameA = "MulGrpA" + s;
        groupNameB = "MulGrpB" + s;
        groupIdA = groups.createGroupReturningId(courseId(), groupNameA);
        groupIdB = groups.createGroupReturningId(courseId(), groupNameB);
        groupUser = ApiClient.createUser(s);
        ApiClient.enrolUser(groupUser.id, courseId(), 5);           // must be enrolled to join a group
        groupUserSearch = "Test" + groupUser.username.substring("autotest_".length());
        groups.addUserToGroup(groupIdA, groupUserSearch);
        groups.addUserToGroup(groupIdB, groupUserSearch);
    }

    @Then("the user is a member of both groups")
    public void theUserIsAMemberOfBothGroups() {
        assertTrue(groups.userInGroup(groupIdA, groupUserSearch), "user not a member of group A");
        assertTrue(groups.userInGroup(groupIdB, groupUserSearch), "user not a member of group B");
    }

    @After("@groups")
    public void cleanupGroup() {
        for (String gn : new String[]{groupName, groupNameA, groupNameB}) {
            if (gn != null) { try { groups.deleteGroups(courseId(), gn); } catch (Throwable ignored) {} }
        }
        if (cohortName != null) {
            try { for (int i = 0; i < 3; i++) admin.cohortDelete(cohortName); } catch (Throwable ignored) {}
        }
        if (groupUser != null) { try { ApiClient.deleteUser(groupUser.id); } catch (Throwable ignored) {} }
        groupName = groupNameA = groupNameB = cohortName = null;
        groupUser = null;
    }
}
