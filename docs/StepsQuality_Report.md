# Steps data-quality report

Scan of the `Steps` column across case sheets. The `Steps` are unreliable (copy-paste/
templating); automation derives steps from Summary+Title+source instead. Flags below are
for the QA team to clean up.

## Summary: 1 cross-lab mismatches | 6 templated step-blocks (>=3 cases share identical Steps) | 905 empty Steps

## Cross-lab-type mismatches (Steps mention the wrong lab type)
- **Moodle_Assessment_Labs** — Steps mention `playground`:  Verify Admin can create assessment activity for all platform

## Templated/duplicated Steps (>=3 cases share identical Steps - likely copy-paste)
- **CloudLabs_Lab_Creation_Status** — 13 cases share one Steps block, e.g.:  All 8 activities returned for courseId=30
- **Pre_Delivery_Lab_Checklist** — 6 cases share one Steps block, e.g.:  Verify that the overall checklist completion percentage
- **Pre_Delivery_Lab_Checklist** — 5 cases share one Steps block, e.g.:  Verify that the activity name filter on the dashboard r
- **NuveBulkMail** — 4 cases share one Steps block, e.g.:  Verify Manage Templates card is visible on dashboard
- **Moodle_Reports** — 3 cases share one Steps block, e.g.:  Verify Completion Status column displays correct status
- **CloudLabs_Lab_Creation_Status** — 3 cases share one Steps block, e.g.:  ResponseStatus = SUCCESS for valid request

(total 6 such blocks across sheets)

## Empty Steps
- 905 cases have a Summary but no Steps. e.g.:  Check if user I can be imported in bulk or not