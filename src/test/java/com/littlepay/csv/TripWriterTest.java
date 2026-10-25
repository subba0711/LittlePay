package com.littlepay.csv;

import com.littlepay.model.Trip;
import com.littlepay.model.TripStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripWriterTest {

    private static final String PAN_A = "5500005555555559";
    private static final String PAN_B = "4111111111111111";

    private final TripWriter writer = new TripWriter();

    @Test
    void writesHeaderAndAllTripStatuses() throws IOException {
        List<Trip> trips = List.of(
                new Trip(LocalDateTime.of(2023, 1, 22, 13, 0, 0), LocalDateTime.of(2023, 1, 22, 13, 5, 0),
                        300, "Stop1", "Stop2", new BigDecimal("3.25"), "Company1", "Bus37", PAN_A,
                        TripStatus.COMPLETED),
                new Trip(LocalDateTime.of(2023, 1, 22, 9, 20, 0), null,
                        0, "Stop3", null, new BigDecimal("7.30"), "Company1", "Bus36", PAN_B,
                        TripStatus.INCOMPLETE),
                new Trip(LocalDateTime.of(2023, 1, 23, 8, 0, 0), LocalDateTime.of(2023, 1, 23, 8, 2, 0),
                        120, "Stop1", "Stop1", BigDecimal.ZERO, "Company1", "Bus37", PAN_B,
                        TripStatus.CANCELLED));

        assertEquals(List.of(
                        TripWriter.HEADER,
                        "22-01-2023 13:00:00, 22-01-2023 13:05:00, 300, Stop1, Stop2, $3.25, Company1, Bus37, "
                                + PAN_A + ", COMPLETED",
                        "22-01-2023 09:20:00, , 0, Stop3, , $7.30, Company1, Bus36, "
                                + PAN_B + ", INCOMPLETE",
                        "23-01-2023 08:00:00, 23-01-2023 08:02:00, 120, Stop1, Stop1, $0.00, Company1, Bus37, "
                                + PAN_B + ", CANCELLED"),
                writeToLines(trips));
    }

    @Test
    void noTripsWritesHeaderOnly() throws IOException {
        assertEquals(List.of(TripWriter.HEADER), writeToLines(List.of()));
    }

    @Test
    void writesToFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("trips.csv");
        writer.write(List.of(new Trip(LocalDateTime.of(2023, 1, 22, 13, 0, 0), LocalDateTime.of(2023, 1, 22, 13, 5, 0),
                300, "Stop1", "Stop2", new BigDecimal("3.25"), "Company1", "Bus37", PAN_A,
                TripStatus.COMPLETED)), file);

        assertEquals(List.of(
                        TripWriter.HEADER,
                        "22-01-2023 13:00:00, 22-01-2023 13:05:00, 300, Stop1, Stop2, $3.25, Company1, Bus37, "
                                + PAN_A + ", COMPLETED"),
                Files.readAllLines(file));
    }

    private List<String> writeToLines(List<Trip> trips) throws IOException {
        StringWriter out = new StringWriter();
        writer.write(trips, out);
        return out.toString().lines().toList();
    }
}
