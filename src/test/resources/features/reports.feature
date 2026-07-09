# CloudLabs Reports (report_cloudlabsreport) — batch 1, the course-level report landing.
# courses.php lists courses (Course Name / Total Assessments / Participants / Start / End Date);
# search filters; course name drills into courseparticipants.php. Role gate is a DEVIATION:
# courses.php only has require_login (no capability), so a student may see the report.
# Data-verification cases (counts/grades/dates match actual) need fixtures - deferred.
# Run: mvnw test -Dcucumber.tags="@reports"

Feature: CloudLabs course reports

  @RPT-courses @reports
  Scenario: The course report lists courses with count and date columns
    When admin opens the cloudlabs course report
    Then the course report lists courses with assessment participant and date columns

  @RPT-search @reports
  Scenario: The course report search filters and clears
    When admin opens the cloudlabs course report
    Then searching the course report filters the rows and clearing restores them

  @RPT-drilldown @reports
  Scenario: Clicking a course drills into its participant report
    When admin opens the cloudlabs course report
    Then clicking a course opens its participant report

  # DEVIATION (fails by design until fixed): courses.php has only require_login (no capability /
  # context check), so a logged-in student can open the course report and see all courses' data.
  @RPT-role @rptdeviation @reports
  Scenario: DEVIATION - the course report should be blocked for students
    When admin opens the cloudlabs course report
    Then a student cannot open the cloudlabs course report
