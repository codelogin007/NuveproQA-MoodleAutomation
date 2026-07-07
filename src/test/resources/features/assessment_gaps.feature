# Assessment (challenge) — batch 1, no provisioning: landing presence, config-toggle mechanics
# (CGAP-A-1..4), form validations on the EXISTING activity's edit form (CGAP-A-5/6/7 — validation
# failure saves NOTHING), and student role enforcement (CGAP-A-8).
# Run: mvnw test -Dcucumber.tags="@assessgaps"

Feature: Assessment labs - presence, config and enforcement

  @assesspresence @assessgaps
  Scenario: The assessment landing offers the attempt controls
    When I open the assessment activity
    Then the assessment landing shows the attempt controls and details

  @CGAP-A-1 @assessgaps
  Scenario: Evaluation type toggles the matching config fields
    When I open a new challenge activity form
    Then the evaluation type toggles the matching config fields

  @CGAP-A-2 @assessgaps
  Scenario: Randomization hides the manual content editor
    When I open a new challenge activity form
    Then enabling randomization hides the manual content editor

  @CGAP-A-3 @assessgaps
  Scenario: The challenge content source toggles the git URL field
    When I open a new challenge activity form
    Then switching the challenge content source toggles the git URL field

  @CGAP-A-4 @assessgaps
  Scenario: Cooldown period toggles attempts-after-cooldown
    When I open a new challenge activity form
    Then setting a cooldown period shows attempts-after-cooldown

  @CGAP-A-5 @assessgaps
  Scenario: A challenge due date before the start date is rejected
    When admin sets the challenge due date before the start date
    Then the challenge due-date validation error is shown

  @CGAP-A-6 @assessgaps
  Scenario: A challenge without a passing grade is rejected
    When admin clears the challenge passing grade and saves
    Then the passing-grade validation error is shown

  @CGAP-A-7 @assessgaps
  Scenario: Invalid test-script-parameters JSON is rejected
    When admin enters invalid test script parameters and saves
    Then the test-script-parameters validation error is shown

  @CGAP-A-8 @assessgaps
  Scenario: A student cannot access the assessment admin pages
    Given a student is enrolled in the assessment course
    When the student opens the assessment activity
    Then the student sees no assessment admin controls and cannot open manage challenges

  # ---- FAIL BY DESIGN: flags a REAL permission defect found 2026-07-07 — the Assessment Problem
  # ---- Statements admin page renders its table for a STUDENT via direct URL (the landing button is
  # ---- hidden per A249, but the PAGE is not capability-protected). Red until fixed.
  @CGAP-A-8 @assessdeviation @assessgaps
  Scenario: DEVIATION - the problem statements admin page must be blocked for students
    Given a student is enrolled in the assessment course
    When the student opens the assessment activity
    Then the problem statements admin page is blocked for the student
