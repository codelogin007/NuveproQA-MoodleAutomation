# Pre-Delivery Lab Readiness Checklist (lab_readiness.php) — batch 1, no provisioning.
# Role gate (cap mod/cloudlabs:viewlabreadiness — admin yes, student no), checklist progress bar
# (100% -> green bg-success), env-type VM/Account field toggle, mandatory support-engineer validation
# (alert, blocks save -> nothing persists). NONE of these do a SUCCESSFUL save, so the shared
# activity (ASSESSMENT_CMID) is never mutated. Run: mvnw test -Dcucumber.tags="@predelivery"

Feature: Pre-Delivery Lab Readiness Checklist

  @PDL-role @predelivery
  Scenario: The Lab Readiness page is admin-only
    When admin opens the lab readiness page
    Then the lab readiness checklist form is shown
    And a student cannot open the lab readiness page

  @PDL-progress @predelivery
  Scenario: Completing all checklist items drives the progress bar to 100% green
    When admin opens the lab readiness page
    Then checking all visible checklist items sets the progress bar to 100 percent and green

  @PDL-envtype @predelivery
  Scenario: The environment type toggles the VM and Account fields
    When admin opens the lab readiness page
    Then switching the environment type toggles the VM and Account fields

  @PDL-validation @predelivery
  Scenario: Saving without a support engineer name is rejected
    When admin opens the lab readiness page
    Then saving with an empty support engineer name is blocked by an alert
