# Moodle groups — first batch: create groups (G12), duplicate names (G14), and the cloudlabs groups
# filter dropdown on Manage User Labs (G1). Groups deleted at teardown.
# @G<n> maps to MoodleRegression_Testing.xlsx > Groups_Testcases.
# Run: mvnw test -Dcucumber.tags="@groups"

Feature: Moodle course groups

  @G12 @groups
  Scenario: Admin can create a group
    When admin creates a group
    Then the group is listed in the course

  # G14 - the manual expects duplicate group names to be allowed, but standard Moodle enforces UNIQUE
  # group names within a course, so the second create is correctly rejected. Asserting real behavior.
  @G14 @groups
  Scenario: Duplicate group names are rejected within a course
    When admin creates a group then attempts a duplicate name
    Then the duplicate group name is rejected

  @G1 @groups
  Scenario: The groups dropdown is present on Manage User Labs
    When admin creates a group and opens Manage User Labs
    Then the groups dropdown is present and lists the group
