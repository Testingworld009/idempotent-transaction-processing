# Optional Selenium UI automation

The supplied case study has no browser UI. Selenium is therefore not the correct tool for testing transaction processing itself. Use Rest Assured for HTTP/API behaviour and JUnit/Mockito for service logic.

A ChromeDriver factory and a disabled example dashboard smoke test are included under `src/test/java/com/company/txn/ui`. Once a dashboard is added, provide stable `data-testid` selectors and set `DASHBOARD_URL` before enabling the test.
