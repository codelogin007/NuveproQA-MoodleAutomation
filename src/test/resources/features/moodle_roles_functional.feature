# Moodle roles — functional subset: R2 (admin site access), R3 (assign/remove role),
# R17 (set system manager). Users seeded/cleaned via Web Services. Assignment at system context.
# Run: mvnw test -Dcucumber.tags="@rolefunc"

Feature: Moodle role functional actions

  @R2 @rolefunc @roles
  Scenario: Admin can access Site Administration
    Then admin can access site administration

  @R17 @roleassign @rolefunc @roles
  Scenario: Admin can set a new user as a system manager
    When admin assigns the Manager role to a new user at system level
    Then the user is a system manager

  @R3 @roleassign @rolefunc @roles
  Scenario: Admin can assign and remove a role for a user
    When admin assigns the Manager role to a new user at system level
    Then the user is a system manager
    When admin removes the Manager role from the user
    Then the user is no longer a system manager
