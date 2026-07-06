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

  @G5 @groups
  Scenario: The groups dropdown defaults to All Groups
    When admin creates a group and opens Manage User Labs
    Then the groups dropdown defaults to all groups

  @G2 @groups
  Scenario: Admin can select a single group in the dropdown
    When admin creates a group and opens Manage User Labs
    Then admin can select a single group in the dropdown

  @G4 @groups
  Scenario: Admin can search a group in the dropdown
    When admin creates a group and opens Manage User Labs
    Then admin can search for the group in the dropdown

  @G42 @groups
  Scenario: The groups column is present on Manage User Labs
    When admin creates a group and opens Manage User Labs
    Then the groups column is present on Manage User Labs

  # G3 - the groups dropdown is single-select, so multiple-group selection is not supported (skips).
  @G3 @groups
  Scenario: Multiple-group selection is not supported (single-select dropdown)
    When admin creates a group and opens Manage User Labs
    Then selecting multiple groups is not supported

  # ---- cohorts + members (G11, G13, G24) ----

  @G11 @groups
  Scenario: Admin can create a cohort
    When admin creates a cohort
    Then the cohort is listed

  @G13 @groups
  Scenario: Admin can create cohorts with the same name (allowed, unlike groups)
    When admin creates two cohorts with the same name
    Then both cohorts with that name exist

  @G24 @groups
  Scenario: Admin can add the same user to multiple groups
    When admin adds the same user to two groups
    Then the user is a member of both groups
