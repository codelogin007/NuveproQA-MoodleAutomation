package com.nuvepro.moodle.steps;

import io.cucumber.java.After;
import io.cucumber.java.Before;

/** Scenario lifecycle: launch a fresh browser/page before each scenario, close it after. */
public class Hooks {
    private final TestContext ctx;

    public Hooks(TestContext ctx) {
        this.ctx = ctx;
    }

    @Before
    public void setUp() {
        ctx.start();
    }

    @After
    public void tearDown() {
        ctx.stop();
    }
}
