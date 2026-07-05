# Playground lab state-action lifecycle (PG-12/15/17/18/19), migrated to the MANDATORY method:
# fresh per-user lab, lab id captured at creation, STATUS polled via the CloudLabs API (logged),
# actions via Manage Labs, ONE lab shared across the ordered scenarios, deleted by PG-19 / teardown.
# Run: mvnw test -Dcucumber.tags="@stateactions"
# (These REPLACE the old shared-lab-lifecycle versions of the same PG numbers.)

Feature: Playground lab state actions (start / stop / delete)

  @PG-12 @stateactions @provisioning
  Scenario: Start provisions a running lab
    Given a fresh enrolled user with a running lab
    Then the shared lab is Running

  @PG-38 @stateactions @provisioning
  Scenario: Sync Status refreshes the lab status from the provider
    Given the shared lab
    When I sync the shared lab
    Then the shared lab sync completes without error

  @PG-15 @stateactions @provisioning
  Scenario: Stop stops the running lab
    Given the shared lab
    When I stop the shared lab
    Then the shared lab is Stopped

  @PG-17 @PG-35 @stateactions @provisioning
  Scenario: Start from Manage Labs restarts a stopped lab to Running
    Given the shared lab
    When I start the shared lab
    Then the shared lab is Running

  @PG-18 @stateactions @provisioning
  Scenario: Stop from Manage Labs stops the running lab
    Given the shared lab
    When I stop the shared lab
    Then the shared lab is Stopped

  @PG-19 @stateactions @provisioning
  Scenario: Delete from Manage Labs deletes the lab
    Given the shared lab
    When I delete the shared lab
    Then the shared lab is Deleted

  @PG-49 @stateactions @provisioning
  Scenario: A deleted lab shows the deleted state
    Given the shared lab
    Then the shared lab shows the deleted state

  @PG-33 @PG-34 @stateactions @provisioning
  Scenario: A new lab can be created from Manage Labs after deletion
    Given the shared lab
    When I create a new lab for the shared user after deletion
    Then the shared lab is Running
