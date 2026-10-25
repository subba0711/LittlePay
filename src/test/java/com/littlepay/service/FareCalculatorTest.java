package com.littlepay.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FareCalculatorTest {

    private final FareCalculator calculator = new FareCalculator();

    @ParameterizedTest
    @CsvSource({
            "Stop1, Stop2, 3.25",
            "Stop2, Stop1, 3.25",
            "Stop2, Stop3, 5.50",
            "Stop3, Stop2, 5.50",
            "Stop1, Stop3, 7.30",
            "Stop3, Stop1, 7.30"
    })
    void completedFareIsSameInBothDirections(String from, String to, String expected) {
        assertEquals(new BigDecimal(expected), calculator.completedFare(from, to));
    }

    @ParameterizedTest
    @CsvSource({
            "Stop1, 7.30",
            "Stop2, 5.50",
            "Stop3, 7.30"
    })
    void maxFareFromStopIsHighestPossibleFare(String stop, String expected) {
        assertEquals(new BigDecimal(expected), calculator.maxFareFrom(stop));
    }

    @Test
    void unknownStopThrows() {
        assertThrows(IllegalArgumentException.class, () -> calculator.maxFareFrom("Stop9"));
        assertThrows(IllegalArgumentException.class, () -> calculator.completedFare("Stop1", "Stop9"));
    }
}
