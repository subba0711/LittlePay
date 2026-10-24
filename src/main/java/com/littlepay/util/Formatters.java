package com.littlepay.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Shared date-time and money formatting used for reading taps and writing trips.
 */
public final class Formatters {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    private Formatters() {
    }

    public static LocalDateTime parseDateTime(String value) {
        return LocalDateTime.parse(value.trim(), DATE_TIME);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        return DATE_TIME.format(dateTime);
    }

    public static String formatMoney(BigDecimal amount) {
        return "$" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
