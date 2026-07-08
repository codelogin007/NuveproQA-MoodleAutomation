# Assessment (challenge) admin surface — batch 3, no provisioning. Read-only presence of the admin
# management pages on ASSESSMENT_CMID=781 (managechallenges + user-attempts drill-down + problem
# statements). Covers A11/A16 (users listed with attempt columns), the override + report controls
# (A45-A54 / A103 surface), the user-attempts drill-down, and A249-admin (admin CAN open the problem
# statements page - the student-blocked counterpart is @assessdeviation).
# Run: mvnw test -Dcucumber.tags="@assessadmin"

Feature: Assessment admin management surface

  @A11 @assessadmin
  Scenario: Manage challenges lists enrolled users with attempt columns
    When admin opens manage challenges
    Then the manage challenges table lists users with attempt columns

  @A-override @assessadmin
  Scenario: The override, report and user-attempt controls are present
    When admin opens manage challenges
    Then the override report and user-attempt controls are present

  @A-userattempts @assessadmin
  Scenario: Admin can open a user's attempts drill-down
    When admin opens manage challenges
    Then admin can open the user attempts drill-down

  @A249admin @assessadmin
  Scenario: Admin can open the assessment problem statements page
    When admin opens the assessment problem statements page
    Then the problem statements admin page renders for admin
