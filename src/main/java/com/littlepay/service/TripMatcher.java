package com.littlepay.service;

import com.littlepay.model.Tap;
import com.littlepay.model.TapType;
import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matches tap-on and tap-off events to create trips using the core business logic.
 */
public class TripMatcher {

    private static final Logger LOG = LoggerFactory.getLogger(TripMatcher.class);

    private static final Comparator<Tap> TAP_ORDER =
            Comparator.comparing(Tap::dateTime).thenComparing(Tap::tapType).thenComparingLong(Tap::id);

    private final FareCalculator fareCalculator;

    public TripMatcher(FareCalculator fareCalculator) {
        this.fareCalculator = fareCalculator;
    }

    /**
     * Matches tap-on and tap-off events into trips, grouped by card (PAN).
     * A repeated tap (same ID and data) is ignored with a warning.
     * Taps are sorted by date-time, then ON before OFF in the same second, then by ID.
     * Trips are sorted by start time before returning.
     * @throws IllegalArgumentException if two taps share an ID but differ, or a tap is at an unknown stop
     */
    public List<Trip> match(List<Tap> taps) {
        Map<Long, Tap> tapsById = new LinkedHashMap<>();
        for (Tap tap : taps) {
            Tap existing = tapsById.putIfAbsent(tap.id(), tap);
            if (existing == null) {
                continue;
            }
            if (!existing.equals(tap)) {
                throw new IllegalArgumentException("Tap " + tap.id() + ": duplicate ID with different data");
            }
            LOG.warn("Ignoring repeated tap {}", tap.id());
        }

        Map<String, List<Tap>> tapsByPan = new LinkedHashMap<>();
        for (Tap tap : tapsById.values()) {
            tapsByPan.computeIfAbsent(tap.pan(), k -> new ArrayList<>()).add(tap);
        }

        List<Trip> trips = new ArrayList<>();
        for (List<Tap> panTaps : tapsByPan.values()) {
            panTaps.sort(TAP_ORDER);
            matchCardTaps(panTaps, trips);
        }
        trips.sort(Comparator.comparing(Trip::started));
        logSummary(taps.size(), trips);
        return trips;
    }

    /**
     * Matches tap-on and tap-off events for a single card (PAN) into trips.
     * If a tap-on has no matching tap-off, it is recorded as an incomplete trip.
     * If a tap-off has no matching tap-on, it is ignored with a warning.
     * If a tap-off is on a different vehicle than the tap-on, the tap-on is recorded as incomplete and the tap-off is ignored.
     */
    private void matchCardTaps(List<Tap> panTaps, List<Trip> trips) {
        Tap openTapOn = null;
        for (Tap tap : panTaps) {
            if (!fareCalculator.isKnownStop(tap.stopId())) {
                throw new IllegalArgumentException("Tap " + tap.id() + ": unknown stop '" + tap.stopId() + "'");
            }
            if (tap.tapType() == TapType.ON) {
                if (openTapOn != null) {
                    trips.add(incompleteTrip(openTapOn));
                }
                openTapOn = tap;
            } else if (openTapOn != null) {
                if (sameVehicle(openTapOn, tap)) {
                    trips.add(finishedTrip(openTapOn, tap));
                } else {
                    LOG.warn("Tap off {} is on a different company or bus to tap on {}, recording tap on as INCOMPLETE and ignoring tap off",
                            tap.id(), openTapOn.id());
                    trips.add(incompleteTrip(openTapOn));
                }
                openTapOn = null;
            } else {
                LOG.warn("Ignoring tap off {} with no matching tap on", tap.id());
            }
        }
        if (openTapOn != null) {
            trips.add(incompleteTrip(openTapOn));
        }
    }

    /**
     * Creates a trip from a tap-on and tap-off event, calculating the fare and duration.
     * If the trip is cancelled (tap-on and tap-off at the same stop), the fare is zero.
     */
    private Trip finishedTrip(Tap tapOn, Tap tapOff) {
        boolean cancelled = tapOn.stopId().equals(tapOff.stopId());
        BigDecimal charge = cancelled
                ? BigDecimal.ZERO
                : fareCalculator.completedFare(tapOn.stopId(), tapOff.stopId());
        return new Trip(
                tapOn.dateTime(),
                tapOff.dateTime(),
                Duration.between(tapOn.dateTime(), tapOff.dateTime()).getSeconds(),
                tapOn.stopId(),
                tapOff.stopId(),
                charge,
                tapOn.companyId(),
                tapOn.busId(),
                tapOn.pan(),
                cancelled ? TripStatus.CANCELLED : TripStatus.COMPLETED);
    }

    /**
     * Creates an incomplete trip from a tap-on event, using the maximum fare from the tap-on stop.
     */
    private Trip incompleteTrip(Tap tapOn) {
        return new Trip(
                tapOn.dateTime(),
                null,
                0,
                tapOn.stopId(),
                null,
                fareCalculator.maxFareFrom(tapOn.stopId()),
                tapOn.companyId(),
                tapOn.busId(),
                tapOn.pan(),
                TripStatus.INCOMPLETE);
    }

    /**
     * Logs a summary of the number of taps and trips processed, along with a breakdown of trip statuses.
     */
    private static void logSummary(int tapCount, List<Trip> trips) {
        Map<TripStatus, Long> countByStatus = new EnumMap<>(TripStatus.class);
        for (TripStatus status : TripStatus.values()) {
            countByStatus.put(status, trips.stream().filter(t -> t.status() == status).count());
        }
        LOG.info("Matched {} taps into {} trips {}", tapCount, trips.size(), countByStatus);
    }

    /**
     * Checks if two taps are on the same vehicle (same company and bus).
     */
    private static boolean sameVehicle(Tap tapOn, Tap tapOff) {
        return tapOn.companyId().equals(tapOff.companyId()) && tapOn.busId().equals(tapOff.busId());
    }
}
