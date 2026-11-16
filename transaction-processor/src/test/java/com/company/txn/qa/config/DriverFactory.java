package com.company.txn.qa.config;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;

public final class DriverFactory {
    private DriverFactory() {}
    public static WebDriver create() {
        String browser = FrameworkConfig.browser().toLowerCase();
        if ("firefox".equals(browser)) {
            FirefoxOptions options = new FirefoxOptions();
            if (FrameworkConfig.headless()) options.addArguments("-headless");
            return new FirefoxDriver(options);
        }
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1000", "--disable-dev-shm-usage");
        if (FrameworkConfig.headless()) options.addArguments("--headless=new", "--no-sandbox");
        return new ChromeDriver(options); // Selenium Manager resolves driver when supported.
    }
}
