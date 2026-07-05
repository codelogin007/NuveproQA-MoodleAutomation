package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

/**
 * Standard Moodle user administration: Add user form (/user/editadvanced.php),
 * user list (/admin/user.php), edit/suspend/delete. No cloudlabs — pure Moodle core.
 */
public class AdminUsers extends BasePage {
    public AdminUsers(Page page) {
        super(page);
    }

    public void openAddUser() {
        navigate("/user/editadvanced.php?id=-1&course=1");
        page.locator("#id_username").first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
    }

    /** Fill the add-user form (does not submit). */
    public void fillNewUser(String username, String pass, String first, String last, String email) {
        page.locator("#id_username").fill(username);
        Locator gen = page.locator("#id_createpassword");                 // "Generate password and notify"
        if (gen.count() > 0 && gen.first().isChecked()) gen.first().uncheck();
        page.waitForTimeout(300);
        // #id_newpassword is a Moodle "passwordunmask" input that stays hidden (masked/behind the
        // generate-password toggle), so fill() would time out — set the value via JS + fire events.
        Locator np = page.locator("#id_newpassword");
        if (np.count() > 0) {
            np.first().evaluate("(el, v) => { el.value = v;"
                    + " el.dispatchEvent(new Event('input', {bubbles:true}));"
                    + " el.dispatchEvent(new Event('change', {bubbles:true})); }", pass);
        }
        page.locator("#id_firstname").fill(first);
        page.locator("#id_lastname").fill(last);
        page.locator("#id_email").fill(email);
    }

    public void submit() {
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1_500);
    }

    /** True if the add-user form is showing validation/error messages (still on the form). */
    public boolean hasFormError() {
        String body = page.locator("body").innerText().toLowerCase();
        return page.locator("[id^='id_error_'], .form-control-feedback, .invalid-feedback").count() > 0
                || body.contains("required")
                || body.contains("already") && body.contains("exist")
                || body.contains("must supply")
                || body.contains("invalid");
    }

    /** After a successful create, Moodle leaves the form (goes to the user's profile/preferences). */
    public boolean leftTheForm() {
        return !page.url().contains("editadvanced.php");
    }

    /** The Moodle user id from the current URL (profile/preferences after create/edit), or null. */
    public String userIdFromUrl() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[?&]id=(\\d+)").matcher(page.url());
        return m.find() ? m.group(1) : null;
    }

    public void openUserList() {
        navigate("/admin/user.php");
        page.waitForTimeout(1_500);
    }

    /** Filter the admin user list by email and return the matched user's id (from the edit link), or null. */
    public String findUserIdByEmail(String email) {
        openUserList();
        Locator ef = page.locator("#id_email");                 // user_filtering email text field
        if (ef.count() > 0) {
            ef.first().fill(email);
            Locator apply = page.locator("#id_addfilter");
            if (apply.count() > 0) apply.first().click();
            page.waitForTimeout(1_500);
        }
        Locator edit = page.locator("a[href*='editadvanced.php?id=']");
        int n = Math.min(edit.count(), 15);
        for (int i = 0; i < n; i++) {
            String href = edit.nth(i).getAttribute("href");
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("[?&]id=(\\d+)").matcher(href == null ? "" : href);
            if (m.find()) return m.group(1);                    // skips the id=-1 "add new" link
        }
        return null;
    }

    public boolean userListedByEmail(String email) {
        // Rely on the filtered edit-link lookup only. (A body-text check would false-positive on the
        // email we just typed into the filter field.)
        return findUserIdByEmail(email) != null;
    }

    /**
     * Whether a user's profile page renders (account exists & is viewable). Reliable, id-based —
     * a deleted/invalid user shows an error page instead of the profile. Used as a robust proxy for
     * "listed in the user list" because /admin/user.php is a report-builder table whose AJAX search
     * does not filter deterministically.
     */
    public boolean userProfilePresent(String userId) {
        navigate("/user/profile.php?id=" + userId);
        page.waitForTimeout(1_200);
        String body = page.locator("body").innerText().toLowerCase();
        boolean error = page.locator(".alert-danger, .errorbox").count() > 0
                || body.contains("can not find")
                || body.contains("invalid user")
                || body.contains("account has been deleted");
        boolean profile = page.locator("#page-user-profile, .userprofile, .profile_tree, [data-region='profile']").count() > 0
                || page.locator("#page-header, .page-header-headings").count() > 0;
        return profile && !error;
    }

    /** Set (or clear) the suspended flag on a user's edit form and save. */
    public void setSuspended(String userId, boolean suspend) {
        openEditUser(userId);
        Locator susp = page.locator("#id_suspended");
        if (susp.count() > 0) {
            if (suspend && !susp.first().isChecked()) susp.first().check();
            if (!suspend && susp.first().isChecked()) susp.first().uncheck();
        }
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1_500);
    }

    /** Change a user's email via the edit form and save. */
    public void changeEmail(String userId, String newEmail) {
        openEditUser(userId);
        page.locator("#id_email").fill(newEmail);
        page.locator("#id_submitbutton").click();
        page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);
        page.waitForTimeout(1_500);
    }

    public String currentEmailValue(String userId) {
        openEditUser(userId);
        return page.locator("#id_email").inputValue();
    }

    /** Open a user's admin edit form directly by id. */
    public void openEditUser(String userId) {
        navigate("/user/editadvanced.php?id=" + userId + "&course=1");
        page.locator("#id_email").first().waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE).setTimeout(30_000));
    }
}
