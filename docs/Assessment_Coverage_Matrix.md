# Assessment (Challenge) Lab — Test Coverage Matrix

_Derived from the plugin control inventory + the 327 Kiwi `Moodle_Assessment_Labs` cases (107 High).
Same method as Playground/Guided/Users/Tags/Roles/Groups. Generated 2026-07-07._

## Scope (STANDING)
Roles admin + student; Account template first; High priority first; fresh-user + CloudLabs-API
method for lab actions (CLAUDE.md "Lab operations — MANDATORY"). `ASSESSMENT_CMID=781` (course 50 —
NOT COURSE_ID=72; derive the course from the `course-<id>` body class, as with Guided).

## Control inventory (anchors)
**Landing** (`challenge-landing.mustache` / `assessment-landing.mustache`): Start
`#np-ap-cl-challenge-start`, Continue `#np-ap-cl-attempt-continue`, New `#np-ap-cl-attempt-new`,
Complete `#np-ap-cl-challenge-end`/`#completeChallengeBtn`, Retake `#btn-cl-retake-assessment`;
attempts table `#np-ap-cl-challenge-attempt-table` (status/tests/grade/marks/result/dates);
complete-confirm modal `#np-ap-cl-cmpleteAssessment-dlg` gated by `#confirmFinalSubmission` checkbox →
`#np-ap-cl-final-submit-btn`; highest grade `#np-ap-cl-highestGrade`, final result
`#np-ap-cl-finalResult`; hidden `#np-ap-cl-sectionId/-id/-timeLeft/-challenge-started`.

**Control panel** (`challenge-controlpanel.mustache`): Submit `#np-ap-cl-submit-attempt-btn`; timer
`#np-ap-cl-timer`; instructions `#openMissionBtn` (+PDF `#downloadPdfBtnChallenge`); credentials
`#openAuthBtn` + `#np-ap-mod-set-password-btn`; answer dialog `#np-ap-cl-answer-dlg` (+
`#np-ap-cl-challenge-answer`, for evaluationtype=ANSWER); hidden `#np-ap-cl-labId/attemptId/evalType/
attempt-status/retakenumber/user_ps_attemptid`.

**Config (mod_form, labtype=challenge)**: `evaluationtype` AUTO/ANSWER/MANUAL; `testscripturl`,
`testscriptparams` (JSON, validated); `testcaseweightage` SAME/CUSTOM + `eachtestcasemarks`;
`challengeanswer`/`challengemarks` (ANSWER); `assessment_randomization` Yes/No;
`challengecontentsource` manual/git + `challengecontentgiturl`/`challenge_content_editor`;
`problem_statement`; `maxattempts` (submissions), `assessmentretake` (attempts),
`allowattemptafterpassing`; `cooldownperiod` (+`attemptsaftercooldown`, hidden when None);
`timelimit`, `submissionfromdate/duedate` (due-before-start validated), `overduehandling`
autosubmit/removelabaccess; `evaluation_timeout_duration/_action` Stop/Park/Delete;
`sendassessmentresult`; `gradepass` (required — validation); `attemptsummaryfields` checkboxes
(Status/Grade/Result/Tests/Marks/FinalResult/EvaluationResult/adminConfiguration = the hide-grades
mechanism).

**Admin**: `managechallenges.php` → `#np-ap-manage-challenge-table` (per-user attempts/submissions/
grade/result), manage/delete attempts, overrides (`#np-ap-cl-add-user-override`), report download
`#np-ap-assessment-report-btn` + email, user attempts `#np-ap-user-attempt` → `manageuserattempt.php`
(`#np-ap-manage-attempt-table`, update grades, delete submission, admin submit, eval result, comments,
export, `#np-ap-update-to-moodle-btn`); problem statements `assessment-problemstatements.php`
(`#np-ap-ps-problemstatement` table, Add/Edit/View/Delete/Activate/Deactivate buttons).

**Services** (labservices.php): startChallenge:1074, submitAttempt:636 (→Evaluating),
endChallengeClicked:1240 (iscompleted/finalresult), getChallengeAttempts:927, retakeAssessment:1894
(cooldown enforcement). Randomization: `assignedRandomProblemStatementForActivity`
(cloudlabsglobals:2921 — random UNUSED PS; repeats last when pool exhausted; only Active PS).
**States**: InProgress → Evaluating → Evaluated/NotEvaluated; cron auto-submit on timelimit/due date
(`update_auto_submit_challenge`), evaluation-timeout action.

## Manual coverage read (Kiwi clusters, 327 cases)
Well covered: retake/cooldown (45), randomization CRUD+reports (91), per-testcase marks (36 — needs
REAL lab work; manual/integration), reopening/overrides (30), hide grades (20), disable-complete-
button states (11), soft-deletion cron (12), git-URL content (17).
Automatable-now (no provisioning): config-toggle mechanics, role enforcement, PS admin CRUD, landing
presence. With provisioning: start→CP→submit→Evaluating, complete-with-checkbox, admin tables.
Defer: per-testcase marks correctness (requires performing lab tasks), cron clusters, proctoring.

## Derived gaps (CGAP-A — proposed; team's own "Assessment Gap Check" rows already exist)
| Id | Case |
|---|---|
| CGAP-A-1 | evaluationtype toggles: AUTO shows script URL/params; ANSWER shows answer+marks; MANUAL hides both |
| CGAP-A-2 | assessment_randomization=Yes hides the manual problem statement/content editor |
| CGAP-A-3 | challengecontentsource git↔manual toggles the git-URL field vs the editor |
| CGAP-A-4 | cooldownperiod=None hides attemptsaftercooldown; setting a period shows it |
| CGAP-A-5 | Challenge due date before start date is rejected (mod_form validation, challenge path) |
| CGAP-A-6 | Missing gradepass on a challenge is rejected ("Provide Passing Grade") |
| CGAP-A-7 | Invalid testscriptparams JSON is rejected (mod_form validation) |
| CGAP-A-8 | Student does NOT see admin manage controls; direct URLs (managechallenges/problemstatements/manageuserattempt) blocked |
| CGAP-A-9 | Complete-assessment final submit is GATED by the consent checkbox (disabled until ticked) |
| CGAP-A-10 | Randomization no-repeat: a user gets UNUSED problem statements until the pool is exhausted, then the last repeats |
| CGAP-A-11 | gradepass boundary: grade exactly equal to gradepass = Pass |
| CGAP-A-12 | retakenumber increments per retake and shows in the admin attempts table |

## Batch plan
1. **Batch 1 (no provisioning):** CGAP-A-1..7 config toggles/validations + CGAP-A-8 role enforcement
   + landing presence (Start/details/attempts table on cmid 781).
2. **Batch 2 (provisioned):** start → CP → submit (→Evaluating) → complete (checkbox gate, CGAP-A-9)
   → admin manage-challenge table row; lab verified via CloudLabs API.
3. **Batch 3:** PS admin CRUD (A249/250/255/257/259) + overrides; then evaluation-dependent cases as
   the evaluation-script config allows.
Defer: per-testcase marks (A65-96), cron (A99/185-188/206-214), proctoring, randomization reports.
