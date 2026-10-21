package com.leelo.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Conversions between the instants of the application and the text values
 * stored in the database.
 *
 * <p>Values are written in ISO-8601 so that they sort chronologically, while
 * the reader also accepts the "YYYY-MM-DD HH:MM:SS" format produced by
 * SQLite.
 */
public final class Timestamps {

    private static final DateTimeFormatter SQLITE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Timestamps() {
    }

    /** The current instant, formatted for storage. */
    public static String now() {
        return format(Instant.now());
    }

    /** Formats an instant for storage. */
    public static String format(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    /** Parses a stored timestamp, returning {@code null} when it cannot be read. */
    public static Instant parse(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return Instant.parse(trimmed);
        } catch (RuntimeException ignored) {
            // Fall through to the SQLite format.
        }
        try {
            return LocalDateTime.parse(trimmed, SQLITE_FORMAT).toInstant(ZoneOffset.UTC);
        } catch (RuntimeException ignored) {
            return null;
        }
    }
}
