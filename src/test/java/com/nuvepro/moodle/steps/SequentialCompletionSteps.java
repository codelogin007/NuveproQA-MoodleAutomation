package com.nuvepro.moodle.steps;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.nuvepro.moodle.config.Settings;
import com.nuvepro.moodle.helpers.ApiClient;
import com.nuvepro.moodle.helpers.Auth;
import com.nuvepro.moodle.pages.CourseAdmin;
import com.nuvepro.moodle.pages.CourseView;
import io.cucumber.java.AfterAll;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.testng.SkipException;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Sequential Activity Completion (@sac) — mark-as-done subset. Builds ONE self-contained fixture,
 * shared across the ordered scenarios (like the lab lifecycle): a fresh course with
 *   Activity 1 = URL, completion "students manually mark as done"
 *   Activity 2 = URL, restricted until Activity 1 is marked complete
 * plus a freshly-seeded student enrolled in the course. Scenarios run in feature order: the
 * restricted/incomplete checks first, then Activity 1 is marked done, then the enabled/completed and
 * persistence checks. @AfterAll deletes the course and the seeded student (leaves the site clean).
 *
 * Requires MOODLE_WS_TOKEN (seeds the student). The admin drives authoring on ctx.page; the student
 * checks run in a separate browser context logged in as the seeded user.
 */
public class SequentialCompletionSteps {
    private final TestContext ctx;

    // shared fixture (created once, torn down once)
    static String courseId;
    static String activity1Cmid;
    static String activity2Cmid;
    static ApiClient.SeededUser student;
    static BrowserContext studentCtx;
    static Page studentPage;
    static CourseView studentView;

    static final String A1 = "Activity 1";
    static final String A2 = "Activity 2";

    public SequentialCompletionSteps(TestContext ctx) {
        this.ctx = ctx;
    }

    @Given("a course where Activity 1 uses mark-as-done completion and Activity 2 is restricted until Activity 1 is complete")
    public void aSequentialCompletionCourse() {
        if (courseId != null) return;   // fixture already built this run
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set (needed to seed the student)");
        long stamp = System.currentTimeMillis();
        CourseAdmin admin = new CourseAdmin(ctx.page);
        courseId = admin.createCourse("SAC Auto " + stamp, "sacauto" + stamp);
        activity1Cmid = admin.addUrlActivity(courseId, A1, true, null);
        activity2Cmid = admin.addUrlActivity(courseId, A2, false, CourseAdmin.requireCompleteJson(activity1Cmid));
        student = ApiClient.createUser(stamp);
        ApiClient.enrolUser(student.id, Long.parseLong(courseId), 5);   // 5 = student
        // open the course as the student
        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        studentPage = studentCtx.newPage();
        studentPage.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(studentPage, student.username, student.password);
        studentView = new CourseView(studentPage);
        studentView.open(courseId);
        System.out.println("[SAC] fixture course=" + courseId + " a1=" + activity1Cmid
                + " a2=" + activity2Cmid + " student=" + student.username);
    }

    @Then("Activity 2 is not accessible to the student")
    public void activity2NotAccessible() {
        studentView.open(courseId);
        assertTrue(studentView.isRestricted(A2), "Activity 2 should be restricted while Activity 1 is incomplete");
    }

    @Then("Activity 1 is shown as not completed")
    public void activity1NotCompleted() {
        studentView.open(courseId);
        assertFalse(studentView.isCompleted(A1), "Activity 1 should be incomplete before it is marked done");
    }

    @Then("the restriction message names Activity 1")
    public void restrictionMessageNamesActivity1() {
        studentView.open(courseId);
        String msg = studentView.restrictionMessage(A2);
        assertTrue(msg.toLowerCase().contains("activity 1"),
                "restriction message should name Activity 1, was: '" + msg + "'");
    }

    @When("the student marks Activity 1 as done")
    public void studentMarksActivity1Done() {
        studentView.open(courseId);
        studentView.markDone(A1);
    }

    @Then("Activity 1 is shown as completed")
    public void activity1Completed() {
        studentView.open(courseId);
        assertTrue(studentView.isCompleted(A1), "Activity 1 should be completed after being marked done");
    }

    @Then("Activity 2 becomes accessible to the student")
    public void activity2Accessible() {
        studentView.open(courseId);
        assertFalse(studentView.isRestricted(A2), "Activity 2 should be accessible after Activity 1 completion");
    }

    @When("the student logs out and logs in again")
    public void studentReLogsIn() {
        try { studentCtx.close(); } catch (Exception ignored) {}
        studentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        studentPage = studentCtx.newPage();
        studentPage.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(studentPage, student.username, student.password);
        studentView = new CourseView(studentPage);
    }

    @Then("Activity 2 remains accessible to the student")
    public void activity2RemainsAccessible() {
        studentView.open(courseId);
        assertFalse(studentView.isRestricted(A2), "Activity 2 should remain accessible after re-login");
    }

    // ================= grade-based cases (SAC-2..5, SAC-14) =================
    // Each grade scenario builds its OWN fresh fixture (independent, no ordering coupling): a course
    // with Activity 1 = Assignment (grade-based completion) + Activity 2 restricted until A1 complete +
    // a seeded enrolled student. Grade scenarios use a separate static holder so they don't collide with
    // the mark-as-done shared fixture; @AfterAll tears down whichever were created.

    static String gCourseId;
    static String gAssignmentCmid;
    static ApiClient.SeededUser gStudent;
    static BrowserContext gStudentCtx;
    static CourseView gStudentView;

    private void buildGradeFixture(CourseAdmin.GradeCompletion cond, int passMark) {
        if (Settings.WS_TOKEN.isEmpty()) throw new SkipException("MOODLE_WS_TOKEN not set");
        teardownGradeFixture();   // fresh per scenario
        long stamp = System.currentTimeMillis();
        CourseAdmin admin = new CourseAdmin(ctx.page);
        gCourseId = admin.createCourse("SAC Grade " + stamp, "sacg" + stamp);
        gAssignmentCmid = admin.addGradedAssignment(gCourseId, A1, cond, passMark, null);
        admin.addUrlActivity(gCourseId, A2, false, CourseAdmin.requireCompleteJson(gAssignmentCmid));
        gStudent = ApiClient.createUser(stamp);
        ApiClient.enrolUser(gStudent.id, Long.parseLong(gCourseId), 5);
        gStudentCtx = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true));
        Page sp = gStudentCtx.newPage();
        sp.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
        Auth.uiLogin(sp, gStudent.username, gStudent.password);
        gStudentView = new CourseView(sp);
        System.out.println("[SAC-grade] course=" + gCourseId + " a1=" + gAssignmentCmid + " student=" + gStudent.username + " cond=" + cond);
    }

    @Given("a course where Activity 1 requires any grade and Activity 2 is restricted until Activity 1 is complete")
    public void gradeFixtureAnyGrade() {
        buildGradeFixture(CourseAdmin.GradeCompletion.ANY_GRADE, 0);
    }

    @Given("a course where Activity 1 requires a passing grade of {int} and Activity 2 is restricted until Activity 1 is complete")
    public void gradeFixturePassing(int passMark) {
        buildGradeFixture(CourseAdmin.GradeCompletion.PASSING_GRADE, passMark);
    }

    @When("the teacher gives the student a grade of {int} on Activity 1")
    public void teacherGradesActivity1(int grade) {
        new CourseAdmin(ctx.page).gradeAssignment(gAssignmentCmid, gStudent.id, grade);
    }

    @Then("the graded Activity 2 becomes accessible to the student")
    public void gradedActivity2Accessible() {
        gStudentView.open(gCourseId);
        assertFalse(gStudentView.isRestricted(A2), "Activity 2 should be accessible after Activity 1 completion");
    }

    @Then("the graded Activity 2 is not accessible to the student")
    public void gradedActivity2NotAccessible() {
        gStudentView.open(gCourseId);
        assertTrue(gStudentView.isRestricted(A2), "Activity 2 should stay restricted while Activity 1 is incomplete");
    }

    private static void teardownGradeFixture() {
        try { if (gStudentCtx != null) gStudentCtx.close(); } catch (Exception ignored) {}
        gStudentCtx = null; gStudentView = null;
        if (gCourseId != null) {
            try {
                BrowserContext c = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                        .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true)
                        .setStorageStatePath(GlobalHooks.STORAGE_STATE));
                Page p = c.newPage();
                p.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
                new CourseAdmin(p).deleteCourse(gCourseId);
                c.close();
                System.out.println("[SAC-grade] deleted course " + gCourseId);
            } catch (Throwable e) {
                System.out.println("[SAC-grade] course cleanup failed: " + e.getMessage());
            }
            gCourseId = null;
        }
        gAssignmentCmid = null;
        if (gStudent != null) { ApiClient.deleteUser(gStudent.id); gStudent = null; }
    }

    @AfterAll
    public static void tearDownFixture() {
        teardownGradeFixture();
        try { if (studentCtx != null) studentCtx.close(); } catch (Exception ignored) {}
        if (courseId != null) {
            try {
                BrowserContext c = GlobalHooks.browser.newContext(new Browser.NewContextOptions()
                        .setBaseURL(Settings.BASE_URL).setIgnoreHTTPSErrors(true)
                        .setStorageStatePath(GlobalHooks.STORAGE_STATE));
                Page p = c.newPage();
                p.setDefaultTimeout(Settings.DEFAULT_TIMEOUT_MS);
                new CourseAdmin(p).deleteCourse(courseId);
                c.close();
                System.out.println("[SAC] deleted fixture course " + courseId);
            } catch (Throwable e) {
                System.out.println("[SAC] course cleanup failed: " + e.getMessage());
            }
        }
        if (student != null) {
            ApiClient.deleteUser(student.id);
            System.out.println("[SAC] deleted seeded student " + student.username);
        }
        courseId = null; activity1Cmid = null; activity2Cmid = null; student = null;
    }
}
