# Playground console READ-ONLY views (PG-3/5/6/45/8), migrated to the standard method:
# ONE fresh per-user lab, duration EXTENDED up front (CloudLabs API) so it can't expire mid-batch,
# console opened once as the seeded user; each view checked on that shared console. Deleted once.
# Read-only views live on the console (cp.php) → each check is TOLERANT (skips if unavailable).
# Run: mvnw test -Dcucumber.tags="@console"

Feature: Playground console read-only views

  @console @provisioning
  Scenario: A running lab console is available (duration extended)
    Given a fresh enrolled user with a running lab
    When I extend the shared lab duration by 2 hours
    And the seeded user opens the lab console

  @PG-3 @console @provisioning
  Scenario: Lab Usage shows a cost value
    Given the seeded user console is open
    Then the Lab Usage view shows a cost value

  @PG-5 @console @provisioning
  Scenario: Lab Info is shown
    Given the seeded user console is open
    Then the Lab Info view is shown

  @PG-6 @console @provisioning
  Scenario: Instructions are shown
    Given the seeded user console is open
    Then the Instructions view is shown

  @PG-45 @console @provisioning
  Scenario: Lab Credentials are shown
    Given the seeded user console is open
    Then the Lab Credentials view is shown

  @PG-8 @console @provisioning
  Scenario: A note can be saved and persists
    Given the seeded user console is open
    When I save a note "automation persist 789"
    Then the saved note persists after reopening the console
