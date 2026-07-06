# Moodle roles — cloudlabs capability matrix (R24-R79). Verifies each role's 12 cloudlabs
# capabilities against the plugin db/access.php defaults. @R<n> maps to Moodle_Roles S.No.
# Administrators bypass capability checks (not a definable role) -> not verified here.
# Run: mvnw test -Dcucumber.tags="@rolematrix"
#
# History: on 2026-07-06 this caught Editing-teacher (missing 4 lab caps) and Non-editing-teacher
# (had 2 assessment caps vs R66) deviating from the defaults; after the site config was fixed, all
# five roles match. See Docs/Roles_Findings.md.

Feature: Moodle role capability matrix (cloudlabs)

  # R44-R54 editing teacher, R33-R43 manager, R55-R65 course creator (ALLOW);
  # R66-R77 non-editing teacher (NOT SET); R78-R79 student (PREVENT).
  @rolematrix @roles
  Scenario Outline: The <role> role has the expected cloudlabs capabilities
    Then the "<role>" role id <roleid> has cloudlabs capabilities "<expected>"

    Examples:
      | role                | roleid | expected |
      | Manager             | 1      | allow    |
      | Course creator      | 2      | allow    |
      | Editing teacher     | 3      | allow    |
      | Non-editing teacher | 4      | notset   |
      | Student             | 5      | prevent  |
