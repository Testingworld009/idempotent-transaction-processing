package com.company.txn.qa.utility;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class LogUtils {
    private static final Path LOG = Paths.get("target", "logs", "qa-execution.log");
    private LogUtils() {}
    public static synchronized void log(String level, String message) {
        try {
            Files.createDirectories(LOG.getParent());
            String line = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    + " [" + level + "] " + message + System.lineSeparator();
            Files.writeString(LOG, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write QA execution log", e);
        }
    }
}
