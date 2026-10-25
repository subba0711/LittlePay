package com.littlepay;

import com.littlepay.csv.TapReader;
import com.littlepay.csv.TripWriter;
import com.littlepay.model.Tap;
import com.littlepay.model.Trip;
import com.littlepay.service.FareCalculator;
import com.littlepay.service.TripMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/*
 * Entry point that reads the command-line arguments and runs the tap-to-trip processing.
 */
public class TripProcessorApplication {

    private static final Logger LOG = LoggerFactory.getLogger(TripProcessorApplication.class);

    private static final String DEFAULT_INPUT = "taps.csv";
    private static final String DEFAULT_OUTPUT = "trips.csv";

    public static void main(String[] args) {
        if (args.length > 2) {
            LOG.error("Usage: java -jar littlepay.jar [input taps.csv] [output trips.csv]");
            System.exit(1);
        }
        Path input = Path.of(args.length > 0 ? args[0] : DEFAULT_INPUT);
        Path output = Path.of(args.length > 1 ? args[1] : DEFAULT_OUTPUT);

        try {
            LOG.info("Reading taps from {}", input);
            int tripCount = run(input, output);
            LOG.info("Wrote {} trips to {}", tripCount, output);
        } catch (IOException e) {
            LOG.error("Failed to process taps", e);
            System.exit(1);
        }
    }

    static int run(Path input, Path output) throws IOException {
        List<Tap> taps = new TapReader().read(input);
        List<Trip> trips = new TripMatcher(new FareCalculator()).match(taps);
        new TripWriter().write(trips, output);
        return trips.size();
    }
}
