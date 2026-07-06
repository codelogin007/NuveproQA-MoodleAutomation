# Moodle tag management (/tag/manage.php) — rename / delete / multi-delete / combine / standard.
# Report-builder table with inplace-edit cells + bulk actions. Tags created via "Add standard tags"
# and cleaned up by the tests. @T<n> maps to MoodleRegression_Testing.xlsx > Moodle_Tags.
# Run: mvnw test -Dcucumber.tags="@tagmgmt"

Feature: Moodle tag management

  @T16 @tagmgmt
  Scenario: Admin can rename a tag
    Given a new standard tag
    When admin renames the tag
    Then the renamed tag exists and the old name does not

  @T18 @tagmgmt
  Scenario: Admin can delete a tag
    Given a new standard tag
    When admin deletes the tag
    Then the tag no longer exists

  @T28 @tagmgmt
  Scenario: Admin can delete multiple tags at once
    Given two new standard tags
    When admin deletes both tags
    Then neither tag exists

  @T24 @tagmgmt
  Scenario: Admin can combine two tags
    Given two new standard tags
    When admin combines the two tags keeping the first
    Then only the surviving tag remains

  @T25 @tagmgmt
  Scenario: Admin can combine more than two tags
    Given three new standard tags
    When admin combines the three tags keeping the first
    Then only the first of the three tags remains

  @T29 @tagmgmt
  Scenario: Combining with a single tag selected leaves it unchanged
    Given a new standard tag
    When admin tries to combine a single selected tag
    Then the single tag is unchanged

  # ---- standard tags in the activity autocomplete (T19-T23, T26) ----

  @T19 @tagmgmt
  Scenario: A standard tag is suggested in the activity tag field
    Given a standard tag exists
    Then the tag is suggested in the activity tag field

  @T20 @tagmgmt
  Scenario: All standard tags are suggested in the activity tag field
    Given two standard tags exist
    Then both tags are suggested in the activity tag field

  @T21 @tagmgmt
  Scenario: A deleted tag is no longer suggested
    Given a standard tag exists
    When admin deletes the tag
    Then the tag is not suggested in the activity tag field

  @T22 @tagmgmt
  Scenario: A tag with its standard mark removed is no longer suggested
    Given a standard tag exists
    When admin removes the standard mark from the tag
    Then the tag is not suggested in the activity tag field

  @T23 @tagmgmt
  Scenario: A renamed tag is suggested under its new name
    Given a standard tag exists
    When admin renames the tag
    Then the renamed tag is suggested and the old name is not

  @T26 @tagmgmt
  Scenario: A merged (combined) tag is no longer suggested
    Given two standard tags exist
    When admin combines the two tags keeping the first
    Then the surviving tag is suggested and the merged tag is not
