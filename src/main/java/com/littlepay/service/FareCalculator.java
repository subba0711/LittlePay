package com.littlepay.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates trip fares based on the stops travelled between.
 */
public class FareCalculator {

    private final Map<String, Map<String, BigDecimal>> fares = new HashMap<>();

    /**
     * Initializes the fare calculator with the fares between stops.
     */
    public FareCalculator() {
        addFare("Stop1", "Stop2", "3.25");
        addFare("Stop2", "Stop3", "5.50");
        addFare("Stop1", "Stop3", "7.30");
    }

    /**
     * Returns the fare for a completed trip between two stops.
     * @param fromStopId the stop where the trip started
     * @param toStopId the stop where the trip ended
     * @return the fare for the trip
     * @throws IllegalArgumentException if no fare is defined between the two stops
     */
    public BigDecimal completedFare(String fromStopId, String toStopId) {
        BigDecimal fare = fares.getOrDefault(fromStopId, Map.of()).get(toStopId);
        if (fare == null) {
            throw new IllegalArgumentException("No fare defined between " + fromStopId + " and " + toStopId);
        }
        return fare;
    }

    /**
     * Returns the maximum possible fare from a given stop to any other stop.
     * @param stopId the stop where the trip started
     * @return the maximum fare for a trip starting from the given stop
     * @throws IllegalArgumentException if the stop is unknown
     */
    public BigDecimal maxFareFrom(String stopId) {
        return fares.getOrDefault(stopId, Map.of()).values().stream()
                .max(Comparator.naturalOrder())
                .orElseThrow(() -> new IllegalArgumentException("Unknown stop: " + stopId));
    }

    /**
     * Returns whether a stop has fares defined.
     * @param stopId the stop to check
     * @return true if the stop is known, false otherwise
     */
    public boolean isKnownStop(String stopId) {
        return fares.containsKey(stopId);
    }

    /**
     * Adds a fare between two stops. Fares are bidirectional.
     * @param stopA the first stop
     * @param stopB the second stop
     * @param amount the fare amount as a string
     */
    private void addFare(String stopA, String stopB, String amount) {
        BigDecimal fare = new BigDecimal(amount);
        fares.computeIfAbsent(stopA, k -> new HashMap<>()).put(stopB, fare);
        fares.computeIfAbsent(stopB, k -> new HashMap<>()).put(stopA, fare);
    }
}
