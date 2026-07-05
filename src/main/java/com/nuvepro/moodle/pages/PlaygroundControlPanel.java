package com.nuvepro.moodle.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.assertions.PlaywrightAssertions;

/**
 * Playground lab CONTROL PANEL (console). Selectors read directly from the plugin source:
 *  - Activity\cloudlabs\templates\lab.mustache
 *  - Activity\cloudlabs\templates\playground-controlpanel.mustache
 * Mirrors pages/playground_control_panel.py.
 */
public class PlaygroundControlPanel extends BasePage {
    public static final String MASTER = ".np-ap-pl-template-master-container";
    public static final String START_LAB_ACCOUNT = "#np-ap-mod-access-launch-btn-acc";
    public static final String START_LAB_VM = "#np-ap-mod-access-launch-btn-vm";
    public static final String LAUNCH_START =
            "#np-ap-mod-access-launch-btn-acc, #np-ap-mod-access-launch-btn-vm";
    public static final String STOP_LAB = ".np-ap-pl-playground-end";
    public static final String REFRESH_LAB = "#np-ap-mod-btn-tb-refresh";
    public static final String INSTRUCTION_FAB = "#openMissionBtn";
    public static final String USAGE_FAB = "#openLabUsageBtn";
    public static final String LAB_INFO_FAB = "#openLabInfoBtn";
    public static final String NOTES_TAB = "#notes-tab";
    public static final String NOTES_TEXTAREA = "#np-ap-pl-notes";
    public static final String NOTES_SAVE = "#np-ap-pl-note-save-btn";
    public static final String ACCESS_LAB_MODAL = "#launchAccModal";
    public static final String H_LAB_STATUS = "#np-ap-mod-loadTimeStatus";
    public static final String H_LAB_ID = "#np-ap-mod-labId";

    public PlaygroundControlPanel(Page page) {
        super(page);
    }

    public void open(int cmid) {
        navigate("/mod/cloudlabs/view.php?id=" + cmid);
    }

    public void openInstructions() {
        page.locator(INSTRUCTION_FAB).click();
    }

    public void saveNote(String text) {
        page.locator(NOTES_TAB).click();
        page.locator(NOTES_TEXTAREA).fill(text);
        page.locator(NOTES_SAVE).click();
    }

    /** Click Stop Lab and confirm; no-op if already stopped (button disabled). */
    public void stopLab() {
        Locator btn = page.locator(STOP_LAB).first();
        if (!btn.isEnabled()) return;
        btn.click();
        confirmModal();
    }

    public String labStatus() {
        return hiddenInputValue(H_LAB_STATUS);
    }

    public String labId() {
        return hiddenInputValue(H_LAB_ID);
    }

    /** After Start Sandbox + confirm, control panel content loads in-place. */
    public void expectControlPanelLoaded(double timeoutMs) {
        PlaywrightAssertions.assertThat(page.locator(STOP_LAB + ", " + LAUNCH_START).first())
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(timeoutMs));
    }

    public void expectControlPanelLoaded() {
        expectControlPanelLoaded(60_000);
    }
}
