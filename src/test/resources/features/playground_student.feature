# PG-23: a STUDENT manages their own lab from the console (fresh per-user lab, API-verified).
# The seeded user IS a student; they open their console (click-retry) and Stop/Start their own lab.
# State verified via the CloudLabs API; lab deleted once at teardown.
# Run: mvnw test -Dcucumber.tags="@studentaction"

Feature: Student self-service lab actions

  @PG-23 @studentaction @provisioning
  Scenario: A student can start and stop their own lab from the console
    Given a fresh enrolled user with a running lab
    When the student starts their lab from the console
    And the student stops their lab from the console
    Then the shared lab is Stopped
