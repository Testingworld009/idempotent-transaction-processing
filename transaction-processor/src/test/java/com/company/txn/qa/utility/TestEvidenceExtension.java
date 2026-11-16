package com.company.txn.qa.utility;

import org.junit.jupiter.api.extension.*;
import org.openqa.selenium.WebDriver;

/** Logs every test outcome and captures a browser screenshot immediately when a UI test throws. */
public class TestEvidenceExtension implements TestWatcher, TestExecutionExceptionHandler, AfterEachCallback {
    @Override public void testSuccessful(ExtensionContext context) {
        LogUtils.log("PASS", context.getDisplayName());
    }
    @Override public void testFailed(ExtensionContext context, Throwable cause) {
        LogUtils.log("FAIL", context.getDisplayName() + " :: " + cause);
    }
    @Override public void testAborted(ExtensionContext context, Throwable cause) {
        LogUtils.log("SKIP", context.getDisplayName() + " :: " + cause.getMessage());
    }
    @Override public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        LogUtils.log("FAIL", context.getDisplayName() + " :: " + throwable);
        WebDriver driver = DriverManager.getDriver();
        if (driver != null && context.getTestClass().map(c -> c.getName().contains(".ui.")).orElse(false)) {
            ScreenshotUtils.capture(driver, context.getDisplayName());
        }
        throw throwable;
    }
    @Override public void afterEach(ExtensionContext context) {
        if (context.getTestClass().map(c -> c.getName().contains(".ui.")).orElse(false)) {
            DriverManager.quitDriver();
        }
    }
}
