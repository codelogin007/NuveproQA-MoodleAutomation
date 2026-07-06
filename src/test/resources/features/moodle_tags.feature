# Moodle tags — first batch: tag creation via the activity edit form (standard Moodle, no Orgo).
# @T<n> maps to MoodleRegression_Testing.xlsx > Moodle_Tags S.No <n>.
# Uses PLAYGROUND_CMID as the activity under edit. Tag names are fixed (reused, not accumulated).
# Run: mvnw test -Dcucumber.tags="@tags"

Feature: Moodle activity tags

  @T1 @tags
  Scenario: The Tags field is available on the activity form
    When I open the activity edit form
    Then the Tags field is available

  @T38 @tags
  Scenario: An activity can be saved without adding tags
    When I open the activity edit form
    And I save the activity without changing tags
    Then the activity is saved

  @T6 @tags
  Scenario: A numeric tag can be added to an activity
    When I open the activity edit form
    And I add the tag "20260706"
    And I save the activity
    Then the tag is present on the activity

  @T7 @tags
  Scenario: An alphanumeric tag can be added to an activity
    When I open the activity edit form
    And I add the tag "AutoTagAlpha"
    And I save the activity
    Then the tag is present on the activity

  @T27 @tags
  Scenario: A tag with the same name as an existing one is accepted
    When I open the activity edit form
    And I add the tag "AutoTagAlpha"
    And I save the activity
    Then the tag is present on the activity

  # ---- input variations (T2-T15) ----

  @T2 @tags
  Scenario: A 1-character tag can be added
    When I open the activity edit form
    And I add a tag of 1 characters
    And I save the activity
    Then the tag is present on the activity

  @T3 @tags
  Scenario: A 49-character tag can be added
    When I open the activity edit form
    And I add a tag of 49 characters
    And I save the activity
    Then the tag is present on the activity

  @T4 @tags
  Scenario: A 50-character tag can be added
    When I open the activity edit form
    And I add a tag of 50 characters
    And I save the activity
    Then the tag is present on the activity

  @T5 @tags
  Scenario: A 51-character tag can be added
    When I open the activity edit form
    And I add a tag of 51 characters
    And I save the activity
    Then the tag is present on the activity

  @T8 @tags
  Scenario: A tag with special characters can be added
    When I open the activity edit form
    And I add the tag "tag-name_1.2"
    And I save the activity
    Then the tag is present on the activity

  @T9 @tags
  Scenario: A number-and-alphabetic tag can be added
    When I open the activity edit form
    And I add the tag "abc123def"
    And I save the activity
    Then the tag is present on the activity

  @T10 @tags
  Scenario: A number-and-special-character tag can be added
    When I open the activity edit form
    And I add the tag "123-45_6"
    And I save the activity
    Then the tag is present on the activity

  @T11 @tags
  Scenario: An alphabetic-and-special-character tag can be added
    When I open the activity edit form
    And I add the tag "abc-de_f"
    And I save the activity
    Then the tag is present on the activity

  @T12 @tags
  Scenario: A number-special-alphabetic tag can be added
    When I open the activity edit form
    And I add the tag "ab1-c2_d"
    And I save the activity
    Then the tag is present on the activity

  @T13 @tags
  Scenario: A blank tag is not created
    When I open the activity edit form
    And I add a blank tag
    Then no tag is added

  @T14 @tags
  Scenario: A tag with leading and trailing spaces is trimmed
    When I open the activity edit form
    And I enter the tag "  Spacedtag  "
    And I save the activity
    Then the trimmed tag "Spacedtag" is present on the activity

  @T15 @tags
  Scenario: Comma-separated values create multiple tags
    When I open the activity edit form
    And I enter the tag "commaone,commatwo"
    And I save the activity
    Then both tags "commaone" and "commatwo" are present on the activity
