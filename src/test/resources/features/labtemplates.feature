# Lab Templates (cloudlabs labtemplates.php / addlabtemplate.php) — batch 1, no persistence.
# List page + columns, add-form config fields, and the site-admin-only gate (require_login +
# is_siteadmin). Create/edit/delete of a template (mutates core config) = batch 2.
# Run: mvnw test -Dcucumber.tags="@labtemplates"

Feature: Lab Templates management

  @LT-list @labtemplates
  Scenario: The lab templates list shows templates with plan and team columns
    When admin opens the lab templates list
    Then the lab templates table lists templates with plan and team columns

  @LT-addform @labtemplates
  Scenario: The add lab template form renders its configuration fields
    When admin opens the add lab template form
    Then the add lab template form shows the configuration fields

  @LT-role @labtemplates
  Scenario: Lab templates are accessible only to a site admin
    When admin opens the lab templates list
    Then a student cannot open the lab templates list
