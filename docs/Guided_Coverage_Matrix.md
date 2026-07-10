# Guided (Practice Project) Lab — Test Coverage Matrix

_Derived from artifacts (plugin control inventory + state machine + the 27 manual cases in
`MoodleRegression_Testing.xlsx > Moodle_Guided_Labs`), not intuition. Same method as
`Playground_Coverage_Matrix.md`._

## Scope (STANDING)
- Roles: **admin + student** only.
- Lab template: **Account** first; platform/VM/Composition variants deferred.
- Mode: **`practiceprojecttype = withprogresstracking`** first; `withoutprogresstracking` later.
- Priority: automate **High** first (all 27 Guided cases are High).
- Provisioning: use the proven **fresh-user lab + CloudLabs API polling + extend/expire** method
  (a guided lab is a cloudlabs lab — start/stop/state verify identically). See CLAUDE.md
  "Lab operations — MANDATORY".

## Control inventory (live elements)
**Landing** (`guided-landing.mustache`): Start Guided Project `#np-ap-pl-guide-start`; Continue
`.np-ap-gp-continue`; New Attempt `#np-ap-gl-new-attempt-btn-top` + gap countdown `#new-attempt-timer`;
Restart `.np-ap-gp-restart`; Complete `.np-ap-guided-project-end`; Completed badge
`.guided-project-ended-badge`; details `#np-ap-cl-challenge-details` (fromdate/todate/timeLimit/
allowedAttempts/attemptsTaken); attempts cards `#labAttemptsContainer`; admin manage buttons
(gated `managePermission`): **User Labs** → `managelabs.php`, **User Guided Project** →
`userguidedprojects.php`, **Manage Overrides**. Hidden: `#np-ap-gl-sectionId`, `#np-ap-gl-id`.

**Control panel** (`guided-controlpanel.mustache`): Submit `#np-ap-gl-submit-project-btn`; Stop Lab
`.np-ap-gl-guide-end` (redirects to landing); timer `#np-ap-cl-timer-cp`; progress ring
`.progressCircle`; Lab Usage `#openLabUsageBtn`, Instructions `#openMissionBtn`, Credentials
`#openAuthBtn`; Checkpoints modal `#guidedProgressCheckPoints` → `.checkPoints`. **DORMANT:** the
inline progress panel + Get Progress `#performProgressCheck` are COMMENTED OUT.

**Config** (`mod_form.php`, `hideIf labtype ne guided`): `guidecontentsource` (manual/git) +
`guidecontentgiturl`; `practiceprojecttype` (with/withoutprogresstracking); `guidedprogresscheckurl`;
`submissionfromdate`/`submissionduedate`; `timelimit`; `assessmentretake` (attempts 1-100);
`each_attempt_time_gap` (cooldown, 0-60).

**Admin page** (`userguidedprojects.php` + `user-guided-projects.mustache`): participants table
`#np-ap-manage-user-guided-projects` (firstname/email/allowedattempts/attemptstaken/completionstatus/
startdate/completeddate/groupname); Checkpoint Details `#np-ap-manage-guided-projects-checkpoints`;
User Attempts `#np-guided-user-attempt` → `practiceuserattempts.php`; Reports: download
`#np-ap-assessment-report-btn` (`exportPracticeProjectReport` CSV) + email
`#np-ap-assessment-report-email-btn` (`sendEmailPracticeProjectReport`); Override → `addguidedprojectoverride.php`.

**State machine**: tables `user_guided_projects` (iscompleted), `guided_project_attempts`
(active/completed, startdate, completeddate, timelimit), `guided_progress` (checkpoints). Services:
`startGuidedProjectAttempt`, `continueGuidedProject`, `reStartGuidedProject`, `completeGuidedProject`,
`submitGuidedProjectAttempt`, `startGuidedProjectOverrideAttempt`, `checkGuidedProjectTimer`.
**Cron** `update_auto_get_progress` (every min) → auto-end due-date-expired projects + time-limit-
expired attempts (does get-progress then auto-submit).

**Reports**: no `report/` dir — delivered from the admin page (CSV download + email).

## Manual cases (27) — automation status
| Case | Summary | Status |
|---|---|---|
| G1 | Guided project option in Add activity | **AUTO now** |
| G2 | Guided script URL (`guidedprogresscheckurl`) field | **AUTO now** |
| G3 | Start/end date fields | **AUTO now** |
| G4 | Enrolled user can start guided project | **AUTO now** |
| G5 | User sees start/end date | **AUTO now** |
| G6 | Admin sees User Labs + User Guided Project buttons | **AUTO now** |
| G7,G8 | Admin per-lab actions (via User Labs → Manage Labs) | **AUTO now** (G8 dup of G7) |
| G9 | User lab listing lists all users' labs | **AUTO now** |
| G10 | User Guided Project table lists all participants' progress | **AUTO now** |
| G11 | Admin sees checkpoints + progress per user | needs progress data |
| G12 | Admin sees start/completion date per user | **AUTO now** |
| G13 | Checkpoint status correct per user | needs progress data |
| G14 | Land on lab control panel | **AUTO now** |
| G15 | Start Lab creates a lab | **AUTO now** (API-verified) |
| G16 | Stop Lab | **AUTO now** |
| G17 | Stop Lab stops + redirects to guided page | **AUTO now** |
| G18 | Get Progress button present | **BLOCKED** (dormant/commented out) |
| G19 | Get Progress shows checkpoints + status | **BLOCKED** (dormant) |
| G20 | Get Progress auto-triggered via cron | defer (cron) |
| G21 | Manual Get Progress updates status | **BLOCKED** (dormant) |
| G22 | Auto-complete when timeline over | defer (time/cron) |
| G23 | Restart after marking complete early | defer (needs completion state) |
| G24 | Restart after allotted duration | defer (time-based) |
| G25 | Guided report in cloudlabs report section | **AUTO now** |
| G26 | Report shows checkpoints/dates/details | needs progress data |
| G27 | Consolidated report download | **AUTO now** |

## Gap cases (CGAP-PGG-*, derived from the inventory)
| Id | Case | Batch |
|---|---|---|
| CGAP-PGG-1 | Continue an active attempt (`.np-ap-gp-continue`) → control panel | 1 |
| CGAP-PGG-2 | Start a New Attempt (`#np-ap-gl-new-attempt-btn-top`) | 1 |
| CGAP-PGG-3 | New Attempt disabled during cooldown (`each_attempt_time_gap` → `#new-attempt-timer`) | 2 |
| CGAP-PGG-4 | New Attempt blocked when `allowedattempts` reached | 2 |
| CGAP-PGG-5 | Submit an attempt (`#np-ap-gl-submit-project-btn`) | 1 |
| CGAP-PGG-6 | Manually Complete the guided project + Completed badge | 1 |
| CGAP-PGG-7 | Restart when NOT allowed (within gap / after due date) → error | 2 (extend/expire) |
| CGAP-PGG-8 | Start attempt → lab reaches RUNNING (completion, API) | 1 |
| CGAP-PGG-9 | Stop Lab → STOPPED + redirect to landing (completion, API) | 1 |
| CGAP-PGG-10 | Admin Checkpoint Details modal renders | needs data |
| CGAP-PGG-11 | Admin User Attempts drill-down (`practiceuserattempts.php`) | 2 |
| CGAP-PGG-12 | Admin grants an override attempt (`addguidedprojectoverride.php`) | 2 |
| CGAP-PGG-13 | Student does NOT see admin manage buttons/table/reports (role) | 1 |
| CGAP-PGG-14 | Student sees only their own project | 1 |
| CGAP-PGG-15 | Config: due-date-before-start validation | 2 |
| CGAP-PGG-16 | Config: `practiceprojecttype` with vs without progress tracking | 2 |
| CGAP-PGG-17 | Config: `guidecontentsource` manual vs git URL | 2 |
| CGAP-PGG-18 | Email the guided report (`sendEmailPracticeProjectReport`) | 2 |
| CGAP-PGG-19 | Bootstrap-table client export (xml/csv/txt/json) | 2 |
| CGAP-PGG-20 | Landing description / read-more / intro video | 3 (low) |

## First batch (automate now, no clarification)
G1, G2, G3, G4, G5, G6, G7/8, G9, G10, G12, G14, G15, G16, G17, G25, G27
+ CGAP-PGG-1, 2, 5, 6, 8, 9, 13, 14.
Blocked (verify Get-Progress): G18, G19, G21. Deferred (time/cron/data): G11, G13, G20, G22, G23, G24, G26.
