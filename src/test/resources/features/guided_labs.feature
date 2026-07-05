# Guided (practice project) labs — first batch (landing + admin + config presence checks).
# Uses GUIDED_CMID (an existing guided activity). No lab provisioning here — read-only checks.
# Lab-action cases (G14-17 + CGAP-PGG-8/9) come next, on the fresh-user + API method.
# @PGG-<n> maps to MoodleRegression_Testing.xlsx > Moodle_Guided_Labs S.No <n>.
# Run: mvnw test -Dcucumber.tags="@guided"

Feature: Guided (practice project) labs

  @PGG-4 @guided
  Scenario: The guided landing offers a way to start the project
    Given I open the guided activity
    Then the guided landing offers a start option

  @PGG-5 @guided
  Scenario: The guided landing shows the project dates
    Given I open the guided activity
    Then the guided landing shows the project dates

  @PGG-6 @guided
  Scenario: Admin sees the User Labs and User Guided Project options
    Given I open the guided activity
    Then admin sees the guided manage buttons

  @PGG-10 @PGG-12 @guided
  Scenario: The User Guided Project page lists participants and their dates
    Given I open the guided activity
    When I open the User Guided Project page
    Then the participants progress table is shown with date columns

  @PGG-9 @guided
  Scenario: The guided User Labs page lists the labs
    Given I open the guided activity
    When I open the guided User Labs page
    Then the guided labs are listed

  @PGG-2 @PGG-3 @guided
  Scenario: The guided activity form has the script URL and date fields
    When I open a new guided activity form
    Then the guided script URL and start and end date fields are present
