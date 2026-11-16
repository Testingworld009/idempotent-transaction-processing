package com.company.txn.ui;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

/** Optional Selenium setup for a separately developed browser dashboard. The assessment has no UI. */
public final class SeleniumDriverFactory {
 private SeleniumDriverFactory() { }
 public static WebDriver createChromeDriver() {
  ChromeOptions options = new ChromeOptions();
  options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1440,1000");
  return new ChromeDriver(options); // Selenium Manager resolves a compatible driver.
 }
}
