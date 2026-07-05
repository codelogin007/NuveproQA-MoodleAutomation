# Lab actions on ONE per-user lab, provisioned once and deleted once (efficient, expiry-proof).
# Actions = Stop, Start (state-changing, via Manage Labs, cp.php-free).
# Read-only = Lab Usage, Info, Instructions, Credentials, Notes (on the console; tolerant of cp.php).
# The two scenarios SHARE the lab (ordered); @AfterAll deletes the lab + user once.
# Run: mvnw test -Dcucumber.tags="@labactions"

Feature: Per-user lab actions, before and after expiry/extension

  @labactions @provisioning
  Scenario: Actions on a fresh running lab
    Given a fresh enrolled user with a running lab
    Then the shared lab read-only views are shown
    When I stop the shared lab
    Then the shared lab is Stopped
    When I start the shared lab
    Then the shared lab is Running

  @labactions @provisioning
  Scenario: Actions after expiry and extension on the same lab
    Given the shared lab
    When I expire the shared lab
    Then the shared lab shows expired
    When I extend the shared lab duration by 1 hour
    Then the shared lab is usable again
    And the shared lab read-only views are shown
    When I stop the shared lab
    Then the shared lab is Stopped
    When I start the shared lab
    Then the shared lab is Running
