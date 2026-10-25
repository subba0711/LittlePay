package com.littlepay.csv;

import com.littlepay.model.Trip;
import com.littlepay.util.Formatters;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes processed trips to the trips.csv output file.
 */
public class TripWriter {

    static final String HEADER =
            "Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status";

    public void write(List<Trip> trips, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(trips, writer);
        }
    }

    public void write(List<Trip> trips, Writer writer) throws IOException {
        BufferedWriter out = new BufferedWriter(writer);
        out.write(HEADER);
        out.newLine();
        for (Trip trip : trips) {
            out.write(format(trip));
            out.newLine();
        }
        out.flush();
    }

    // Incomplete trips have no tap off, so Finished and ToStopId are left empty.
    private static String format(Trip trip) {
        return String.join(", ",
                Formatters.formatDateTime(trip.started()),
                trip.finished() == null ? "" : Formatters.formatDateTime(trip.finished()),
                String.valueOf(trip.durationSecs()),
                trip.fromStopId(),
                trip.toStopId() == null ? "" : trip.toStopId(),
                Formatters.formatMoney(trip.chargeAmount()),
                trip.companyId(),
                trip.busId(),
                trip.pan(),
                trip.status().name());
    }
}
