# PG-21: a bulk lab action applies to MULTIPLE selected labs. Seeds TWO fresh users + labs,
# selects both rows in Manage Labs, stops the selection, verifies BOTH reach Stopped via the
# CloudLabs API. Both labs + users deleted at teardown. Run: mvnw test -Dcucumber.tags="@bulk"

Feature: Bulk lab actions

  @PG-21 @bulk @provisioning
  Scenario: A bulk action applies to multiple selected labs
    Given two fresh enrolled users with running labs
    When I stop all selected labs from Manage Labs
    Then both labs are Stopped
