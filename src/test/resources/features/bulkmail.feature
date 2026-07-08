# NuveBulkMail (local_bulkmail) — batch 1, settings form (no mail sending). Field/option presence,
# custom-text validation, role gate (cap local/bulkmail:managesettings), and one SAVE-and-RESTORE
# (the settings are the plugin's global config — captured and restored so nothing is left changed).
# Mail composition/sending/campaign-cron are deferred (need SMTP capture). Run: -Dcucumber.tags="@bulkmail"

Feature: NuveBulkMail settings

  @BM-fields @bulkmail
  Scenario: The settings form exposes the password pattern and behavior options
    When admin opens the bulk mail settings
    Then the settings form shows the password pattern and behavior fields

  @BM-customtext @bulkmail
  Scenario: The custom text field accepts alphanumeric and special characters
    When admin opens the bulk mail settings
    Then the custom text field accepts alphanumeric and special characters

  @BM-role @bulkmail
  Scenario: The bulk mail settings are not accessible to a student
    When admin opens the bulk mail settings
    Then a student cannot open the bulk mail settings

  @BM-save @bulkmail
  Scenario: Admin can save a password pattern and behavior (then restored)
    When admin opens the bulk mail settings
    Then admin can save a password pattern and behavior and it persists
