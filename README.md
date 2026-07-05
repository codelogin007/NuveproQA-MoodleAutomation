# Nuvepro Moodle — UI Automation (Playwright-Java + TestNG + Cucumber)

BDD UI regression automation for the Nuvepro Moodle LMS. Tests run against a **deployed
Moodle instance** (a URL); test-data preconditions use Moodle/plugin **APIs** where possible,
and behaviour is driven through the **UI** with Playwright.

## Prerequisites
- **JDK 17** (already installed; `JAVA_HOME` set).
- **Maven is NOT required** — use the bundled Maven Wrapper (`mvnw`). First run downloads Maven once.
- On first setup: `mvnw exec` will also fetch the Playwright browser (Chromium) if not present.

## Configure
Copy `.env.example` to `.env` and fill in (the `.env` is git-ignored):
- `MOODLE_BASE_URL`, admin/student credentials, `MOODLE_WS_TOKEN`
- `COURSE_ID` (course used by activity-creation tests, e.g. from the course URL `?id=72`)
- `PLAYGROUND_CMID` (an existing sandbox activity), optional `GUIDED_CMID` / `ASSESSMENT_CMID`

> Note: quote values that contain `#` (e.g. passwords) in `.env`, and keep the file BOM-free.

## Run (from this folder: `automation-java`)

| Goal | Command (Command Prompt) |
|---|---|
| Fast suite (hidden) — connectivity + non-provisioning scenarios | `mvnw test` |
| Provisioning suite — spins a REAL cloud lab | `mvnw test -Pslow` |
| **Watch** one scenario, slowed, browser visible | `mvnw test -Pwatch -Dheadless=false -Dslowmo=500 -Dcucumber.tags="@PG-1"` |
| Run only one scenario (hidden) | `mvnw test -Pwatch -Dcucumber.tags="@PG-8"` |

PowerShell: prefix with `.\mvnw` and quote the arg: `"-Dcucumber.tags=@PG-1"`.

**Flags**
- `-Dheadless=false` — show the browser (default is headless).
- `-Dslowmo=<ms>` — delay each action so a headed run is easy to follow.
- `-Dcucumber.tags="..."` — scope scenarios by tag: a case (`@PG-1`), a group (`@smoke`),
  provisioning (`@provisioning`), or an expression (`@PG-1 or @PG-8`).
- `-Pwatch` — run ONLY the tagged scenario(s), skipping the plain ConnectivityTest logins.

## Reports (regenerated each run)
- **Cucumber (readable, per scenario):** `target/cucumber-report.html`
- **TestNG/Surefire summary:** `target/surefire-reports/emailable-report.html`
- **Traceability & execution** (manual case → scenario → result, tied to the test-case
  spreadsheet): `reports/traceability_execution.html` / `.csv`
  Generate after runs: `python tools/traceability.py` (merges `target/cucumber_fast.json`
  + `cucumber_slow.json`; save each run's `target/cucumber.json` to those names).
- **Screenshots:** `reports/*.png`

## Layout
```
src/main/java/com/nuvepro/moodle/
  config/Settings.java        env/.env + -D overrides (headless, slowmo, ...)
  helpers/Auth.java           UI login
  pages/                      Page Objects (BasePage, PlaygroundLanding, PlaygroundControlPanel, ManageLabs)
src/test/java/com/nuvepro/moodle/
  BaseTest.java               TestNG Playwright lifecycle (used by ConnectivityTest)
  ConnectivityTest.java       login smoke (plain TestNG)
  RunCucumberTest.java        Cucumber runner (emits target/cucumber.json)
  steps/                      GlobalHooks (login-once/storageState + lab safety-net),
                              Hooks, TestContext (PicoContainer), PlaygroundSteps
src/test/resources/features/  Gherkin .feature files (one @PG-n tag per manual case)
testng-fast.xml / -slow.xml / -watch.xml   suite definitions
```

## How the suite is organised
- **All-BDD:** every case is a tagged Cucumber scenario; `@provisioning` scenarios spin a real
  cloud lab and share ONE lab per run (PG-12 provisions, PG-15 stops, `@AfterAll` is the safety net).
- **Login once:** admin logs in in `@BeforeAll`, saved as Playwright `storageState`; every scenario
  starts pre-authenticated (no per-scenario login).
