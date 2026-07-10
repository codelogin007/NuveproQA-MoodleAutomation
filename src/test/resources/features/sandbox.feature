# Sandbox (cloudlabs sandbox/playground landing) — batch 1, no provisioning. The sandbox landing
# (view.php?id=PLAYGROUND_CMID) shows lab details (type/status/created/sandbox id), the duration
# (alloted/consumed/remaining) and the description + Read More. Run: -Dcucumber.tags="@sandbox"

Feature: Sandbox landing details

  @SB-details @sandbox
  Scenario: The sandbox landing shows the lab details
    When admin opens the sandbox activity
    Then the sandbox landing shows lab type status created date and sandbox id

  @SB-duration @sandbox
  Scenario: The sandbox landing shows the duration
    When admin opens the sandbox activity
    Then the sandbox landing shows the alloted consumed and remaining duration

  @SB-description @sandbox
  Scenario: The sandbox landing shows a description with a Read More control
    When admin opens the sandbox activity
    Then the sandbox landing shows a description section and a Read More control
