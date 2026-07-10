# Roles — Automation Findings (cloudlabs capability matrix)

Automated verification of each role's 12 cloudlabs capabilities against the plugin's documented
defaults (`Activity/cloudlabs/db/access.php`). Run: `mvnw test -Dcucumber.tags="@rolematrix"`.
Generated 2026-07-06 against regtest311.cloudloka.com.

**Documented default (all 12 caps):** student = Prevent · editing-teacher / manager / course-creator = Allow
· non-editing-teacher = not set. Site Administrators bypass all capability checks.

## ✅ Roles matching the documented defaults
| Role | Result |
|---|---|
| Manager (roleid 1) | all 12 caps **Allow** — matches (R33-R43) |
| Course creator (roleid 2) | all 12 caps **Allow** — matches (R55-R65) |
| Student (roleid 5) | all 12 caps **Prevent** — matches (R78-R79) |

## ✅ RESOLVED — both deviations fixed (verified by automation 2026-07-06)
After the two deviations below were reported, the site role config was corrected and the
`@rolematrix` automation re-run: **all five roles now match the documented defaults** (Editing teacher
regained Create/Delete/Start-Stop Labs + Manage Overrides; Non-editing teacher lost the 2 assessment
capabilities). The record below is retained for history.

## ⚠️ Deviations found (site config ≠ documented defaults) — since fixed

### 1. Editing teacher is MISSING 4 lab capabilities (has 8/12)
Expected Allow (per R50-R54) but the site has **not set**:
- `mod/cloudlabs:activityuserlabscreate` — Create User Labs (R51)
- `mod/cloudlabs:activityuserlabsdelete` — Delete User Labs (R52)
- `mod/cloudlabs:activityuserlabsstartstop` — Start/Stop User Labs (R53)
- `mod/cloudlabs:activityuseroverrides` — Manage User Overrides (R54)

Effect: an editing teacher cannot create / delete / start-stop labs or manage overrides on this site.
Likely an **intentional restriction** — confirm with the product owner.

### 2. Non-editing teacher HAS 2 assessment capabilities it should not
Expected not-set (per **R66**: "non-editing teacher should not have any assessment permissions") but
the site has **Allow**:
- `mod/cloudlabs:activityuserassessments` — Manage User Assessments (R67)
- `mod/cloudlabs:activityuserassessmentsmanageattempts` — Manage Assessment Attempts (R68)

Effect: a non-editing teacher can manage user assessments + attempts. This **contradicts R66** and is
a likely **defect / config drift** — recommend triage.

## Test handling
The matrix tests assert the documented defaults. The two deviating roles are separate scenarios tagged
`@roledeviation` that **fail by design** to keep the deviation visible until confirmed/fixed. For a
green run: `-Dcucumber.tags="@rolematrix and not @roledeviation"`.
