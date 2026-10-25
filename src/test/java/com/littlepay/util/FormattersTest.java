package com.littlepay.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormattersTest {

    @Test
    void parsesDateTime() {
        assertEquals(LocalDateTime.of(2023, 1, 22, 13, 0, 0),
                Formatters.parseDateTime("22-01-2023 13:00:00"));
    }

    @Test
    void parsesDateTimeWithSurroundingSpaces() {
        assertEquals(LocalDateTime.of(2023, 1, 22, 13, 5, 0),
                Formatters.parseDateTime(" 22-01-2023 13:05:00 "));
    }

    @Test
    void formatsDateTime() {
        assertEquals("05-02-2023 09:07:03",
                Formatters.formatDateTime(LocalDateTime.of(2023, 2, 5, 9, 7, 3)));
    }

    @Test
    void invalidDateTimeThrows() {
        assertThrows(DateTimeParseException.class, () -> Formatters.parseDateTime("2023-01-22 13:00:00"));
    }

    @ParameterizedTest
    @CsvSource({
            "3.25, $3.25",
            "7.3, $7.30",
            "0, $0.00",
            "5.555, $5.56"
    })
    void formatsMoney(String amount, String expected) {
        assertEquals(expected, Formatters.formatMoney(new BigDecimal(amount)));
    }

}
