package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manage Labs listing (managelabs.php). Used for independent verification of lab state
 * (Running/Stopped/Deleted/Failed counters). Mirrors pages/manage_labs.py.
 */
public class ManageLabs extends BasePage {
    private static final String[] KEYS = {"Running", "Stopped", "Deleted", "Failed"};

    // Toolbar controls (created-users tab)
    public static final String ACTIONS_BTN = "#np-ap-cl-assessment-lab-actions";
    public static final String START = "#np-ap-manage-start-btn";
    public static final String STOP = "#np-ap-manage-stop-btn";
    public static final String DELETE = "#np-ap-manage-delete-btn";
    public static final String CREATE = "#np-ap-manage-create-btn";
    public static final String LAB_DETAILS_BTN = "#np-ap-manage-view-btn";
    public static final String LAB_DETAIL_MODAL = "#lab-detail-modal";
    public static final String SYNC_BTN = "#np-ap-manage-sync-btn";
    public static final String ROW_CHECKBOX = "#np-ap-manage-lab-table input[name='btSelectItem']";
    // Tabs
    public static final String CREATED_TAB = "a[href='#labcreatedusers']";
    public static final String NOTCREATED_TAB = "a[href='#labnotcreatedusers']";
    public static final String NOTCREATED_CREATE = "#np-ap-manage-labsnotcreated-create-btn";

    /** True if the given action item (`<a disabled>`) currently carries the disabled attribute. */
    public boolean actionDisabled(String selector) {
        Locator l = page.locator(selector);
        return l.count() > 0 && l.first().getAttribute("disabled") != null;
    }

    public boolean hasLabRows() {
        return page.locator(ROW_CHECKBOX).count() > 0;
    }

    public void selectFirstRow() {
        page.locator(ROW_CHECKBOX).first().check();
    }

    public int rowCount() {
        return page.locator(ROW_CHECKBOX).count();
    }

    public void selectAllRows() {
        int n = rowCount();
        for (int i = 0; i < n; i++) {
            page.locator(ROW_CHECKBOX).nth(i).check();
        }
    }

    /** First row's text (Lab ID | email | Action | Status | ...) for state assertions. */
    public String firstRowText() {
        Locator row = page.locator("#np-ap-manage-lab-table tbody tr").first();
        return row.count() > 0 ? row.innerText().replaceAll("\\s+", " ").trim() : "";
    }

    /**
     * Select the first lab row, open the Actions dropdown (for Start/Stop/Delete), click the
     * action and confirm the Moodle dialog.
     *
     * NOTE: Start/Stop/Delete are <a class="lab-action-btn"> anchors inside the Actions dropdown.
     * The 'disabled' ATTRIBUTE has no effect on an anchor (only <button>/<input> honour it) and the
     * click handler fires regardless — so we do NOT gate on it (doing so wrongly skipped Delete,
     * which carries a cosmetic 'disabled' attr from a missing comma in setToolbarButtonState but is
     * fully functional). Create is a real <button>, so its disabled state IS respected by the caller.
     */
    public void performActionOnFirstRow(String itemSelector) {
        selectFirstRow();
        page.waitForTimeout(1_000); // let the selection event update the toolbar
        boolean inDropdown = START.equals(itemSelector) || STOP.equals(itemSelector) || DELETE.equals(itemSelector);
        if (inDropdown) {
            page.locator(ACTIONS_BTN).click();
            page.waitForTimeout(400);
        }
        page.locator(itemSelector).click(new Locator.ClickOptions().setForce(true));
        // Start/Stop/Delete/Create raise a Moodle confirm; Sync Status fires immediately (no dialog).
        try {
            confirmModal();
        } catch (Throwable noDialog) {
            // no confirmation for this action — proceed
        }
    }

    /** Public wrapper so step classes can confirm the Moodle dialog raised by a bulk action. */
    public void confirmVisibleDialog() {
        confirmModal();
    }

    // ===================== lab state machine (provider-aware, cp.php-free) =====================

    public enum LabState { NOTCREATED, RUNNING, STOPPED, DELETED, INPROGRESS, FAILED, UNKNOWN }

    private static final java.util.Set<LabState> SETTLED =
            java.util.EnumSet.of(LabState.NOTCREATED, LabState.RUNNING, LabState.STOPPED,
                    LabState.DELETED, LabState.FAILED);

    /** Map an action + status pair (e.g. from the CloudLabs API's nl_action/nl_status) to a state. */
    public static LabState classifyActionStatus(String action, String status) {
        if (action == null) return LabState.NOTCREATED;
        String a = action.toLowerCase();
        String s = status == null ? "" : status.toLowerCase();
        if (s.contains("inprogress") || s.contains("in progress") || s.contains("pending")
                || s.contains("processing") || s.contains("queued")) return LabState.INPROGRESS;
        if (s.contains("failed") || s.contains("error")) return LabState.FAILED;
        if (a.contains("delete") && s.contains("complete")) return LabState.DELETED;
        if (a.contains("stop") && s.contains("complete")) return LabState.STOPPED;
        if ((a.contains("create") || a.contains("start")) && (s.contains("complete") || s.contains("success")))
            return LabState.RUNNING;
        return LabState.UNKNOWN;
    }

    /** Map a Manage Labs row's text (action + status) to a logical lab state. */
    private LabState classify(String rowTextLower) {
        String r = rowTextLower;
        if (r.contains("inprogress") || r.contains("in progress") || r.contains("pending")
                || r.contains("processing") || r.contains("queued")) return LabState.INPROGRESS;
        if (r.contains("failed") || r.contains("error")) return LabState.FAILED;
        if (r.contains("delete") && r.contains("complete")) return LabState.DELETED;
        if (r.contains("stop") && r.contains("complete")) return LabState.STOPPED;
        if ((r.contains("start") || r.contains("create") || r.contains("running"))
                && (r.contains("complete") || r.contains("running") || r.contains("success")))
            return LabState.RUNNING;
        System.out.println("[ManageLabs] classify UNKNOWN for row: " + r.replaceAll("\\s+", " ").trim());
        return LabState.UNKNOWN;
    }

    /** Authoritative current state of the (first) lab, read from its Manage Labs row (action+status). */
    public LabState firstRowState() {
        if (!hasLabRows()) return LabState.NOTCREATED;
        return classify(firstRowText().toLowerCase());
    }

    // ---- email-targeted operations (for fresh per-user labs) ----

    private Locator createdRowForEmail(String email) {
        return page.locator("#np-ap-manage-lab-table tbody tr")
                .filter(new Locator.FilterOptions().setHasText(email));
    }

    /** State of the lab belonging to {@code email} (created-users tab). */
    public LabState stateForEmail(String email) {
        Locator row = createdRowForEmail(email);
        if (row.count() == 0) return LabState.NOTCREATED;
        return classify(row.first().innerText().toLowerCase());
    }

    /** The lab id shown in Manage Labs = the CloudLabs provider subscriptionId. */
    private String extractLabId(Locator row) {
        for (String cell : row.locator("td").allInnerTexts()) {
            String t = cell.trim();
            if (t.matches("\\d{3,}")) return t;   // lab id (avoids the 1-digit retake column)
        }
        return null;
    }

    public String labIdForEmail(String email) {
        Locator row = createdRowForEmail(email);
        return row.count() > 0 ? extractLabId(row.first()) : null;
    }

    /** All lab ids in rows for this user (a user can have an old deleted lab + a fresh one). */
    public java.util.List<String> labIdsForEmail(String email) {
        java.util.List<String> ids = new java.util.ArrayList<>();
        Locator rows = createdRowForEmail(email);
        for (int i = 0; i < rows.count(); i++) {
            String id = extractLabId(rows.nth(i));
            if (id != null) ids.add(id);
        }
        return ids;
    }

    public String firstRowLabId() {
        Locator row = page.locator("#np-ap-manage-lab-table tbody tr").first();
        return row.count() > 0 ? extractLabId(row) : null;
    }

    /** Poll until this user's lab reaches a target state (reloading Manage Labs each time). */
    public LabState waitForEmailState(String email, java.util.Set<LabState> targets, int maxSeconds, int sectionId) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        LabState st = stateForEmail(email);
        while (System.currentTimeMillis() < deadline) {
            if (targets.contains(st)) return st;
            page.waitForTimeout(15_000);
            open(sectionId);
            st = stateForEmail(email);
        }
        return st;
    }

    /** Create a lab for an enrolled user via the not-created-users tab (admin action, no cp.php). */
    public void createLabForUserEmail(String email, int sectionId) {
        open(sectionId);
        page.locator(NOTCREATED_TAB).click();
        page.waitForTimeout(2_000);
        Locator row = page.locator("#labnotcreatedusers tbody tr")
                .filter(new Locator.FilterOptions().setHasText(email));
        if (row.count() == 0) {
            throw new IllegalStateException("user not found in not-created list: " + email);
        }
        row.first().locator("input[name='btSelectItem']").check();
        page.waitForTimeout(1_000);
        page.locator(NOTCREATED_CREATE).click(new Locator.ClickOptions().setForce(true));
        try { confirmModal(); } catch (Throwable ignored) {}
    }

    /** Check the row checkboxes for several users (for a bulk action). */
    public void selectRowsForEmails(java.util.List<String> emails) {
        for (String email : emails) {
            Locator row = createdRowForEmail(email);
            if (row.count() > 0) row.first().locator("input[name='btSelectItem']").check();
        }
    }

    /** Perform an action on the CURRENT multi-row selection (open Actions dropdown, click, confirm). */
    public void performActionOnSelection(String itemSelector) {
        page.waitForTimeout(1_000);
        boolean inDropdown = START.equals(itemSelector) || STOP.equals(itemSelector) || DELETE.equals(itemSelector);
        if (inDropdown) {
            page.locator(ACTIONS_BTN).click();
            page.waitForTimeout(400);
        }
        page.locator(itemSelector).click(new Locator.ClickOptions().setForce(true));
        try { confirmModal(); } catch (Throwable ignored) {}
    }

    /** Perform Start/Stop/Delete on the created-tab row belonging to {@code email}. */
    public void performActionForEmail(String email, String itemSelector, int sectionId) {
        open(sectionId);
        Locator row = createdRowForEmail(email);
        if (row.count() == 0) throw new IllegalStateException("no lab row for user: " + email);
        row.first().locator("input[name='btSelectItem']").check();
        page.waitForTimeout(1_000);
        boolean inDropdown = START.equals(itemSelector) || STOP.equals(itemSelector) || DELETE.equals(itemSelector);
        if (inDropdown) {
            page.locator(ACTIONS_BTN).click();
            page.waitForTimeout(400);
        }
        page.locator(itemSelector).click(new Locator.ClickOptions().setForce(true));
        try { confirmModal(); } catch (Throwable ignored) {}
    }

    /**
     * Robust read that tolerates slow bootstrap-table AJAX: an UNKNOWN result is usually a
     * not-yet-loaded row, so re-read a few times (short waits, one reload) before concluding.
     * This prevents mis-reading an already-settled lab as UNKNOWN and triggering a long wait.
     */
    public LabState firstRowStateStable(int sectionId) {
        LabState st = firstRowState();
        for (int i = 0; i < 3 && st == LabState.UNKNOWN; i++) {
            page.waitForTimeout(2_000);
            st = firstRowState();               // cheap re-read (data may have just populated)
        }
        if (st == LabState.UNKNOWN) {           // one full reload before giving up
            open(sectionId);
            st = firstRowState();
        }
        return st;
    }

    /** Poll (reloading Manage Labs) until the lab reaches one of the target states, or timeout. */
    public LabState waitForFirstRowState(java.util.Set<LabState> targets, int maxSeconds, int sectionId) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        LabState st = firstRowState();
        while (System.currentTimeMillis() < deadline) {
            if (targets.contains(st)) return st;
            page.waitForTimeout(15_000);
            open(sectionId);
            st = firstRowState();
        }
        return st;
    }

    /**
     * Drive the (single) lab to {@code target} using Manage Labs actions, waiting for each
     * transition to COMPLETE. Idempotent: if already there (or reachable in one step) it returns
     * quickly; otherwise it performs the needed action(s) and waits. Does NOT use cp.php.
     * Returns the state actually reached.
     */
    public LabState ensureState(LabState target, int sectionId) {
        LabState cur = LabState.UNKNOWN;
        for (int hop = 0; hop < 4; hop++) {   // RUNNING is the hub: any state reachable in <=3 hops
            open(sectionId);
            cur = firstRowStateStable(sectionId);   // robust read (won't misfire on a slow AJAX load)
            if (cur == LabState.INPROGRESS) {
                cur = waitForFirstRowState(SETTLED, 900, sectionId);   // real transition: wait long
            } else if (cur == LabState.UNKNOWN) {
                cur = waitForFirstRowState(SETTLED, 90, sectionId);    // read glitch: brief wait, then give up
            }
            if (cur == target) return cur;
            LabState next = stepToward(cur, target, sectionId);
            if (next == cur) break;           // no progress possible -> give up
        }
        return cur;
    }

    /** Take ONE transition toward {@code target}, waiting for it to complete; returns the new state. */
    private LabState stepToward(LabState cur, LabState target, int sectionId) {
        java.util.Set<LabState> runOrFail = java.util.EnumSet.of(LabState.RUNNING, LabState.FAILED);
        java.util.Set<LabState> stopOrFail = java.util.EnumSet.of(LabState.STOPPED, LabState.FAILED);
        switch (target) {
            case RUNNING:
                if (cur == LabState.STOPPED) { performActionOnFirstRow(START); return waitForFirstRowState(runOrFail, 900, sectionId); }
                if (cur == LabState.DELETED || cur == LabState.NOTCREATED || cur == LabState.FAILED) {
                    createFirstLab(sectionId); return waitForFirstRowState(runOrFail, 1200, sectionId);
                }
                break;
            case STOPPED:
                if (cur == LabState.RUNNING) { performActionOnFirstRow(STOP); return waitForFirstRowState(stopOrFail, 600, sectionId); }
                if (cur == LabState.DELETED || cur == LabState.NOTCREATED || cur == LabState.FAILED) {
                    createFirstLab(sectionId); return waitForFirstRowState(runOrFail, 1200, sectionId); // next hop stops it
                }
                break;
            case DELETED:
                if (cur == LabState.RUNNING) { performActionOnFirstRow(STOP); return waitForFirstRowState(stopOrFail, 600, sectionId); } // next hop deletes
                if (cur == LabState.STOPPED || cur == LabState.FAILED) {
                    performActionOnFirstRow(DELETE); return waitForFirstRowState(java.util.EnumSet.of(LabState.DELETED), 600, sectionId);
                }
                break;
            default:
                break;
        }
        return cur;   // no applicable transition
    }

    /** Create/provision the lab. For a Deleted/Complete row the created-users Create button applies;
     *  for a truly not-created lab, use the not-created-users tab. */
    private void createFirstLab(int sectionId) {
        open(sectionId);
        if (hasLabRows() && !actionDisabled(CREATE)) {
            selectFirstRow();
            page.waitForTimeout(800);
            performActionOnFirstRow(CREATE);
            return;
        }
        // fall back to the not-created-users tab
        if (page.locator(NOTCREATED_TAB).count() > 0) {
            page.locator(NOTCREATED_TAB).click();
            page.waitForTimeout(1_500);
            Locator cb = page.locator("#labnotcreatedusers input[name='btSelectItem']");
            if (cb.count() > 0) {
                cb.first().check();
                page.waitForTimeout(800);
                page.locator(NOTCREATED_CREATE).click(new Locator.ClickOptions().setForce(true));
                try { confirmModal(); } catch (Throwable ignored) {}
            }
        }
    }

    public ManageLabs(Page page) {
        super(page);
    }

    public void open(int sectionId) {
        navigate("/mod/cloudlabs/managelabs.php?sectionid=" + sectionId);
        page.locator("#np-ap-manage-lab-table").first().waitFor(new Locator.WaitForOptions()
                .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE).setTimeout(30_000));
        // bootstrap-table loads its rows via AJAX AFTER the table element renders. Wait for the
        // data to actually populate (a real lab row OR a definitive "no records" row) before
        // reading state — otherwise we read a placeholder and mis-detect the state as UNKNOWN.
        for (int i = 0; i < 30; i++) {
            boolean hasRow = page.locator(ROW_CHECKBOX).count() > 0;
            boolean noRecords = page.locator("#np-ap-manage-lab-table tbody .no-records-found,"
                    + " #np-ap-manage-lab-table tbody tr.no-records-found").count() > 0;
            if (hasRow || noRecords) break;
            page.waitForTimeout(1_000);
        }
        page.waitForTimeout(1_500);
    }

    /** {Running, Stopped, Deleted, Failed} from the status summary (null if not found). */
    public Map<String, Integer> counters() {
        String text = page.locator("body").innerText();
        Map<String, Integer> out = new LinkedHashMap<>();
        for (String key : KEYS) {
            Matcher m = Pattern.compile(key + "\\s*:?\\s*(\\d+)", Pattern.CASE_INSENSITIVE).matcher(text);
            out.put(key, m.find() ? Integer.parseInt(m.group(1)) : null);
        }
        return out;
    }

    public int runningCount() {
        Integer r = counters().get("Running");
        return r == null ? 0 : r;
    }
}
