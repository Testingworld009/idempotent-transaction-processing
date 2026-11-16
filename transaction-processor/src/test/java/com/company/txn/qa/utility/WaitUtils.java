package com.company.txn.qa.utility;

import com.company.txn.qa.config.FrameworkConfig;
import java.time.Duration;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public class WaitUtils {
    private final WebDriverWait wait;
    public WaitUtils(WebDriver driver) {
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(FrameworkConfig.explicitWaitSeconds()));
    }
    public WebElement visible(By locator) { return wait.until(ExpectedConditions.visibilityOfElementLocated(locator)); }
    public WebElement clickable(By locator) { return wait.until(ExpectedConditions.elementToBeClickable(locator)); }
    public boolean invisible(By locator) { return wait.until(ExpectedConditions.invisibilityOfElementLocated(locator)); }
    public void waitForUrlContains(String value) { wait.until(ExpectedConditions.urlContains(value)); }
}
