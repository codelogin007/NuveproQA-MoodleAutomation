# Assessment (challenge) lifecycle — ONE provisioned lab with REAL evaluation: start attempt ->
# control panel -> lab RUNNING (CloudLabs API) -> submit for evaluation (the engine runs the
# configured S3-Life-Cycle scripts; a fresh student passes 0 tests -> grade 0/Fail is EXPECTED) ->
# attempt reaches Evaluated. This is the proven, high-value core of the assessment module.
# Teardown deletes the student (cascade-deletes the lab).
# Run: mvnw test -Dcucumber.tags="@assesslab"   (provisions a REAL lab; evaluation takes minutes)

Feature: Assessment lab lifecycle with evaluation

  @assesslab @provisioning
  Scenario: A student submits an assessment attempt and it evaluates end to end
    Given a student is enrolled in the assessment course
    When the student starts the assessment
    Then the assessment control panel is shown
    And the assessment lab reaches Running
    When the student submits the assessment attempt
    Then the attempt is evaluated

  # Verified behavior: this activity AUTO-COMPLETES once the (final) attempt evaluates -
  # isChallengeEnded flips to 1, the final result (Fail / grade 0 for a fresh student) is rendered,
  # and Retake is offered. Covers the completion + final-result outcome (also A127). The consent-gate
  # dialog (CGAP-A-9) stays in the DOM; we verify its wiring (final submit disabled until consent).
  @CGAP-A-9 @assesscomplete @provisioning
  Scenario: The assessment auto-completes with a final result and the consent gate is wired
    Given a student is enrolled in the assessment course
    When the student starts the assessment
    Then the assessment control panel is shown
    And the assessment lab reaches Running
    When the student submits the assessment attempt
    Then the attempt is evaluated
    And the assessment auto-completes with a final result
    And the complete-assessment consent gate is present and wired
