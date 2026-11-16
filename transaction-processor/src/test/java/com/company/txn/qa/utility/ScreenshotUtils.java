package com.company.txn.qa.utility;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.openqa.selenium.*;

public final class ScreenshotUtils {
    private ScreenshotUtils() {}
    public static Path capture(WebDriver driver, String testName) {
        if (driver == null) return null;
        try {
            Path dir = Paths.get("target", "screenshots");
            Files.createDirectories(dir);
            String safe = testName.replaceAll("[^A-Za-z0-9._-]", "_");
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"));
            Path target = dir.resolve(safe + "_" + stamp + ".png");
            Files.copy(((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE), target,
                    StandardCopyOption.REPLACE_EXISTING);
            LogUtils.log("INFO", "Screenshot saved: " + target);
            return target;
        } catch (IOException | WebDriverException e) {
            LogUtils.log("ERROR", "Screenshot capture failed for " + testName + ": " + e.getMessage());
            return null;
        }
    }
}
