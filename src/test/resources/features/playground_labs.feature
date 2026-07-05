# Playground (sandbox) lab regression — all High/Medium cases as BDD scenarios.
# Each @PG-<n> tag maps 1:1 to MoodleRegression_Testing.xlsx > 'Moodle_Playground_Labs' S.No.
# A scenario may carry TWO case tags when one operation genuinely covers both
# (e.g. @PG-33 @PG-34: create-from-Manage-Labs after delete).
#
# @provisioning scenarios spin a REAL cloud lab (run via: mvn test -Pslow). They share ONE
# lab and run as an ORDERED LIFECYCLE (feature-file order matters):
#   start (PG-12) -> running-console checks -> stop (PG-15) -> restart via Manage Labs
#   (PG-17/35) -> stop via Manage Labs (PG-18) -> sync (PG-38) -> delete (PG-19) ->
#   deleted state (PG-49) -> create-after-delete (PG-33/34) -> student lifecycle (PG-23)
#   -> bulk (PG-21). GlobalHooks @AfterAll stops any leftover lab.
# Low-priority cases are deferred (see TODO.md). Config-gated cases skip with a reason.

Feature: Playground (sandbox) labs

  @PG-1 @smoke
  Scenario: Admin can create a playground activity
    When I create a new playground activity
    Then the playground activity is created and then removed

  @PG-2
  Scenario: Manage Labs exposes per-lab actions
    Given I open the playground activity
    When I open Manage Labs
    Then per-lab action controls are available

  # PG-8 migrated to playground_console.feature (fresh lab + seeded-user console).

  @PG-9
  Scenario: Create a lab via catalog UI input
    When I open the catalog-UI lab console
    Then the catalog UI input surface is shown

  @PG-10
  Scenario: CloudLabs course progress report loads
    When I open the CloudLabs course progress report
    Then the report loads without errors

  @PG-11
  Scenario: Manage Labs lists labs with status counters
    Given I open the playground activity
    When I open Manage Labs
    Then the labs are listed with status counters

  # ---------- gap-fill (non-destructive, admin+student, Account templates) ----------

  @PG-20 @augmented
  Scenario: Manage Labs actions are disabled with no lab selected
    Given I open the playground activity
    When I open Manage Labs
    Then Start, Stop and Delete are disabled until a lab is selected

  @PG-24 @augmented
  Scenario: Admin can manage labs but a student cannot
    Given I open the playground activity
    Then admin sees the Manage Labs option
    And a student does not see the Manage Labs option

  @PG-37 @augmented
  Scenario: Lab Details shows the selected lab information
    Given I open the playground activity
    When I open Manage Labs
    And I open Lab Details for a lab
    Then the lab details modal is shown

  @PG-39 @augmented
  Scenario: Not-created users tab offers Create but not lab actions
    Given I open the playground activity
    When I open Manage Labs
    Then the not-created users tab offers Create but not lab actions

  @PG-40 @augmented
  Scenario: Cancelling the Start Sandbox confirmation creates no lab
    Given I open the playground activity
    When I click Start Sandbox and cancel the confirmation
    Then no lab is created

  @PG-41 @augmented
  Scenario: Continue resumes the existing lab
    Given I open the playground activity
    When I continue the existing lab
    Then the lab control panel is shown

  @PG-42 @augmented
  Scenario: Landing shows the lab details fields
    Given I open the playground activity
    Then the landing shows the lab type and sandbox id

  @PG-32 @augmented
  Scenario: Course progress report shows playground lab data
    When I open the CloudLabs course progress report
    Then the report lists courses with lab activity columns

  # ================= provisioning lifecycle (REAL cloud lab, ordered) =================

  # PG-12 migrated to playground_actions.feature (fresh-lab + API polling).

  # PG-3 migrated to playground_console.feature.

  @PG-25 @provisioning @augmented
  Scenario: Lab Usage shows valid cost values
    Given a running playground lab
    When I open the Lab Usage popup
    And I request the lab cost
    Then valid cost values are displayed

  @PG-26 @provisioning @augmented
  Scenario: Landing shows allotted, consumed and remaining duration
    Given a running playground lab
    When I open the playground activity
    Then the landing shows consumed and remaining duration values

  # PG-5/6 migrated to playground_console.feature.

  @PG-56 @provisioning @augmented
  Scenario: Instructions and lab info content are present
    Given a running playground lab
    Then the instructions content is not empty

  # PG-45 migrated to playground_console.feature.

  @PG-46 @provisioning @augmented
  Scenario: Lab password can be changed
    Given a running playground lab
    When I open the Lab Credentials popup
    And I change the lab password
    Then the lab password update is accepted

  @PG-7 @provisioning
  Scenario: Speed test button works when enabled
    Given a running playground lab
    Then the speed test button works when enabled

  @PG-13 @provisioning
  Scenario: Jump to Console opens the access popup
    Given a running playground lab
    When I click Jump to Console
    Then the access lab popup or a console tab opens

  @PG-14 @provisioning
  Scenario: Access lab redirect entry is present
    Given a running playground lab
    Then a platform access control is present

  @PG-30 @provisioning @augmented
  Scenario: Access lab opens the cloud platform console
    Given a running playground lab
    When I open a platform console from the access panel
    Then a platform console tab opens outside Moodle

  @PG-27 @provisioning @augmented
  Scenario: A saved note persists after reloading the console
    Given a running playground lab
    When I save the note "persist check 456"
    And I reload the lab console
    Then the saved note should be "persist check 456"

  @PG-16 @PG-29 @provisioning
  Scenario: Refresh lab keeps the lab running
    Given a running playground lab
    When I refresh the lab
    Then the lab remains running

  @PG-4 @provisioning
  Scenario: VM lab loads inside an iframe
    Then the VM lab loads inside an iframe

  # PG-15/17/35/18 migrated to playground_actions.feature (fresh-lab + API polling).

  # ---- Manage Labs action completions & state transitions ----

  # PG-38/19/49/33/34 migrated to playground_actions.feature (fresh-lab + API polling).

  # PG-23 migrated to playground_student.feature (fresh lab + student console + API).

  # PG-21 migrated to playground_bulk.feature (two fresh labs + API-verified bulk stop).

  # ---- config-gated (skip with a reason until the precondition exists) ----

  @PG-31 @provisioning @augmented
  Scenario: Starting a lab with a misconfigured template surfaces an error
    Then starting a misconfigured-template lab surfaces a clear error

  @PG-50 @provisioning @augmented
  Scenario: An expired-duration lab shows the expired message
    Then the expired-duration lab state is verified

  @PG-54 @provisioning @augmented
  Scenario: An unlimited-duration lab does not expire
    Then the unlimited-duration lab is verified

  @PG-55 @provisioning @augmented
  Scenario: A provider-side failure surfaces a clear error
    Then the provider-failure handling is verified
