package com.company.txn.ui;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;

/**
 * Example only. Enable after a real dashboard exists and set DASHBOARD_URL.
 * Selenium is intentionally not used to test the background processor or REST API.
 */
@Disabled("No browser UI is included in the supplied assessment requirements")
class TransactionDashboardSeleniumExample {
 @Test void dashboardDisplaysTransactionTable() {
  String url = System.getenv("DASHBOARD_URL");
  if (url == null || url.isBlank()) throw new IllegalStateException("Set DASHBOARD_URL when a dashboard is available");
  WebDriver driver = SeleniumDriverFactory.createChromeDriver();
  try {
   driver.get(url);
   new WebDriverWait(driver, Duration.ofSeconds(10)).until(
       ExpectedConditions.visibilityOfElementLocated(By.cssSelector("[data-testid='transactions-table']")));
  } finally { driver.quit(); }
 }
}
