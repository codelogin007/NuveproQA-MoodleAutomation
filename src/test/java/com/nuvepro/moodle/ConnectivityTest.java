package com.nuvepro.moodle;

import com.nuvepro.moodle.helpers.Auth;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;

/**
 * Smoke tests validating the environment is wired correctly.
 * Login success = absence of Moodle's 'notloggedin' body class (this theme adds no
 * 'userloggedin' class). Mirrors tests/test_connectivity.py.
 */
public class ConnectivityTest extends BaseTest {

    private boolean loggedIn() {
        String bodyClass = page.locator("body").getAttribute("class");
        return bodyClass == null || !bodyClass.contains("notloggedin");
    }

    @Test(groups = {"smoke"}, description = "PG connectivity: admin can log in")
    public void adminCanLogin() {
        Auth.loginAsAdmin(page);
        assertFalse(page.locator("body").getAttribute("class").contains("notloggedin"),
                "Admin not logged in; URL=" + page.url());
    }

    @Test(groups = {"smoke"}, description = "connectivity: student can log in")
    public void studentCanLogin() {
        Auth.loginAsStudent(page);
        assertFalse(page.locator("body").getAttribute("class").contains("notloggedin"),
                "Student not logged in; URL=" + page.url());
    }
}
