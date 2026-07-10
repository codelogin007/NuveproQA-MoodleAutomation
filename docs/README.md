# Docs — test cases & coverage

Version-controlled copies of the QA test-case master and the automation coverage docs.

## Files
- **`MoodleRegression_Testing_Kiwi.xlsx`** — the current master test-case workbook (Kiwi TCMS export).
  - Per-case automation status lives in each sheet's **`Automated`** column (`Yes` / `No` / `Deferred` /
    `Deviation`) with the exact test or reason in **`Automation_Ref`**.
  - The **`Automation_Coverage`** sheet is the at-a-glance dashboard (per-sheet Yes/No/Deferred/Deviation
    counts + % automated).
  - Derived gap cases are the `CGAP-*` rows (Case_Status = PROPOSED).
- **`*_Coverage_Matrix.md`** — per-module control inventory + coverage plan (Playground, Guided,
  Assessment, Pre-Delivery Lab Readiness).
- **`Roles_Findings.md`** — the roles/permissions findings write-up.

## Conventions
- This repo copy is the versioned source; update it here (via PR) so changes flow through git.
- Mark a case `Automated = Yes` (with its `feature @tag`) only when a green test verifies THAT case
  (presence ≠ behaviour). Deviations are fail-by-design tests flagging a real defect.
- Backups (`*.backup*.xlsx`) and Excel lock files (`~$*`) are git-ignored.

See `../PROGRESS.md` for the live per-module status and the coverage-at-a-glance table.
