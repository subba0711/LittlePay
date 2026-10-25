package com.littlepay.csv;

import com.littlepay.model.Trip;
import com.littlepay.util.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(TripWriter.class);

    static final String HEADER =
            "Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status";

    /**
     * Writes the list of trips to the specified output file in CSV format.
     * The first line is a header row, followed by one line per trip.
     *
     * @param trips the list of trips to write
     * @param path  the path to the output file
     * @throws IOException if an I/O error occurs writing to the file
     */
    public void write(List<Trip> trips, Path path) throws IOException {
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            write(trips, writer);
        }
        LOG.info("Wrote {} trips to {}", trips.size(), path);
    }

    /**
     * Writes the list of trips to the specified Writer in CSV format.
     * The first line is a header row, followed by one line per trip.
     *
     * @param trips  the list of trips to write
     * @param writer the Writer to write to
     * @throws IOException if an I/O error occurs writing to the Writer
     */
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
