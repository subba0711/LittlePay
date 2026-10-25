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
            Comparator.comparing(Tap::dateTime).thenComparingLong(Tap::id);

    private final FareCalculator fareCalculator;

    public TripMatcher(FareCalculator fareCalculator) {
        this.fareCalculator = fareCalculator;
    }

    public List<Trip> match(List<Tap> taps) {
        Map<String, List<Tap>> tapsByPan = new LinkedHashMap<>();
        for (Tap tap : taps) {
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

    private void matchCardTaps(List<Tap> panTaps, List<Trip> trips) {
        Tap openTapOn = null;
        for (Tap tap : panTaps) {
            if (tap.tapType() == TapType.ON) {
                if (openTapOn != null) {
                    trips.add(incompleteTrip(openTapOn));
                }
                openTapOn = tap;
            } else if (openTapOn != null) {
                if (sameVehicle(openTapOn, tap)) {
                    trips.add(finishedTrip(openTapOn, tap));
                } else {
                    LOG.warn("Tap off {} is on a different bus to tap on {}, recording tap on as INCOMPLETE and ignoring tap off",
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

    private static void logSummary(int tapCount, List<Trip> trips) {
        Map<TripStatus, Long> countByStatus = new EnumMap<>(TripStatus.class);
        for (TripStatus status : TripStatus.values()) {
            countByStatus.put(status, trips.stream().filter(t -> t.status() == status).count());
        }
        LOG.info("Matched {} taps into {} trips {}", tapCount, trips.size(), countByStatus);
    }

    private static boolean sameVehicle(Tap tapOn, Tap tapOff) {
        return tapOn.companyId().equals(tapOff.companyId()) && tapOn.busId().equals(tapOff.busId());
    }
}
