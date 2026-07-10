# My Labs (theme_nuvetheme /theme/nuvetheme/mylabs.php) — batch 1, no provisioning.
# Page renders lab cards + search; unauthenticated -> login redirect; search sanitizes script input
# (XSS). Launch/stop/expired/empty-state need real labs -> batch 2. Run: -Dcucumber.tags="@mylabs"

Feature: My Labs page

  @ML-page @mylabs
  Scenario: The My Labs page lists lab cards with a search
    When admin opens the My Labs page
    Then the My Labs page shows lab cards and a search field

  @ML-unauth @mylabs
  Scenario: My Labs requires authentication
    Then an unauthenticated user is redirected to login from My Labs

  @ML-search @mylabs
  Scenario: The My Labs search treats script input as text (XSS-safe)
    When admin opens the My Labs page
    Then entering a script payload in the My Labs search does not execute it
