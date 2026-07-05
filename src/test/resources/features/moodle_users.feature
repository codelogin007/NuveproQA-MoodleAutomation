# Moodle user administration (standard Moodle core — no cloudlabs). Fast, deterministic.
# @U<n> maps to MoodleRegression_Testing.xlsx > Moodle_users S.No <n>.
# Users are seeded/cleaned via Web Services (ApiClient); create-flow tests use the admin UI.
# Run: mvnw test -Dcucumber.tags="@users"

Feature: Moodle user administration

  @U1 @users
  Scenario: Admin can create a new user
    When admin creates a new user via the form
    Then the user is created successfully

  @U2 @users
  Scenario: Add-user form requires mandatory fields
    When admin submits the add-user form empty
    Then the form shows validation errors

  @U3 @users
  Scenario: Creating a user with an existing username is rejected
    Given a registered user
    When admin tries to create a user with the same username
    Then the form shows a duplicate-user error

  @U4 @users
  Scenario: A weak password is rejected by the password policy
    When admin creates a user with a weak password
    Then the form shows a password-policy error

  @U5 @users
  Scenario: A registered user can log in with valid credentials
    Given a registered user
    Then the user can log in with valid credentials

  @U6 @users
  Scenario: Login with invalid credentials is rejected
    Then login with invalid credentials is rejected

  @U21 @users
  Scenario: A user can log out
    Given a registered user
    Then the user can log in and then log out

  @U25 @users
  Scenario: A created user appears in the admin user list
    Given a registered user
    Then the user appears in the admin user list

  @U26 @U27 @users
  Scenario: A suspended user cannot log in and a re-enabled user can
    Given a registered user
    When admin suspends the user
    Then the user cannot log in
    When admin unsuspends the user
    Then the user can log in with valid credentials

  @U30 @users
  Scenario: Admin can change a user's email
    Given a registered user
    When admin changes the user email
    Then the user email is updated

  # ---- derived gaps (CGAP-U-*) ----

  @CGAP-U-1 @users
  Scenario: Invalid email format is rejected on user creation
    When admin creates a user with an invalid email format
    Then the form shows an email-format error

  @CGAP-U-2 @users
  Scenario: Duplicate email is rejected on user creation
    Given a registered user
    When admin tries to create a user with the same email
    Then the form shows a duplicate-email error

  @CGAP-U-6 @users
  Scenario: A deleted user cannot log in and is not listed
    Given a registered user
    When admin deletes the user
    Then the deleted user cannot log in
    And the deleted user is not listed

  @CGAP-U-7 @users
  Scenario: Force password change on first login is enforced
    When admin creates a user with force password change
    Then the user is forced to change password on login

  @CGAP-U-8 @users
  Scenario: A user can change their own password
    Given a registered user
    Then the user can change their own password and log in with it

  @CGAP-U-9 @users
  Scenario: Admin can add and remove a user from a cohort
    Given a registered user
    Then admin can add and remove the user from a cohort

  @CGAP-U-11 @users
  Scenario: The user-list filter narrows to the matching user
    Given a registered user
    Then filtering the user list narrows to that user

  @CGAP-U-12 @users
  Scenario: A suspended user is flagged in the user list
    Given a registered user
    When admin suspends the user
    Then the user is shown as suspended in the list
