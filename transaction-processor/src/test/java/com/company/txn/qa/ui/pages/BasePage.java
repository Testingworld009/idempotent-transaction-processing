package com.company.txn.qa.ui.pages;

import com.company.txn.qa.utility.WaitUtils;
import org.openqa.selenium.*;

public abstract class BasePage {
    protected final WebDriver driver;
    protected final WaitUtils waits;
    protected BasePage(WebDriver driver) { this.driver = driver; this.waits = new WaitUtils(driver); }
    public String title() { return driver.getTitle(); }
    public String currentUrl() { return driver.getCurrentUrl(); }
    protected void click(By locator) { waits.clickable(locator).click(); }
    protected void type(By locator, String value) { WebElement e = waits.visible(locator); e.clear(); e.sendKeys(value); }
    protected String text(By locator) { return waits.visible(locator).getText(); }
}
