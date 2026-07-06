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
