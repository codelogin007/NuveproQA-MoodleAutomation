# Playground (Sandbox) — Coverage Matrix

Artifact-driven coverage analysis for the playground lab type. Derived from the cloudlabs Mustache
templates (control inventory), the lab lifecycle (state machine), and cross-cut by template options
and roles; de-duplicated against the existing regression sheets.

**Legend:** ✅ covered (case id) · 🟥 **GAP** (candidate new case) · ↪ covered in another sheet (dedup)

Case ID sources: `PG-1..16` (original) · `CGAP-PG-17..39` (gap-fill added) · other sheets noted inline.

Templates inventoried: `playground-landing.mustache`, `playground-controlpanel.mustache`,
`lab.mustache`, `manage-labs.mustache`. Lab states/actions from the hidden state inputs + manage-labs
counters. Template config options from `lab.mustache` hidden inputs (`enable_cost`, `enable_duration`,
`enable_lab_notes`, `enable_speedtest`, `enable_lab_prov_input`, `is_iframe`, `is_composition`,
`enable_notification`, `lab_info`).

> **SCOPE (see CLAUDE.md / TODO.md):** roles under test = **admin + student** only. Complete all
> cases on **Account** lab templates first; **cloud platforms (AWS/CSP/GCP), Composition and VM**
> variants are **DEFERRED to the end** and need activities with those template types.

---

## A. Control inventory × coverage

### A1. Create activity (mod_form) — `course/modedit.php?add=cloudlabs`
| Control / behaviour | Positive | Negative | Roles |
|---|---|---|---|
| Lab type radio → Playground | ✅ PG-1 | — | ✅ CGAP-PG-24 |
| Name / description fields | ✅ PG-1 | 🟥 required-field validation | |
| Lab template select | ✅ PG-1 | 🟥 no template selected | |
| Save (create) per platform AWS/CSP/GCP | ✅ CGAP-PG-22 | 🟥 create with bad template → error (✅ CGAP-PG-31) | |

### A2. Playground landing (`playground-landing.mustache`)
| Control / behaviour | Positive | Negative |
|---|---|---|
| Start Sandbox `#np-ap-pl-playground-start` | ✅ PG-12 | — |
| Start confirm modal (Yes) | ✅ PG-12 | 🟥 **Cancel** path (no lab created) |
| Continue `.np-ap-pl-continue` (resume active lab) | 🟥 **explicit resume case** | — |
| Details panel: Lab Created / Type / Status | 🟥 **fields correct** | — |
| Duration Allotted / Consumed / Remaining | ✅ CGAP-PG-26 | — |
| Sandbox ID, Enrolment End Date | 🟥 **shown correctly** | — |
| Description + Read More modal | 🟥 **read-more opens full text** | — |
| Manage Labs link | ✅ PG-11 | — |

### A3. Control panel / console (`playground-controlpanel.mustache` + `lab.mustache`)
| Control / behaviour | Positive | Negative |
|---|---|---|
| In-panel Start Lab (acc/vm) | ✅ PG-12 | 🟥 provider failure → error |
| Stop Lab `.np-ap-pl-playground-end` | ✅ PG-15 | — |
| Instruction FAB `#openMissionBtn` | ✅ PG-6 | — |
| Download instructions PDF `#downloadPdfBtnPlayground` | 🟥 **download PDF** | — |
| Lab Usage FAB `#openLabUsageBtn` (popup) | ✅ PG-3 | — |
| Usage cost values (Allotted/Consumed/Remaining) | ✅ CGAP-PG-25 | — |
| Lab Info FAB `#openLabInfoBtn` | ✅ PG-5 | — |
| **Lab Credentials FAB `#openAuthBtn`** | 🟥 **view credentials** | — |
| Set password / change password `#np-ap-mod-set-password-btn` | 🟥 **change lab password** | — |
| Notes tab + save | ✅ PG-8 / CGAP-PG-27 | — |
| **Support tab (contact support)** | 🟥 **support mailto** | — |
| Toolbar: Refresh `#np-ap-mod-btn-tb-refresh` | ✅ PG-16 / CGAP-PG-29 | — |
| Toolbar: **Fullscreen** `#...-fullscreen` | 🟥 | — |
| Toolbar: **Open in new tab** `#...-newtab` | 🟥 | — |
| Toolbar: Speed Test `#...-speedTest` | ✅ PG-7 / CGAP-PG-28 | — |
| **Machines dropdown** (multi-machine VM) | 🟥 **switch machine** | — |
| Jump to Console → access modal `#launchAccModal` | ✅ PG-13 | — |
| Account launch buttons → platform console | ✅ PG-14 / CGAP-PG-30 | — |
| VM lab loads in iframe | ✅ PG-4 | — |
| **Composition** lab (sub-catalog tabs, `#subCatalogDataSubmitBtn`) | 🟥 **composition provision** | — |
| Catalog UI input (`enable_lab_prov_input`) | ✅ PG-9 | 🟥 invalid input |
| Back button `#pl-back-btn` | 🟥 **returns to landing** | — |
| **Lab state messages**: deleted / expired / no-access / popped | 🟥 **each renders on that state** | — |
| Duration/expiry notification (`enable_notification`) | 🟥 **warning near expiry** | — |

### A4. Manage Labs (`manage-labs.mustache`)
| Control / behaviour | Positive | Negative | Dedup |
|---|---|---|---|
| Group filter `#course_groups` | 🟥 **filters list** | — | ↪ Groups_Testcases |
| Tabs: created / not-created | 🟥 **populations correct** | — | ↪ Activities |
| Status counters Running/Stopped/Deleted/Failed | ✅ PG-11 | — | |
| Create (created tab) | ✅ CGAP-PG-33 | — | |
| Create (not-created / pre-create) | — | — | ↪ Activities (Pre-create labs) |
| Actions ▸ Start | ✅ CGAP-PG-17 | ✅ CGAP-PG-39 | |
| Actions ▸ Stop | ✅ CGAP-PG-18 | ✅ CGAP-PG-39 | |
| Actions ▸ Delete | ✅ CGAP-PG-19 | ✅ CGAP-PG-39 | |
| Actions disabled without selection | ✅ CGAP-PG-20 | | |
| Bulk action (multi-select) | ✅ CGAP-PG-21 | | |
| Lab Details `#np-ap-manage-view-btn` | ✅ CGAP-PG-37 | | |
| Sync Status `#np-ap-manage-sync-btn` | ✅ CGAP-PG-38 | | |
| Report ▸ Download (active + no-labs, group select) | — | — | ↪ Activities (Export manage labs report) |
| Table search / pagination / export | — | — | ↪ Activities |
| Column toggle / show-columns | 🟥 minor | — | |

---

## B. Lab state-transition matrix
States from `#np-ap-mod-loadTimeStatus` + manage-labs counters. Cell = case (✅) / GAP (🟥) /
`invalid` (should be blocked — negative).

| From ↓ \ Action → | Create | Start | Stop | Delete | Sync |
|---|---|---|---|---|---|
| **NotCreated** | ✅ PG-12 / CGAP-PG-33 | invalid | invalid | invalid → ✅ CGAP-PG-39 | ✅ CGAP-PG-38 |
| **Creating** | — | invalid | invalid | 🟥 cancel-in-progress | — |
| **Running** | invalid | invalid → ✅ CGAP-PG-39 | ✅ PG-15 / CGAP-PG-18 | ✅ CGAP-PG-19 | ✅ CGAP-PG-38 |
| **Stopped** | — | ✅ CGAP-PG-35 (restart) | invalid | ✅ CGAP-PG-19 | — |
| **Deleted** | ✅ CGAP-PG-34 (recreate) | invalid | invalid | invalid | — |
| **Failed** | ✅ CGAP-PG-36 | 🟥 retry start | — | ✅ CGAP-PG-19 | — |

---

## C. Cross-cutting: template config options → playground behaviour
Each option, when enabled on the lab template, changes the console. One case per option (on-path),
plus the off-path (control hidden) where meaningful.

| Template option | Behaviour when ON | Case |
|---|---|---|
| Embed Lab / iframe (VM) | lab loads in iframe | ✅ PG-4 |
| Cost enabled | Usage cost shown | ✅ CGAP-PG-25 |
| Duration enabled | Allotted/Consumed/Remaining shown | ✅ CGAP-PG-26 |
| Notes enabled | Notes tab present + save | ✅ PG-8 |
| Speed test enabled | Speed test runs | ✅ CGAP-PG-28 |
| Catalog UI input | input surface to provision | ✅ PG-9 |
| Composition | sub-catalog selection + provision | 🟥 gap |
| Lab info / instructions / access details | shown in FABs | ✅ PG-6 (partial) 🟥 access-details/lab-info correctness |
| Notification enabled | expiry warning shown | 🟥 gap |
| Unlimited duration (FDD) | no expiry | 🟥 gap |

---

## D. Cross-cutting: roles — SCOPE = admin + student only
Per project scope (CLAUDE.md), only **admin** and **student** roles are tested. Manager / teacher /
non-editing-teacher are **deferred / on request**.

| Role | Create activity | Launch lab | Manage Labs + actions |
|---|---|---|---|
| Admin | ✅ PG-1 | ✅ | ✅ CGAP-PG-17..21, 33..39 |
| Student | n/a | ✅ CGAP-PG-23 | ✅ CGAP-PG-24 (cannot manage) |
| ~~Manager / Teacher / Non-editing~~ | deferred | deferred | deferred |

---

## E. Remaining GAPS — candidate new cases (`CGAP-PG-40+`)
Not yet covered by any case (playground sheet or dedup sheets):

1. Start confirm modal **Cancel** → no lab created.
2. **Continue/resume** an active lab from the landing.
3. Landing **Details fields** correct (Lab Created, Type, Status, Sandbox ID, Enrol end date).
4. Description **Read More** modal shows full content.
5. Instructions **Download PDF**.
6. **Lab Credentials** FAB shows access credentials.
7. **Change/Set lab password**.
8. **Support** tab (contact support mailto).
9. Toolbar **Fullscreen**.
10. Toolbar **Open in new tab** (lab pops out; landing shows "launched in another tab").
11. **Machines dropdown** — switch between machines (multi-machine VM lab).
12. **Composition** playground lab — sub-catalog selection + provision.
13. Catalog UI input **invalid input** handling (negative).
14. **Back** button returns to landing.
15. Lab **state messages** render correctly: deleted / expired / no-access / popped-out.
16. **Notification** near duration expiry.
17. **Unlimited duration** template → lab does not expire (FDD: Lab Usage for Unlimited Duration).
18. Manage Labs **Group filter** narrows the list (dedup-check vs Groups sheet).
19. Manage Labs **tabs** show correct created/not-created populations (dedup-check vs Activities).
20. Provider failure on **Start** → clear error (negative).
21. Access-details / lab-info **content correctness** (not just popup opens).
22. Manage Labs **column toggle / export** from playground context (dedup vs Activities).

---

## F. Coverage summary (playground)
- **Original manual cases:** 16 (PG-1..16)
- **Gap-fill added:** 23 (CGAP-PG-17..39)
- **Remaining candidate gaps:** ~22 (section E) — some are dedup-checks against Activities/Groups sheets.
- **Covered by other sheets (dedup):** pre-create labs, export manage-labs report, group dropdown,
  course progress report, template config validation, enrolment-based lab deletion.

Once section E is reviewed/approved, the "fool-proof" playground set is complete and we automate the
full set. This matrix is the template for every other module (Guided, Assessment, …).
