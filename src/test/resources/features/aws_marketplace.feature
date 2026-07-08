# AWS Marketplace Integration (local_nuveawsmarketplace) — batch 1, admin settings page
# (/admin/settings.php?section=local_nuveawsmarketplace_settings). Field/region presence, email
# validation (invalid -> error, not persisted), role gate. The secret-key-not-masked FINDING is a
# fail-by-design deviation. No successful save of changed values. Run: -Dcucumber.tags="@awsmarket"

Feature: AWS Marketplace settings

  @AWS-config @awsmarket
  Scenario: The AWS Marketplace configuration page shows its fields
    When admin opens the aws marketplace settings
    Then the aws settings show the region access-key secret-key and support-email fields

  @AWS-region @awsmarket
  Scenario: The region dropdown offers region options
    When admin opens the aws marketplace settings
    Then the region dropdown offers at least one region

  @AWS-validation @awsmarket
  Scenario: An invalid support email is rejected
    When admin opens the aws marketplace settings
    Then an invalid support email is rejected on save

  @AWS-role @awsmarket
  Scenario: The AWS Marketplace settings are not accessible to a student
    When admin opens the aws marketplace settings
    Then a student cannot open the aws marketplace settings

  # FINDING (fails by design until fixed): the AWS Secret Key is a plain-text input with no masking
  # (Moodle offers admin_setting_configpasswordunmask). Sensitive credential shown in clear.
  @AWS-secret @awsdeviation @awsmarket
  Scenario: DEVIATION - the AWS Secret Key should be masked
    When admin opens the aws marketplace settings
    Then the aws secret key field is masked
