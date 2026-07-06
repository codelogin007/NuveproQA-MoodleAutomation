package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * Moodle course groups (/group/index.php, /group/group.php) + the cloudlabs groups filter dropdown
 * (#course_groups, a select2) on the Manage User Labs page (managelabs.php).
 */
public class Groups extends BasePage {
    public Groups(Page page) {
        super(page);
    }

    /** Submit the create-group form; returns true if it succeeded (left the form to the group index). */
    public boolean createGroupSucceeded(int courseId, String name) {
        navigate("/group/group.php?courseid=" + courseId + "&id=0");
        page.locator("#id_name").waitFor();
        page.locator("#id_name").fill(name);
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1_500);
        return !page.url().contains("group.php");   // still on group.php == validation error (e.g. duplicate name)
    }

    public void createGroup(int courseId, String name) {
        createGroupSucceeded(courseId, name);
    }

    /** How many groups whose name matches {@code name} exist in the course groups list. */
    public int groupCount(int courseId, String name) {
        navigate("/group/index.php?id=" + courseId);
        page.waitForTimeout(1_200);
        return page.locator("#groups option").filter(new Locator.FilterOptions().setHasText(name)).count();
    }

    /** Delete every group matching {@code name} (one per pass — deletion re-renders the list). */
    public void deleteGroups(int courseId, String name) {
        for (int i = 0; i < 6; i++) {
            navigate("/group/index.php?id=" + courseId);
            page.waitForTimeout(1_000);
            Locator opt = page.locator("#groups option").filter(new Locator.FilterOptions().setHasText(name));
            if (opt.count() == 0) return;
            opt.first().click();
            Locator del = page.locator("input[value='Delete selected group'], button:has-text('Delete selected group')");
            if (del.count() == 0) return;
            del.first().click();
            page.waitForTimeout(1_000);
            Locator yes = page.locator("input[value='Yes'], button:has-text('Yes'), "
                    + "button:has-text('Delete'), #region-main .btn-primary");
            if (yes.count() > 0 && yes.first().isVisible()) yes.first().click();
            page.waitForTimeout(1_500);
        }
    }

    public void openManageLabs(int sectionId) {
        navigate("/mod/cloudlabs/managelabs.php?sectionid=" + sectionId);
        page.waitForTimeout(2_500);
    }

    public boolean groupsDropdownPresent() {
        return page.locator("#course_groups").count() > 0;
    }

    public boolean groupsDropdownHasGroup(String name) {
        return page.locator("#course_groups option").filter(new Locator.FilterOptions().setHasText(name)).count() > 0;
    }
}
