package com.nuvepro.moodle;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * TestNG entry point for the Cucumber/BDD scenarios. Discovered by testng-fast.xml
 * (package scan). Runs the Gherkin scenarios in src/test/resources/features using the
 * step definitions in com.nuvepro.moodle.steps.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "com.nuvepro.moodle.steps",
        plugin = {"pretty", "html:target/cucumber-report.html", "json:target/cucumber.json"}
)
public class RunCucumberTest extends AbstractTestNGCucumberTests {
}
