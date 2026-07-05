package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.pages.AdminUsers;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import java.util.ArrayList;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Moodle user administration steps (standard Moodle core, no cloudlabs). Users are seeded/cleaned
 * via Web Services (ApiClient); the create-flow and admin-edit tests drive the admin UI on ctx.page.
 * Login-as-user tests use a fresh (non-admin) browser context.
 */
public class MoodleUsersSteps {
    private final TestContext ctx;
    private final AdminUsers admin;

    private ApiClient.SeededUser user;        // the "registered user" (WS-created)
    private String newEmail;
    private String forcePwUser;
    private String forcePwPass;
    private Page userPage;
    private String profileCity;
    private String editedFirstName;
    private String cohortToDelete;
    private final List<Long> toDelete = new ArrayList<>();
    private BrowserContext freshCtx;

    public MoodleUsersSteps(TestContext ctx) {
        this.ctx = ctx;
        this.admin = new AdminUsers(ctx.page);
    }

    private void requireWs() {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
    }

    // ---- U1: create a user via the admin form ----
    @When("admin creates a new user via the form")
    public void adminCreatesANewUserViaTheForm() {
        admin.openAddUser();
        long stamp = System.currentTimeMillis();
        String username = "autoui_" + stamp;
        String email = username + "@example.com";
        admin.fillNewUser(username, "AutoT@" + stamp + "xZ", "Auto", "UiTest", email);
        admin.submit();
        String id = admin.userIdFromUrl();                 // create redirects to the new user's profile
        if (id != null && !id.equals("-1")) toDelete.add(Long.parseLong(id));
    }

    @Then("the user is created successfully")
    public void theUserIsCreatedSuccessfully() {
        assertTrue(admin.leftTheForm(), "still on the add-user form after submit (create failed)");
    }

    // ---- U2: mandatory fields ----
    @When("admin submits the add-user form empty")
    public void adminSubmitsTheAddUserFormEmpty() {
        admin.openAddUser();
        admin.submit();
    }

    @Then("the form shows validation errors")
    public void theFormShowsValidationErrors() {
        assertFalse(admin.leftTheForm(), "empty form was accepted (should be rejected)");
        assertTrue(admin.hasFormError(), "no validation errors shown on the empty form");
    }

    // ---- Given: a registered user (WS) ----
    @Given("a registered user")
    public void aRegisteredUser() {
        requireWs();
        user = ApiClient.createUser(System.currentTimeMillis());
        toDelete.add(user.id);
    }

    // ---- U3: duplicate username ----
    @When("admin tries to create a user with the same username")
    public void adminTriesToCreateAUserWithTheSameUsername() {
        admin.openAddUser();
        long stamp = System.currentTimeMillis();
        admin.fillNewUser(user.username, "AutoT@" + stamp + "xZ", "Dup", "User", "dup" + stamp + "@example.com");
        admin.submit();
    }

    @Then("the form shows a duplicate-user error")
    public void theFormShowsADuplicateUserError() {
        assertFalse(admin.leftTheForm(), "duplicate username was accepted (should be rejected)");
        assertTrue(admin.hasFormError(), "no duplicate-user error shown");
    }

    // ---- U4: weak password ----
    @When("admin creates a user with a weak password")
    public void adminCreatesAUserWithAWeakPassword() {
        admin.openAddUser();
        long stamp = System.currentTimeMillis();
        admin.fillNewUser("autoweak_" + stamp, "abc", "Weak", "Pass", "weak" + stamp + "@example.com");
        admin.submit();
    }

    @Then("the form shows a password-policy error")
    public void theFormShowsAPasswordPolicyError() {
        assertFalse(admin.leftTheForm(), "weak password was accepted (should be rejected)");
        assertTrue(admin.hasFormError(), "no password-policy error shown");
    }

    // ---- U5 / U27: valid login ----
    @Then("the user can log in with valid credentials")
    public void theUserCanLogInWithValidCredentials() {
        Page p = loginFresh(user.username, user.password);
        assertTrue(loggedIn(p), "registered user could not log in with valid credentials");
    }

    // ---- U6: invalid login ----
    @Then("login with invalid credentials is rejected")
    public void loginWithInvalidCredentialsIsRejected() {
        Page p = loginFresh("no_such_user_" + System.currentTimeMillis(), "WrongPass#123");
        assertFalse(loggedIn(p), "login succeeded with invalid credentials");
    }

    // ---- U21: logout ----
    @Then("the user can log in and then log out")
    public void theUserCanLogInAndThenLogOut() {
        Page p = loginFresh(user.username, user.password);
        assertTrue(loggedIn(p), "user could not log in");
        Object sesskey = p.evaluate("() => (window.M && M.cfg && M.cfg.sesskey) || ''");
        p.navigate(Settings.BASE_URL + "/login/logout.php?sesskey=" + sesskey);
        p.waitForTimeout(1_500);
        assertFalse(loggedIn(p), "user still logged in after logout");
    }

    // ---- U25: listed (verified via the user's profile — the admin list is a report-builder
    // table whose AJAX search does not filter deterministically) ----
    @Then("the user appears in the admin user list")
    public void theUserAppearsInTheAdminUserList() {
        assertTrue(admin.userProfilePresent(String.valueOf(user.id)),
                "registered user account is not present/viewable: id=" + user.id);
    }

    // ---- U26 / U27: suspend / unsuspend ----
    @When("admin suspends the user")
    public void adminSuspendsTheUser() {
        admin.setSuspended(String.valueOf(user.id), true);
    }

    @When("admin unsuspends the user")
    public void adminUnsuspendsTheUser() {
        admin.setSuspended(String.valueOf(user.id), false);
    }

    @Then("the user cannot log in")
    public void theUserCannotLogIn() {
        Page p = loginFresh(user.username, user.password);
        assertFalse(loggedIn(p), "suspended user was able to log in");
    }

    // ---- U30: change email ----
    @When("admin changes the user email")
    public void adminChangesTheUserEmail() {
        newEmail = "changed" + System.currentTimeMillis() + "@example.com";
        admin.changeEmail(String.valueOf(user.id), newEmail);
    }

    @Then("the user email is updated")
    public void theUserEmailIsUpdated() {
        assertEquals(admin.currentEmailValue(String.valueOf(user.id)), newEmail, "email not updated");
    }

    // ---- U10/U11: profile update + reflected ----
    @When("the user updates their profile city")
    public void theUserUpdatesTheirProfileCity() {
        userPage = loginFresh(user.username, user.password);
        assertTrue(loggedIn(userPage), "user could not log in");
        profileCity = "AutoCity" + System.currentTimeMillis();
        userPage.navigate(Settings.BASE_URL + "/user/edit.php");
        userPage.waitForTimeout(1_500);
        userPage.locator("#id_city").fill(profileCity);
        userPage.locator("#id_submitbutton").click();
        userPage.waitForTimeout(2_000);
    }

    @Then("the profile change is reflected")
    public void theProfileChangeIsReflected() {
        userPage.navigate(Settings.BASE_URL + "/user/edit.php");
        userPage.waitForTimeout(1_500);
        assertEquals(userPage.locator("#id_city").inputValue(), profileCity, "profile city change not persisted");
    }

    // ---- U12: invalid profile data ----
    @When("the user saves an invalid email in their profile")
    public void theUserSavesAnInvalidEmailInTheirProfile() {
        userPage = loginFresh(user.username, user.password);
        assertTrue(loggedIn(userPage), "user could not log in");
        userPage.navigate(Settings.BASE_URL + "/user/edit.php");
        userPage.waitForTimeout(1_500);
        userPage.locator("#id_email").fill("notanemail");
        userPage.locator("#id_submitbutton").click();
        userPage.waitForTimeout(1_500);
    }

    @Then("the profile form shows a validation error")
    public void theProfileFormShowsAValidationError() {
        boolean stillOnForm = userPage.url().contains("/user/edit.php");
        boolean err = userPage.locator("[id^='id_error_'], .invalid-feedback, .form-control-feedback").count() > 0
                || userPage.locator("body").innerText().toLowerCase().contains("invalid email");
        assertTrue(stillOnForm && err, "invalid email accepted in profile (url=" + userPage.url() + ")");
    }

    // ---- U22: session after logout ----
    @Then("after the user logs out a protected page requires login")
    public void afterLogoutAProtectedPageRequiresLogin() {
        Page p = loginFresh(user.username, user.password);
        assertTrue(loggedIn(p), "user could not log in");
        Object sk = p.evaluate("() => (window.M && M.cfg && M.cfg.sesskey) || ''");
        p.navigate(Settings.BASE_URL + "/login/logout.php?sesskey=" + sk);
        p.waitForTimeout(1_000);
        p.navigate(Settings.BASE_URL + "/user/edit.php");
        p.waitForTimeout(1_500);
        assertTrue(!loggedIn(p) || p.url().contains("/login/"),
                "protected page did not require login after logout (url=" + p.url() + ")");
    }

    // ---- U15: already enrolled -> course page ----
    @Given("a registered user enrolled in the course")
    public void aRegisteredUserEnrolledInTheCourse() {
        requireWs();
        if (Settings.COURSE_ID.isEmpty()) throw new SkipException("COURSE_ID not set");
        user = ApiClient.createUser(System.currentTimeMillis());
        toDelete.add(user.id);
        ApiClient.enrolUser(user.id, Long.parseLong(Settings.COURSE_ID), 5);
    }

    @Then("the user sees the course page not an enrolment page")
    public void theUserSeesTheCoursePage() {
        Page p = loginFresh(user.username, user.password);
        assertTrue(loggedIn(p), "user could not log in");
        p.navigate(Settings.BASE_URL + "/course/view.php?id=" + Settings.COURSE_ID,
                new Page.NavigateOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(45_000));
        p.waitForTimeout(2_000);
        // enrolled -> stays on the course page; not-enrolled -> redirected to /enrol/index.php
        assertTrue(p.url().contains("/course/view.php") && !p.url().contains("/enrol/"),
                "enrolled user was not taken to the course page (url=" + p.url() + ")");
    }

    // ---- U24: admin edit + delete ----
    @When("admin edits the user first name")
    public void adminEditsTheUserFirstName() {
        editedFirstName = "Edited" + System.currentTimeMillis();
        admin.changeFirstName(String.valueOf(user.id), editedFirstName);
        admin.openEditUser(String.valueOf(user.id));
        assertEquals(ctx.page.locator("#id_firstname").inputValue(), editedFirstName, "first name not updated");
    }

    // (U24 delete reuses the existing "admin deletes the user" step — WS admin delete.)

    // ---- U28: create cohort ----
    @Then("admin can create a cohort that is listed")
    public void adminCanCreateACohortThatIsListed() {
        long s = System.currentTimeMillis();
        String name = "AutoCohort" + s;
        admin.cohortCreate(name, "autocoh" + s);
        cohortToDelete = name;
        assertTrue(admin.cohortListed(name), "cohort not listed after creation: " + name);
    }

    // ---- U29: delete cohort ----
    @Then("admin can create and then delete a cohort")
    public void adminCanCreateAndThenDeleteACohort() {
        long s = System.currentTimeMillis();
        String name = "AutoCohortDel" + s;
        admin.cohortCreate(name, "autocohd" + s);
        assertTrue(admin.cohortListed(name), "cohort not created: " + name);
        admin.cohortDelete(name);
        assertFalse(admin.cohortListed(name), "cohort still listed after delete: " + name);
    }

    // ---- CGAP-U-1: invalid email format ----
    @When("admin creates a user with an invalid email format")
    public void adminCreatesAUserWithAnInvalidEmailFormat() {
        admin.openAddUser();
        long s = System.currentTimeMillis();
        admin.fillNewUser("autobad_" + s, "AutoT@" + s + "xZ", "Bad", "Email", "notanemail");
        admin.submit();
    }

    @Then("the form shows an email-format error")
    public void theFormShowsAnEmailFormatError() {
        assertFalse(admin.leftTheForm(), "invalid email was accepted (should be rejected)");
        assertTrue(admin.hasFormError(), "no email-format error shown");
    }

    // ---- CGAP-U-2: duplicate email ----
    @When("admin tries to create a user with the same email")
    public void adminTriesToCreateAUserWithTheSameEmail() {
        admin.openAddUser();
        long s = System.currentTimeMillis();
        admin.fillNewUser("autodupe_" + s, "AutoT@" + s + "xZ", "Dup", "Email", user.email);
        admin.submit();
    }

    @Then("the form shows a duplicate-email error")
    public void theFormShowsADuplicateEmailError() {
        assertFalse(admin.leftTheForm(), "duplicate email was accepted (should be rejected)");
        assertTrue(admin.hasFormError(), "no duplicate-email error shown");
    }

    // ---- CGAP-U-6: deleted user ----
    @When("admin deletes the user")
    public void adminDeletesTheUser() {
        ApiClient.deleteUser(user.id);
        toDelete.remove(Long.valueOf(user.id));   // avoid double-delete in cleanup
    }

    @Then("the deleted user cannot log in")
    public void theDeletedUserCannotLogIn() {
        Page p = loginFresh(user.username, user.password);
        assertFalse(loggedIn(p), "deleted user was able to log in");
    }

    @Then("the deleted user is not listed")
    public void theDeletedUserIsNotListed() {
        assertFalse(admin.userProfilePresent(String.valueOf(user.id)),
                "deleted user account is still present/viewable: id=" + user.id);
    }

    // ---- CGAP-U-7: force password change ----
    @When("admin creates a user with force password change")
    public void adminCreatesAUserWithForcePasswordChange() {
        admin.openAddUser();
        long s = System.currentTimeMillis();
        forcePwUser = "autofpc_" + s;
        forcePwPass = "AutoT@" + s + "xZ";
        String email = forcePwUser + "@example.com";
        admin.fillNewUser(forcePwUser, forcePwPass, "Force", "Pwd", email);
        ctx.page.locator("#id_preference_auth_forcepasswordchange").check();
        admin.submit();
        String id = admin.userIdFromUrl();
        if (id != null && !id.equals("-1")) toDelete.add(Long.parseLong(id));
    }

    @Then("the user is forced to change password on login")
    public void theUserIsForcedToChangePasswordOnLogin() {
        Page p = loginFresh(forcePwUser, forcePwPass);
        boolean forced = p.url().contains("change_password")
                || p.locator("#id_newpassword1").count() > 0
                || p.locator("body").innerText().toLowerCase().contains("change your password");
        assertTrue(forced, "user was not forced to change password (url=" + p.url() + ")");
    }

    // ---- CGAP-U-8: change own password ----
    @Then("the user can change their own password and log in with it")
    public void theUserCanChangeTheirOwnPassword() {
        Page p = loginFresh(user.username, user.password);
        assertTrue(loggedIn(p), "user could not log in");
        String newPass = "NewAuto@" + System.currentTimeMillis() + "xZ";
        p.navigate(Settings.BASE_URL + "/login/change_password.php");
        p.waitForTimeout(1_500);
        p.locator("#id_password").fill(user.password);
        p.locator("#id_newpassword1").fill(newPass);
        p.locator("#id_newpassword2").fill(newPass);
        p.locator("#id_submitbutton").click();
        p.waitForTimeout(2_000);
        Object sk = p.evaluate("() => (window.M && M.cfg && M.cfg.sesskey) || ''");
        p.navigate(Settings.BASE_URL + "/login/logout.php?sesskey=" + sk);
        p.waitForTimeout(1_000);
        Page p2 = loginFresh(user.username, newPass);
        assertTrue(loggedIn(p2), "user could not log in with the changed password");
    }

    // ---- CGAP-U-9 / U-11 / U-12: deferred ----
    // These depend on modern Moodle UI that resists deterministic automation on this instance:
    //  - /admin/user.php is a report-builder table whose AJAX "query" search does not filter reliably
    //    (searching a known user returns 0 rows; the URL ?query= param is ignored), and email is not a
    //    displayed column, so "filter narrows" (U-11) and "suspended flagged in list" (U-12) can't be
    //    verified from that list yet;
    //  - the cohort index is a JS-rendered table whose Assign link href is an unfilled ":id" template,
    //    so the real cohort id isn't available server-side (U-9).
    // Tracked as gaps; needs dedicated report-builder / cohort-selector work.
    @Then("admin can add and remove the user from a cohort")
    public void adminCanAddAndRemoveTheUserFromACohort() {
        throw new SkipException("deferred: cohort index is JS-rendered (assign href is a ':id' template)"
                + " - needs report id resolution");
    }

    @Then("filtering the user list narrows to that user")
    public void filteringTheUserListNarrowsToThatUser() {
        throw new SkipException("deferred: /admin/user.php report-builder AJAX search does not filter"
                + " deterministically on this instance");
    }

    @Then("the user is shown as suspended in the list")
    public void theUserIsShownAsSuspendedInTheList() {
        throw new SkipException("deferred: report-builder user list - suspended indicator not"
                + " determinable without a reliable list filter");
    }

    // ---- helpers ----
    private Page loginFresh(String u, String pw) {
        if (freshCtx != null) { try { freshCtx.close(); } catch (Throwable ignored) {} }
        freshCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page p = freshCtx.newPage();
        p.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(p, u, pw);
        return p;
    }

    private boolean loggedIn(Page p) {
        String bc = p.locator("body").getAttribute("class");
        return bc != null && !bc.contains("notloggedin");
    }

    @After("@users")
    public void cleanup() {
        if (freshCtx != null) { try { freshCtx.close(); } catch (Throwable ignored) {} freshCtx = null; }
        if (cohortToDelete != null) { try { admin.cohortDelete(cohortToDelete); } catch (Throwable ignored) {} cohortToDelete = null; }
        for (Long id : toDelete) {
            try { ApiClient.deleteUser(id); } catch (Throwable ignored) {}
        }
        toDelete.clear();
        user = null;
    }
}
