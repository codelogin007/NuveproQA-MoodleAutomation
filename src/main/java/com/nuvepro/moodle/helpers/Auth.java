package com.nuvepro.moodle.helpers;

import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;

/** Login helpers. UI login for the flow under test. Mirrors helpers/auth.py. */
public final class Auth {
    private Auth() {}

    public static void uiLogin(Page page, String username, String password) {
        // Retry once and use DOMCONTENTLOADED + a settle wait (the dashboard polls, so
        // networkidle never fires; login can also flake transiently under a slow instance).
        for (int attempt = 1; attempt <= 2; attempt++) {
            page.navigate(Settings.BASE_URL + "/login/index.php",
                    new com.microsoft.playwright.Page.NavigateOptions().setTimeout(45_000)
                            .setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
            page.locator("#username").fill(username);
            page.locator("#password").fill(password);
            page.locator("#loginbtn").click();
            page.waitForTimeout(2_500);
            String bodyClass = page.locator("body").getAttribute("class");
            if (bodyClass == null || !bodyClass.contains("notloggedin")) {
                return; // logged in
            }
        }
        // Left on the login page after retries; the calling test will assert/skip accordingly.
    }

    public static void loginAsAdmin(Page page) {
        uiLogin(page, Settings.ADMIN_USER, Settings.ADMIN_PASS);
    }

    public static void loginAsStudent(Page page) {
        uiLogin(page, Settings.STUDENT_USER, Settings.STUDENT_PASS);
    }
}
