package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

import java.util.regex.Pattern;

/**
 * Role capability inspection via the role definition VIEW page
 * (/admin/roles/define.php?action=view&roleid=N). Each capability appears in a row whose text
 * includes the capability name and its permission (Allow / Prevent / Prohibit); not-set
 * capabilities are absent. Used to verify the cloudlabs capability matrix per role.
 */
public class RolePermissions extends BasePage {
    /** The 12 cloudlabs capabilities (from the plugin db/access.php). */
    public static final String[] CLOUDLABS_CAPS = {
            "mod/cloudlabs:activityuserlabs",
            "mod/cloudlabs:activityuserassessments",
            "mod/cloudlabs:activityuseroverrides",
            "mod/cloudlabs:activityuserlabscreate",
            "mod/cloudlabs:activityuserlabsdelete",
            "mod/cloudlabs:activityuserlabsstartstop",
            "mod/cloudlabs:activityuserassessmentsmanageattempts",
            "mod/cloudlabs:activityuserassessmentsupdategrades",
            "mod/cloudlabs:activityuserassessmentssubmit",
            "mod/cloudlabs:activityuserassessmentsdeleteattempts",
            "mod/cloudlabs:activityuserguidedprojects",
            "mod/cloudlabs:viewlabreadiness",
    };

    public RolePermissions(Page page) {
        super(page);
    }

    public void openRoleView(int roleId) {
        navigate("/admin/roles/define.php?action=view&roleid=" + roleId);
        page.locator("table").first().waitFor();
        page.waitForTimeout(1_000);
    }

    /** Permission of a capability on the currently-open role view: allow / prevent / prohibit / notset.
     *  Uses indexOf + a boundary check (no regex) against whitespace-normalised row text. */
    public String permissionOf(String cap) {
        Object res = page.evaluate(
                "(cap) => {"
                + " for (const r of document.querySelectorAll('tr')) {"
                + "   const t = (r.textContent||'').replace(/\\s+/g,' ');"
                + "   const i = t.indexOf(cap);"
                + "   if (i >= 0) {"
                + "     const after = t.charAt(i + cap.length);"
                // permission words (Allow/Prevent/Prohibit) start UPPERCASE and are glued to the cap;
                // the longer caps (…labscreate/delete/startstop) continue with a LOWERCASE letter.
                + "     if (!/[a-z]/.test(after)) {"
                + "       if (t.includes('Allow')) return 'allow';"
                + "       if (t.includes('Prohibit')) return 'prohibit';"
                + "       if (t.includes('Prevent')) return 'prevent';"
                + "       return 'notset';"
                + "     }"
                + "   }"
                + " } return 'notset'; }", cap);
        return String.valueOf(res);
    }
}
