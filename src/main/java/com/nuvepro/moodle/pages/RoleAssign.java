package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

/**
 * System-level role assignment (/admin/roles/assign.php?contextid=1&roleid=N). Standard Moodle
 * dual-listbox user selector: search -> #addselect (potential) -> Add -> #removeselect (assigned).
 */
public class RoleAssign extends BasePage {
    public RoleAssign(Page page) {
        super(page);
    }

    public void openSystemRoleAssign(int roleId) {
        navigate("/admin/roles/assign.php?contextid=1&roleid=" + roleId);
        page.locator("#addselect").waitFor();
        page.waitForTimeout(800);
    }

    /** Search for a user (by full name) and Add them to this role. */
    public void assignUser(String searchTerm) {
        Locator search = page.locator("#addselect_searchtext");
        if (search.count() > 0) {
            search.click();
            search.type(searchTerm);          // char-by-char -> triggers the AJAX keyup search
            page.waitForTimeout(2_500);
        }
        // #addselect has a disabled optgroup/placeholder header; pick the first ENABLED (real user) option
        Locator options = page.locator("#addselect option:not([disabled])");
        if (options.count() > 0) options.first().click();
        page.locator("input[name='add']").click();
        page.waitForTimeout(1_800);
    }

    /** Whether the user (matched by name/email substring) is in the assigned (#removeselect) list. */
    public boolean userAssigned(String term) {
        return page.locator("#removeselect option")
                .filter(new Locator.FilterOptions().setHasText(term)).count() > 0;
    }

    public void removeUser(String term) {
        Locator opt = page.locator("#removeselect option")
                .filter(new Locator.FilterOptions().setHasText(term));
        if (opt.count() > 0) {
            opt.first().click();
            page.locator("input[name='remove']").click();
            page.waitForTimeout(1_800);
        }
    }
}
