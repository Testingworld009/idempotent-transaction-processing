package com.company.txn.qa.config;

public final class FrameworkConfig {
    private FrameworkConfig() {}
    public static String apiBaseUrl() { return env("API_BASE_URL", "http://localhost:8080"); }
    public static String dashboardUrl() { return env("DASHBOARD_URL", ""); }
    public static String browser() { return env("BROWSER", "chrome"); }
    public static boolean headless() { return Boolean.parseBoolean(env("HEADLESS", "true")); }
    public static int explicitWaitSeconds() { return Integer.parseInt(env("EXPLICIT_WAIT_SECONDS", "10")); }
    public static String dbUrl() { return env("DB_URL", ""); }
    public static String dbUser() { return env("DB_USER", ""); }
    public static String dbPassword() { return env("DB_PASSWORD", ""); }
    public static String dbDriver() { return env("DB_DRIVER", "org.postgresql.Driver"); }
    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
