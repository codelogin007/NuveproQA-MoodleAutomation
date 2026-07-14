# Sequential Activity Completion (Moodle core: Activity completion + Restrict access). @SAC-<n> maps
# 1:1 to the 'Sequential_Activity_Completion' sheet in MoodleRegression_Testing_Kiwi.xlsx (row-1 = SAC-1).
#
# This is the MARK-AS-DONE subset (no grading needed): a self-contained fixture is built once — a fresh
# course with Activity 1 (URL, "students manually mark as done") and Activity 2 (URL, restricted until
# Activity 1 is complete), plus a freshly-seeded enrolled student. Scenarios run in feature order:
# restricted/incomplete checks first, then Activity 1 is marked done, then the enabled/completed and
# persistence checks. @AfterAll deletes the course + student (site left clean). Needs MOODLE_WS_TOKEN.
#
# The grade-based cases (SAC-2..8, SAC-14) need a submit-and-grade workflow and are the next batch.
# SAC-15 (3+ activity chain) is a separate multi-activity fixture, also deferred to that batch.
#
# Run: mvnw test -Pwatch -Dcucumber.tags="@sac"

Feature: Sequential activity completion (mark-as-done gating)

  @SAC-1 @sac
  Scenario: Activity 2 stays locked while Activity 1 is not completed
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    Then Activity 2 is not accessible to the student

  @SAC-10 @sac
  Scenario: Activity 2 stays locked when Activity 1 is not marked as done
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    Then Activity 2 is not accessible to the student

  @SAC-12 @sac
  Scenario: Activity 1 is shown as incomplete before completion
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    Then Activity 1 is shown as not completed

  @SAC-16 @sac @augmented
  Scenario: The restriction message names the required prior activity
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    Then the restriction message names Activity 1

  @SAC-11 @sac
  Scenario: Activity 1 is shown as completed once marked as done
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    When the student marks Activity 1 as done
    Then Activity 1 is shown as completed

  @SAC-9 @sac
  Scenario: Activity 2 unlocks once Activity 1 is marked as done
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    Then Activity 2 becomes accessible to the student

  @SAC-13 @sac
  Scenario: Activity 2 stays unlocked after the student logs out and back in
    Given a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete
    When the student logs out and logs in again
    Then Activity 2 remains accessible to the student

  # ================= grade-based cases (@sacgrade) =================
  # Activity 1 = Assignment with grade-based completion; the teacher grades the student via the gradebook
  # (Edit mode + Single view override). Each scenario builds + tears down its own fresh fixture.

  @SAC-2 @sac @sacgrade
  Scenario: Activity 2 unlocks once Activity 1 is completed with any grade
    Given a course where Activity 1 requires any grade and Activity 2 is restricted until Activity 1 is complete
    When the teacher gives the student a grade of 60 on Activity 1
    Then the graded Activity 2 becomes accessible to the student

  @SAC-3 @sac @sacgrade
  Scenario: Activity 2 stays locked when Activity 1 has no grade
    Given a course where Activity 1 requires any grade and Activity 2 is restricted until Activity 1 is complete
    Then the graded Activity 2 is not accessible to the student

  @SAC-4 @sac @sacgrade
  Scenario: Activity 2 unlocks once Activity 1 receives a passing grade
    Given a course where Activity 1 requires a passing grade of 50 and Activity 2 is restricted until Activity 1 is complete
    When the teacher gives the student a grade of 60 on Activity 1
    Then the graded Activity 2 becomes accessible to the student

  @SAC-5 @sac @sacgrade
  Scenario: Activity 2 stays locked when Activity 1 receives a failing grade
    Given a course where Activity 1 requires a passing grade of 50 and Activity 2 is restricted until Activity 1 is complete
    When the teacher gives the student a grade of 30 on Activity 1
    Then the graded Activity 2 is not accessible to the student

  @SAC-14 @sac @sacgrade
  Scenario: Activity 2 unlocks when Activity 1 grade is updated from failing to passing
    Given a course where Activity 1 requires a passing grade of 50 and Activity 2 is restricted until Activity 1 is complete
    When the teacher gives the student a grade of 30 on Activity 1
    Then the graded Activity 2 is not accessible to the student
    When the teacher gives the student a grade of 70 on Activity 1
    Then the graded Activity 2 becomes accessible to the student
