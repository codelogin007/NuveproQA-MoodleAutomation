# Progress Tracker — Nuvepro Moodle UI Automation

Living status of what's **automated (green)**, **open (to do)**, and **deferred (with reason)**, by
module. Update as batches complete. Legend: ✅ done · 🔲 open · ⏸️ deferred/blocked.
Last updated: 2026-07-06.

Repo: https://github.com/codelogin007/NuveproQA-MoodleAutomation (branch `main`).
Run a module: `mvnw test "-Dcucumber.tags=@<tag>"`.

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
- ⏸️ CGAP-PGG-5 Submit: skips — disabled on a stopped lab; needs a submit-while-running scenario
- 🔲 Attempt lifecycle remainder: New Attempt, cooldown, Complete, override — CGAP-PGG-2/3/4/6/7/10/11/12/19
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

## Not started (next sheets — apply the CLAUDE.md coverage-matrix + gap-rows gate first)
- 🔲 Assessment (challenge) — largest sheet (~145+); cloudlabs provisioning
- 🔲 Reports (46 — cloudlabs report), Activities (48), All Course Page (44)
- 🔲 Settings, cron, Logs, Lab_Templates, LTI & SSO, Cloudlabs Integration, LoadBalancer
- (Roles + Groups: first batches done — see their sections above; remainder open there)

## Gap rows written into the xlsx (derived, `CGAP-*`)
- ✅ `Moodle_users`: CGAP-U-1..12 · `Moodle_Tags`: CGAP-T-1..11 · `Moodle_Roles`: CGAP-R-1..12 · `Groups_Testcases`: CGAP-GR-1..11
- 🔲 Playground/Guided gaps live in the matrix `.md` docs — not yet appended to their sheets

## Infra / cross-cutting
- ✅ Fresh-user lab + CloudLabs API polling (extend/expire) — the standard lab method
- ✅ WS ApiClient (seed/enrol/delete users); id-based profile checks for the report-builder list
- 🔲 Traceability results column refresh (needs one comprehensive run per suite)
- 🔲 Report-builder deep handling (would unlock CGAP-U-9/11/12 + faster tag ops)
