package com.littlepay.service;

import com.littlepay.model.Tap;
import com.littlepay.model.TapType;
import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import com.littlepay.util.Formatters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TripMatcherTest {

    private static final String PAN_A = "5500005555555559";
    private static final String PAN_B = "4111111111111111";

    private final TripMatcher matcher = new TripMatcher(new FareCalculator());

    @Test
    void matchesExampleInput() {
        List<Tap> taps = List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, "Stop1", "Bus37", PAN_A),
                tap(2, "22-01-2023 13:05:00", TapType.OFF, "Stop2", "Bus37", PAN_A),
                tap(3, "22-01-2023 09:20:00", TapType.ON, "Stop3", "Bus36", PAN_B),
                tap(4, "23-01-2023 08:00:00", TapType.ON, "Stop1", "Bus37", PAN_B),
                tap(5, "23-01-2023 08:02:00", TapType.OFF, "Stop1", "Bus37", PAN_B),
                tap(6, "24-01-2023 16:30:00", TapType.OFF, "Stop2", "Bus37", PAN_A));

        List<Trip> trips = matcher.match(taps);

        assertEquals(List.of(
                new Trip(at("22-01-2023 09:20:00"), null, 0, "Stop3", null,
                        new BigDecimal("7.30"), "Company1", "Bus36", PAN_B, TripStatus.INCOMPLETE),
                new Trip(at("22-01-2023 13:00:00"), at("22-01-2023 13:05:00"), 300, "Stop1", "Stop2",
                        new BigDecimal("3.25"), "Company1", "Bus37", PAN_A, TripStatus.COMPLETED),
                new Trip(at("23-01-2023 08:00:00"), at("23-01-2023 08:02:00"), 120, "Stop1", "Stop1",
                        BigDecimal.ZERO, "Company1", "Bus37", PAN_B, TripStatus.CANCELLED)),
                trips);
    }

    @ParameterizedTest
    @CsvSource({
            "Stop1, Stop2, 3.25",
            "Stop2, Stop1, 3.25",
            "Stop2, Stop3, 5.50",
            "Stop3, Stop2, 5.50",
            "Stop1, Stop3, 7.30",
            "Stop3, Stop1, 7.30"
    })
    void completedTripChargesFareInEitherDirection(String from, String to, String expected) {
        Trip trip = single(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, from, "Bus37", PAN_A),
                tap(2, "22-01-2023 13:10:00", TapType.OFF, to, "Bus37", PAN_A))));

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(new BigDecimal(expected), trip.chargeAmount());
        assertEquals(600, trip.durationSecs());
    }

    @ParameterizedTest
    @CsvSource({
            "Stop1, 7.30",
            "Stop2, 5.50",
            "Stop3, 7.30"
    })
    void incompleteTripChargesMaxFareFromStop(String stop, String expected) {
        Trip trip = single(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, stop, "Bus37", PAN_A))));

        assertEquals(TripStatus.INCOMPLETE, trip.status());
        assertEquals(new BigDecimal(expected), trip.chargeAmount());
        assertNull(trip.finished());
        assertNull(trip.toStopId());
        assertEquals(0, trip.durationSecs());
    }

    @Test
    void cancelledTripIsFree() {
        Trip trip = single(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, "Stop2", "Bus37", PAN_A),
                tap(2, "22-01-2023 13:01:00", TapType.OFF, "Stop2", "Bus37", PAN_A))));

        assertEquals(TripStatus.CANCELLED, trip.status());
        assertEquals(BigDecimal.ZERO, trip.chargeAmount());
        assertEquals(60, trip.durationSecs());
    }

    @Test
    void tapsAreMatchedInTimeOrderRegardlessOfInputOrder() {
        Trip trip = single(matcher.match(List.of(
                tap(2, "22-01-2023 13:05:00", TapType.OFF, "Stop3", "Bus37", PAN_A),
                tap(1, "22-01-2023 13:00:00", TapType.ON, "Stop1", "Bus37", PAN_A))));

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(new BigDecimal("7.30"), trip.chargeAmount());
    }

    @Test
    void tapOnComesBeforeTapOffInTheSameSecond() {
        Trip trip = single(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.OFF, "Stop2", "Bus37", PAN_A),
                tap(2, "22-01-2023 13:00:00", TapType.ON, "Stop1", "Bus37", PAN_A))));

        assertEquals(TripStatus.COMPLETED, trip.status());
        assertEquals(new BigDecimal("3.25"), trip.chargeAmount());
        assertEquals(0, trip.durationSecs());
    }

    @Test
    void cardsAreMatchedIndependently() {
        List<Trip> trips = matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, "Stop1", "Bus37", PAN_A),
                tap(2, "22-01-2023 13:01:00", TapType.ON, "Stop2", "Bus37", PAN_B),
                tap(3, "22-01-2023 13:05:00", TapType.OFF, "Stop2", "Bus37", PAN_A),
                tap(4, "22-01-2023 13:06:00", TapType.OFF, "Stop3", "Bus37", PAN_B)));

        assertEquals(2, trips.size());
        assertEquals(PAN_A, trips.get(0).pan());
        assertEquals(new BigDecimal("3.25"), trips.get(0).chargeAmount());
        assertEquals(PAN_B, trips.get(1).pan());
        assertEquals(new BigDecimal("5.50"), trips.get(1).chargeAmount());
    }

    @Test
    void secondTapOnClosesPreviousTripAsIncomplete() {
        List<Trip> trips = matcher.match(List.of(
                tap(1, "22-01-2023 09:00:00", TapType.ON, "Stop2", "Bus37", PAN_A),
                tap(2, "22-01-2023 17:00:00", TapType.ON, "Stop1", "Bus37", PAN_A),
                tap(3, "22-01-2023 17:10:00", TapType.OFF, "Stop3", "Bus37", PAN_A)));

        assertEquals(2, trips.size());
        assertEquals(TripStatus.INCOMPLETE, trips.get(0).status());
        assertEquals(new BigDecimal("5.50"), trips.get(0).chargeAmount());
        assertEquals(TripStatus.COMPLETED, trips.get(1).status());
        assertEquals(new BigDecimal("7.30"), trips.get(1).chargeAmount());
    }

    @Test
    void tapOffWithoutTapOnIsIgnored() {
        assertTrue(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.OFF, "Stop1", "Bus37", PAN_A))).isEmpty());
    }

    @Test
    void tapOffOnDifferentBusLeavesTripIncomplete() {
        Trip trip = single(matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, "Stop1", "Bus37", PAN_A),
                tap(2, "22-01-2023 13:05:00", TapType.OFF, "Stop2", "Bus99", PAN_A))));

        assertEquals(TripStatus.INCOMPLETE, trip.status());
        assertEquals(new BigDecimal("7.30"), trip.chargeAmount());
        assertEquals("Bus37", trip.busId());
    }

    @ParameterizedTest
    @CsvSource({
            "Stop9",
            "''"
    })
    void cancelledTripAtUnknownStopThrows(String stop) {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.ON, stop, "Bus37", PAN_A),
                tap(2, "22-01-2023 13:05:00", TapType.OFF, stop, "Bus37", PAN_A))));

        assertEquals("Tap 1: unknown stop '" + stop + "'", e.getMessage());
    }

    @Test
    void ignoredTapOffAtUnknownStopThrows() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> matcher.match(List.of(
                tap(1, "22-01-2023 13:00:00", TapType.OFF, "Stop9", "Bus37", PAN_A))));

        assertEquals("Tap 1: unknown stop 'Stop9'", e.getMessage());
    }

    @Test
    void noTapsProduceNoTrips() {
        assertTrue(matcher.match(List.of()).isEmpty());
    }

    private static Trip single(List<Trip> trips) {
        assertEquals(1, trips.size());
        return trips.get(0);
    }

    private static Tap tap(long id, String dateTime, TapType type, String stopId, String busId, String pan) {
        return new Tap(id, at(dateTime), type, stopId, "Company1", busId, pan);
    }

    private static LocalDateTime at(String dateTime) {
        return Formatters.parseDateTime(dateTime);
    }
}
