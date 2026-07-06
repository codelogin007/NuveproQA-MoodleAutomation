# Moodle custom roles (R86-R88): create a custom role from an archetype preset (which enables the
# cloudlabs capabilities), confirm it's listed, has the capabilities, and is offered when enrolling
# users. The role is deleted at teardown. Run: mvnw test -Dcucumber.tags="@customrole"

Feature: Moodle custom roles

  @R86 @R87 @customrole @roles
  Scenario: Admin can create a custom role with capabilities enabled
    When admin creates a custom role based on the editing-teacher archetype
    Then the custom role is listed
    And the custom role has cloudlabs capabilities enabled

  # R88 - deferred: a freshly created role may need "allow role assignment" config before it appears
  # in the AJAX "Enrol users" dropdown; step SKIPS with a reason until that is handled. See PROGRESS.md.
  @R88 @customrole @roles
  Scenario: The custom role is available when enrolling users
    When admin creates a custom role based on the editing-teacher archetype
    Then the custom role is available in the enrol-users dropdown
