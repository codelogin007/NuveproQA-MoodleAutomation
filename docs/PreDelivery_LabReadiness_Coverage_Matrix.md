# Pre-Delivery Lab Readiness Checklist — Coverage Matrix

_Feature: `lab_readiness.php` in the cloudlabs plugin. Kiwi sheet `Pre_Delivery_Lab_Checklist` (72 cases).
Generated 2026-07-08. Same method as the other modules._

## Control inventory (anchors)
- **URL:** `/mod/cloudlabs/lab_readiness.php?id={cmid}` (works for any cloudlabs activity — playground/
  guided/challenge). Reached via the activity settings-nav node "Lab Readiness".
- **Capability:** `mod/cloudlabs:viewlabreadiness` (db/access.php) — student=PREVENT; editingteacher/
  manager/coursecreator/admin=ALLOW. Enforced at lab_readiness.php:15 `require_capability`.
- **Form** `#labReadinessForm` (POST, multipart). Checklist items `.checklist-item[data-label]` in
  Catalog(7)/Orgo(11)/Moodle-<labtype>(5-15) cards. Progress bar `#labProgressBar` — color by %:
  ≤40 bg-danger, ≤75 bg-warning, <100 bg-info, 100 bg-success (green). Progress counts VISIBLE boxes.
- **Env type:** `input[name='envtype'][value=vm|account]` toggles `.vm-field` / `.account-field`.
- **Mandatory:** `#supportName`/`input[name='supportname']` (alert "Please enter Support Engineer Name.")
  and a screenshot `input[name='screenshot']` (alert "Please upload a reference screenshot...") — both
  `alert()` + `return` BEFORE `form.submit()` (lab_readiness.js), so a failed validation persists nothing.
- **Save** `#saveChecklistBtn`; persists to `cloudlabs_lab_readiness`, fires `lab_readiness_updated`
  event (audit log), and at 100% sets `course_modules.visible=1`. Audit log table "Checklist Audit Log".

## Automated (batch 1, `@predelivery` — all green, NO successful save)
| Case | Scenario |
|---|---|
| Lab Readiness tab admin/manager only, not student | admin sees form; WS student blocked (cap) |
| Checklist present + completion % + green at 100% | tick all visible -> #labProgressBar 100% bg-success |
| Env type VM/Account toggles fields | envtype radios toggle .vm-field/.account-field |
| Mandatory Support Engineer Name | empty name -> alert -> save blocked (nothing persists) |

## Derived gaps (CGAP-PDL)
| Id | Case |
|---|---|
| CGAP-PDL-1 | Progress bar color thresholds: bg-danger ≤40, bg-warning ≤75, bg-info <100, bg-success at 100 |
| CGAP-PDL-2 | Env-type toggle RECOMPUTES the % (hidden fields excluded from the count) |
| CGAP-PDL-3 | Mandatory screenshot: valid name but no screenshot -> alert, save blocked |
| CGAP-PDL-4 | At 100% + saved, a subsequent config change raises the backend-warning banner |

## Deferred (need file upload / write / log assertions)
- Successful save + audit-log row (mutates the activity + sets it visible) — needs a throwaway activity
- Screenshot upload + re-upload override (file upload + audit log)
- Dashboard: activity/project/support-engineer filters + search + clear (readiness dashboard page — a
  separate labadminservices surface; batch 2 candidate, read-only, automatable)
- Force-enable-below-100% flagging (needs a specific data state)
