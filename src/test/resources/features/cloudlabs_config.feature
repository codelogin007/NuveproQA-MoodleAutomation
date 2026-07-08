# Cloudlabs config sheets — Content_Integration_Git_URL (#4) + Difficulty_Level_Configuration (#5).
# All no-provisioning form/config checks; NO save (activity add-form toggles + admin-settings presence).
# NOTE: the manual expects difficulty levels Beginner/Practitioner/Proficient/Expert, but the actual
# configured levels are Beginner/Intermediate/Advanced/Expert (admin-configurable via
# cloudlabs/difficulty_levels) - a spec discrepancy; we assert configurable-levels-present + Beginner
# default. Run: mvnw test -Dcucumber.tags="@cloudlabsconfig"

Feature: Cloudlabs activity config - description source and difficulty level

  @contentsource @cloudlabsconfig
  Scenario: The description source toggles the manual editor and the git URL field
    When I open a new cloudlabs activity form
    Then the description source toggles the git URL field

  @difficulty @cloudlabsconfig
  Scenario: The activity difficulty level offers the configured levels and defaults to Beginner
    When I open a new challenge activity form for config
    Then the difficulty level select offers the configured levels and defaults to Beginner

  @difficulty @cloudlabsconfig
  Scenario: The Difficulty Level Settings section is present on Cloud Server Settings
    When admin opens the cloud server settings
    Then the difficulty level settings section is present
