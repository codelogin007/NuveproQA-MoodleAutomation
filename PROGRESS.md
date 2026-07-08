# Progress Tracker — Nuvepro Moodle UI Automation

Living status of what's **automated (green)**, **open (to do)**, and **deferred (with reason)**, by
module. Update as batches complete. Legend: ✅ done · 🔲 open · ⏸️ deferred/blocked.
Last updated: 2026-07-08.

Repo: https://github.com/codelogin007/NuveproQA-MoodleAutomation (branch `main`).
Run a module: `mvnw test "-Dcucumber.tags=@<tag>"`.

---

## Coverage at a glance
Denominators are the **current master `MoodleRegression_Testing_Kiwi.xlsx`** (36 case-sheets, ~1,976
original doc cases + our 116 `CGAP-*`/gap rows = 2,092 rows). "Automated scenarios" = green Cucumber
scenarios (a Scenario Outline counts its data rows). "Doc cases covered" = `covered / sheet total`
(sheet total excludes our gap rows). Covered is approximate — one scenario can exercise several doc
cases and vice-versa; many uncovered cases are combinatorial variations of a covered mechanic, or are
deferred. **Overall automated coverage of the current master is ~9%** — the Kiwi doc is far larger than
the original and adds many feature areas not yet started (listed below).

### Modules with automation
| Module (Kiwi sheet) | Automated scenarios | Doc cases covered | Derived gaps | Deferred (reason) |
|---|---|---|---|---|
| Playground (Moodle_Playground_Labs 16 + reorg into Sandbox/Lab_Control_Panel/My_Labs) | ~24 green | ~30 (spans those sheets) | CGAP-PG(37)+PlaygroundGap | template variants, config-gated, value-checks |
| Guided (Moodle_Guided_Labs 42) | 13 | ~12 / 42 | CGAP-PGG (19) | Complete btn dormant; overrides/checkpoint; cron |
| Moodle_users (35) | 25 | ~24 / 35 | CGAP-U (12) | report-builder list, bulk import, config/mail cases |
| Tags (Moodle_Tags 43) | 29 | ~29 / 43 | CGAP-T (11) | Orgo/report tags, config-gated |
| Roles (Moodle_Roles 90) | 10 (matrix ≈ 56 cases) | ~56 / 90 | CGAP-R (12) | enforcement gaps, broad functional |
| Groups (Groups_Testcases 48) | 11 | ~10 / 48 | CGAP-GR (11) | cohort-sync enrolment, reports, time-based |
| Assessment (Moodle_Assessment_Labs 327) | 16 (15 green + 1 defect red) | ~10 / 327 | CGAP-A (11) | PS-CRUD, per-testcase marks, cron, proctoring, hide-grades |
| Pre-Delivery (Pre_Delivery_Lab_Checklist 72) | 4 green | ~4 / 72 | CGAP-PDL (4) | successful save+audit log, screenshot upload, dashboard filters |
| NuveBulkMail (local_bulkmail, sheet 110) | 4 green | ~5 / 110 | CGAP-BM (3) | compose/send, templates, reminders, campaign cron (need SMTP capture) |
| AWS_Marketplace (local_nuveawsmarketplace, 36) | 4 green + 1 defect red | ~6 / 36 | CGAP-AWS (3) | subscriptions flow, save valid config (mutates), multi-region |
| Content_Integration_Git_URL (24) | 1 green | ~3 / 24 | CGAP-CIG (2) | git-fetch valid/invalid, all-three-lab-types matrix |
| Difficulty_Level_Configuration (14) | 2 green | ~3 / 14 | CGAP-DLC (3) | empty-save handling, config→activity flow |
| **Subtotal (worked sheets)** | **~123 green scenarios** | **~193 / ~857 doc cases** | 128 gap rows | — |

### Not started — 0 covered (the rest of the current master, ~1,375 cases across 27 sheets)
Reports 202 · Practice_Project 141 · All Course Page 106 · Lab_Templates 82 ·
Moodle settings 79 · Randomization_Activity 68 · Activities 67 ·
Moodle_Logs 50 · CloudLabs_Lab_Creation_Status 49 · My_Labs 41 ·
Lab_Control_Panel 34 · Course_Sync_Multi_Tenant 33 · User_Sync_Orgo 31 · Raven360_AWS_Content 29 ·
Lab_Creation_Region_Based 23 · Moodle_cron 17 ·
Sequential_Activity_Completion 16 · Moodle-Cloudlabs Integration 13 ·
Sandbox 12 · Course_Category_Lab_Provision 12 · Stop_Lab_Enhancement 7 · LoadBalacer_Moodle 7.

### Grand total (current master)
**~172 of ~1,976 original doc cases automated (~9%)** · ~108 green scenarios · 116 `CGAP-*`/gap rows we
authored & merged into the Kiwi doc. The bulk of the gap is the newly-added feature sheets + expanded
Assessment/Reports/Practice_Project/NuveBulkMail — most are automatable (config/UI/admin surface), some
are real-lab/cron/proctoring (deferred). Note: some new sheets (Sandbox, Lab_Control_Panel, My_Labs,
Practice_Project) overlap Playground/Guided mechanics already automated, so effective coverage of NEW
behavior is a bit higher than the raw 9%.

Findings raised: Roles permissions defect (fixed+verified) · Assessment CGAP-A-8 (`@assessdeviation`,
open — student can open the problem-statements admin page) · guided cmid 780 env fixes (due date/timelimit).

---

## Playground (cloudlabs sandbox lab)  — tag `@provisioning` groups
- ✅ **State actions** (`@stateactions`): PG-12, 15, 17, 18, 19, 33, 34, 35, 38, 49
- ✅ **Console read-only** (`@console`): PG-3, 5, 6, 8 · PG-45 template-gated skip
- ✅ **Student self-service** (`@studentaction`): PG-23
- ✅ **Bulk** (`@bulk`): PG-21
- 🔲 Value-checks: PG-25, 26, 56, 46 (mostly template-gated on sandbox_1 — need enabling template)
- 🔲 Traceability refresh (results column is stale)
- ⏸️ Template variants (VM / Composition / AWS-CSP-GCP) — need cmids (see TODO.md)
- ⏸️ Config-gated: PG-31, 50, 54, 55 (fault-injection / expired / unlimited templates)
- ⏸️ CGAP-PG-* gap cases (Playground_Coverage_Matrix.md §E)

## Guided (practice project)  — tags `@guided` + `@guidedgaps`
- ✅ Landing + admin + config presence: PGG-4, 5, 6, 9, 10, 12, 2, 3
- ✅ **Derived gaps, no-provisioning** (`@guidedgaps`): CGAP-PGG-13/14 (student sees no admin buttons;
  admin participants page blocked for student — capability ENFORCEMENT), CGAP-PGG-15 (due-before-start
  rejected), CGAP-PGG-16/17 (progress-tracking + content-source config toggles)
- ✅ **Lab lifecycle** (`@guidedlab`, one provisioned lab): G14 control panel, G15/CGAP-PGG-8
  start→RUNNING (API), G16/G17/CGAP-PGG-9 stop→STOPPED + back to landing, CGAP-PGG-1 Continue.
  Gotchas handled: start/continue confirm modals; cp.php CP detected by CONTROLS (no
  .guided-control-panel container); lab NOT auto-provisioned — click "Start hands-on lab"; lab id
  read from admin Manage Labs (CP inputs are server-rendered/stale).
- ✅ **Attempt lifecycle** (`@guidedattempts`): CGAP-PGG-5 submit while RUNNING (firm), CGAP-PGG-3
  cooldown blocked-signal observed (1-min gap), CGAP-PGG-2 New Attempt after the gap → CP. All green.
- ⏸️ CGAP-PGG-6 Complete button: DORMANT (commented out in guided-landing.mustache, like Get-Progress)
- 🔲 Attempt lifecycle remainder: CGAP-PGG-4 (attempts exhausted), 7 (restart blocked), 10/11/12
  (checkpoint data / drill-down / override), 19 (table export)
- ⚠️ **Env fixes applied 2026-07-07** (restore notes): guided activity due date was EXPIRED
  (2026-05-09 15:52 → 2027) and time limit was 2 MINUTES (→ 1h) — either made the activity unusable
  for every student; flag to the team if intentional.
- 🔲 Admin: Checkpoint Details modal, User Attempts drill-down, report email
- ⏸️ **Blocked:** G18, G19, G21 (Get-Progress button is DORMANT/commented out — needs product confirmation)
- ⏸️ Deferred (time/cron/data): G20 (cron), G22 (auto-complete), G23/G24 (restart timing), G11/G13/G26 (checkpoint data)

## Moodle_users  — tag `@users`
- ✅ Create/validate/login: U1, U2, U3, U4, U5, U6, U21, U25
- ✅ Profile: U10, U11, U12 · Session/enrol: U22, U15
- ✅ Admin ops: U24 (edit+delete), U26, U27, U30 · Cohorts: U28, U29
- ✅ Derived gaps: CGAP-U-1, 2, 6, 7, 8
- 🔲 U7 (Remember Me), U8 (login lockout — config/destructive), U9 (forgot-password — needs Mailtrap), U13 (self-enrol — config), U23 (bulk CSV import)
- ⏸️ U14 (paid enrol), U16-U19 (activity submission — broad, overlaps other sheets), U20 (time-based unenrol)
- ⏸️ CGAP-U-9 (cohort assign — JS `:id`), CGAP-U-11 (list filter), CGAP-U-12 (suspended-in-list) — report-builder AJAX list

## Tags  — tags `@tags` (creation) + `@tagmgmt` (management)
- ✅ **Creation** (activity form): T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T27, T38
- ✅ **Management** (/tag/manage.php): T16 rename, T18 delete, T28 multi-delete, T24/T25/T29 combine,
  T19/T20 standard→suggested, T21/T22/T26 not-suggested, T23 renamed-suggested
- 🔲 T17 (rename updates modified date), T41 (UI messages on add/delete/combine)
- ⏸️ T30-T39 (tags on Orgo lab-details page + resource-duration report — EXTERNAL Orgo/cloudlabs reports)
- ⏸️ T40 (admin blocks tag editing — config), CGAP-T gaps (T-2 dedup, T-5 browse, T-7 reuse+count, T-10 length cap are automatable)

## Roles  — tag `@rolematrix`
- ✅ **Cloudlabs capability matrix (R24-R79)**: 5 data-driven scenarios verify all 12 cloudlabs caps
  per role vs db/access.php defaults (Manager/CourseCreator/EditingTeacher=allow, non-editing=notset,
  student=prevent). Admins bypass checks (not verified). All green.
- ✅ **Finding caught + fixed**: on 2026-07-06 flagged Editing-teacher (missing 4 lab caps) and
  Non-editing-teacher (had 2 assessment caps vs R66); site config corrected; re-run confirms fixed.
  See ../Docs/Roles_Findings.md.
- ✅ **Custom roles R86-R87** (`@customrole`): create a custom role from an archetype preset (gets
  cloudlabs caps) → listed → capabilities enabled. Role deleted at teardown.
- 🔲 **R88** deferred: custom role in the "Enrol users" dropdown — a fresh role likely needs
  "allow role assignment" config first; step SKIPS with a reason (AJAX enrol-modal + allow-assign work)
- ✅ **Functional subset** (`@rolefunc`): R2 (admin Site Administration access), R17 (assign Manager →
  system manager), R3 (assign then remove a role) — all green (system-context dual-listbox assign)
- 🔲 Enforcement gaps CGAP-R-1/2/7 (control visible/hidden by capability at runtime), R80-R85 UI integration
- ⏸️ R88 custom role in enrol dropdown (deferred — allow-assign / AJAX modal)
- ⏸️ R1/R5-R16 broad functional (course create/grade/submit/forum — overlaps other sheets)
- ✅ Gap rows CGAP-R-1..12 written into the Moodle_Roles sheet

## Groups  — tag `@groups`
- ✅ Create group (G12), duplicate-name rejected (G14 - Moodle enforces unique names; manual expected
  duplicates allowed → corrected), groups dropdown present (G1), default All Groups (G5), select single
  group (G2), search in dropdown (G4), groups column on Manage User Labs (G42)
- ⏸️ G3 select-multiple: SKIPS - the dropdown is single-select (All Groups or one), so multi-select is
  not supported by the UI (documented)
- ✅ Cohorts + members: create cohort (G11), duplicate cohort names allowed (G13 - opposite of groups),
  same user in multiple groups (G24, via the group-members dual-listbox)
- 🔲 Report export by group (G6-G10, G27-G37), groups column on other reports (G34)
- ⏸️ Cohort-sync enrolment method (G15-G19, G23, G25-G33, G45-G46) - complex enrolment-method UI + sync
- ⏸️ Cohort membership add/remove (G20-G22) - needs the cohort-assign page id (JS `:id` on the index)
- ⏸️ Duration/time-based (G26/G30-G33/G43)
- ✅ Gap rows CGAP-GR-1..11 in the Groups_Testcases sheet

## Assessment (challenge)  — tag `@assessgaps` (Kiwi sheet: 327 cases, 107 High)
- ✅ **Batch 1 (no provisioning)**: landing presence (attempt controls/details on ASSESSMENT_CMID=781,
  course 50); CGAP-A-1..4 config toggles (evaluationtype AUTO/ANSWER/MANUAL fields, randomization
  hides editor, content-source git URL, cooldown→attempts-after with polling); CGAP-A-5/6/7 edit-form
  validations (due-before-start, missing gradepass, invalid testscriptparams JSON — validation failure
  saves nothing); CGAP-A-8 student blocked from managechallenges.php + no admin controls on landing.
- 🔴 **DEFECT found (CGAP-A-8 / `@assessdeviation`, fails by design until fixed)**: a STUDENT can open
  `assessment-problemstatements.php` by direct URL — the admin table renders (the landing button is
  hidden per A249, but the page lacks a capability check; managechallenges.php IS protected).
- ✅ **Batch 2 (provisioned + REAL evaluation)** (`@assesslab` + `@assesscomplete`): start → control
  panel → lab RUNNING (API) → submit → the engine runs the configured S3-Life-Cycle scripts → attempt
  Evaluated (fresh student = grade 0 / Fail, EXPECTED). Then the activity AUTO-COMPLETES
  (isChallengeEnded=1, finalResult=Fail — also covers A127); CGAP-A-9 consent gate verified wired
  (final submit disabled → enables on ticking #confirmFinalSubmission). All green.
  Gotchas: challenge-landing.mustache is the ACTIVE template (not assessment-landing); complete button
  is `.np-ap-cl-challenge-end` (auto-hides once ended); "Start hands-on lab" launch needed; lab id from
  admin Manage Labs. ASSESSMENT_SCRIPT_URL/LAB_TEMPLATE documented in .env.example (plan 777/team 90).
- ✅ **Batch 3 (admin surface, no provisioning)** (`@assessadmin`): manage-challenges table lists
  enrolled users with grade/result columns (A11/A16); override + report + user-attempt controls present
  (A45-54/A103 surface); user-attempts drill-down opens userattempts.php (#np-ap-user-attempts-table,
  via row-radio select + #np-ap-user-attempt); admin CAN open the problem-statements page (A249-admin,
  counterpart to the @assessdeviation student defect). All green.
- 🔲 Problem-statements CRUD (A250/255/257/259): 781 has NO problem statements (randomization off) -
  needs a randomized activity with seeded PS; deferred until such an activity/cmid is available
- 🔲 Overrides deep flow (add/decrease/unlimited A45-A54), retake-after-completion, manual grade update
  (A115), console-open (A125) - need attempt data / provisioning
- ⏸️ Per-testcase marks (A65-96 — needs real lab work), cron clusters (A99/185-188/206-214), proctoring
- ✅ Matrix: ../Docs/Assessment_Coverage_Matrix.md · CGAP-A-1..12 in the Kiwi sheet (A-10 deduped)

## Pre-Delivery Lab Readiness Checklist  — tag `@predelivery` (Kiwi: Pre_Delivery_Lab_Checklist, 72)
- ✅ **Batch 1 (no provisioning)**: Lab Readiness page (`lab_readiness.php?id=cmid`) admin-only (cap
  `mod/cloudlabs:viewlabreadiness`; student blocked); checklist `.checklist-item` completion drives
  `#labProgressBar` to 100% + green `bg-success`; env-type VM/Account toggles `.vm-field`/`.account-field`;
  mandatory Support-Engineer name (empty → `alert()` → save blocked, nothing persists). All green.
- 🔲 Batch 2: readiness dashboard filters/search (activity/project/support-engineer), successful save +
  audit-log row, screenshot upload + re-upload override, backend-warning banner, force-enable flagging
- ✅ Matrix ../Docs/PreDelivery_LabReadiness_Coverage_Matrix.md · CGAP-PDL-1..4 (pending Kiwi write — file was open)

## NuveBulkMail  — tag `@bulkmail` (plugin `local_bulkmail`; Kiwi sheet NuveBulkMail, 110)
- ✅ **Batch 1 (settings form, no mail)**: `/local/bulkmail/settingspage.php` — passwordpattern (5 opts,
  incl "Username + custom text") + passwordbehavior ("Use pattern from settings" / "Send reset link") +
  from/domains fields present; customtext accepts alphanumeric+special; settings blocked for a student
  (cap `local/bulkmail:managesettings`); save a pattern+behavior → persists, then originals RESTORED. Green.
- 🔲 Batch 2+: compose flow (compose.php step1/step3), templates CRUD (templates.php/template_form.php),
  reminders (reminders.php), schedule, logs — automatable UI; ⏸️ actual mail send + campaign cron
  (process_campaigns) need SMTP capture.

## AWS Marketplace  — tag `@awsmarket` (plugin `local_nuveawsmarketplace`; Kiwi sheet 36)
- ✅ **Batch 1 (admin settings)**: `/admin/settings.php?section=local_nuveawsmarketplace_settings` shows
  region dropdown + access_key + secret_key + support_email; invalid support email rejected (validation,
  not persisted); student blocked (Moodle "sectionerror"). Green.
- 🔴 **FINDING (`@awsdeviation`, red by design)**: the **AWS Secret Key is a plain-text input, not masked**
  (should use admin_setting_configpasswordunmask) — sensitive credential shown in clear.
- 🔲 Batch 2: subscriptions flow (awsmpsubscriptions.php), multi-region, save-valid-config (mutates → save+restore)

## Cloudlabs config sheets  — tag `@cloudlabsconfig`
- ✅ **Content_Integration_Git_URL** (`@contentsource`): `descriptionsource` manual/git toggles the
  `descriptiongiturl` field (the general/sandbox variant; guided `guidecontentsource` + challenge
  `challengecontentsource` already covered). 🔲 git-fetch valid/invalid + full 3-lab-type matrix.
- ✅ **Difficulty_Level_Configuration** (`@difficulty`): activity `difficulty_level` select (configured
  levels + Beginner default); admin "Difficulty Level Settings" (`cloudlabs/difficulty_levels`) on
  Cloud Server Settings. ⚠️ Spec discrepancy: documented levels Beginner/Practitioner/Proficient/Expert
  vs actual Beginner/Intermediate/Advanced/Expert (admin-configurable). 🔲 empty-save handling.

## Not started (next sheets — apply the CLAUDE.md coverage-matrix + gap-rows gate first)
- 🔲 Reports (Kiwi: 203 — cloudlabs report), Activities (68), All Course Page (107)
- 🔲 Settings, cron, Logs, Lab_Templates, LTI & SSO, Cloudlabs Integration, LoadBalancer
- 🔲 New Kiwi feature sheets: AWS_Marketplace, Randomization_Activity, Practice_Project, Sandbox,
  Lab_Control_Panel, My_Labs, etc. (many overlap Playground/Guided/Assessment already covered)
- (Assessment: batches 1-3 done — see its section; Roles + Groups: first batches done — remainder open there)

## Gap rows written into the xlsx (derived, `CGAP-*`)
- ✅ Old master `MoodleRegression_Testing.xlsx`: `Moodle_users` CGAP-U-1..12 · `Moodle_Tags` CGAP-T-1..11 ·
  `Moodle_Roles` CGAP-R-1..12 · `Groups_Testcases` CGAP-GR-1..11
- ✅ **New master `MoodleRegression_Testing_Kiwi.xlsx`** (team's updated doc): all of the above PLUS
  37 Playground gap rows, `Moodle_Guided_Labs` CGAP-PGG (19), `Moodle_Assessment_Labs` CGAP-A-1..12 —
  102 rows merged, Case_Status=PROPOSED, deduped, backed up. Matrices in ../Docs/*_Coverage_Matrix.md.

## Infra / cross-cutting
- ✅ Fresh-user lab + CloudLabs API polling (extend/expire) — the standard lab method
- ✅ WS ApiClient (seed/enrol/delete users); id-based profile checks for the report-builder list
- 🔲 Traceability results column refresh (needs one comprehensive run per suite)
- 🔲 Report-builder deep handling (would unlock CGAP-U-9/11/12 + faster tag ops)
