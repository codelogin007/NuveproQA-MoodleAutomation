package com.nuvepro.moodle.steps;

import com.nuvepro.moodle.pages.RolePermissions;
import io.cucumber.java.en.Then;

import static org.testng.Assert.assertEquals;

/**
 * Roles module — cloudlabs capability matrix (R24-R79). Verifies each definable role's cloudlabs
 * capabilities against the plugin db/access.php defaults (student=prevent, non-editing-teacher=notset,
 * editingteacher/manager/coursecreator=allow). Site Administrators bypass capability checks entirely
 * (they are users, not a definable role), so their matrix is "has everything" and is not verified here.
 */
public class RolesSteps {
    private final TestContext ctx;
    private final RolePermissions rolePerms;

    public RolesSteps(TestContext ctx) {
        this.ctx = ctx;
        this.rolePerms = new RolePermissions(ctx.page);
    }

    @Then("the {string} role id {int} has cloudlabs capabilities {string}")
    public void roleHasCloudlabsCapabilities(String roleName, int roleId, String expected) {
        rolePerms.openRoleView(roleId);
        StringBuilder mismatches = new StringBuilder();
        for (String cap : RolePermissions.CLOUDLABS_CAPS) {
            String actual = rolePerms.permissionOf(cap);
            if (!actual.equals(expected)) {
                mismatches.append("\n  ").append(cap).append(" = ").append(actual).append(" (expected ").append(expected).append(")");
            }
        }
        assertEquals(mismatches.length(), 0, roleName + " (roleid " + roleId + ") capability mismatches:" + mismatches);
    }
}
