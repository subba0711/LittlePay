package com.littlepay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TripProcessorApplicationTest {

    @Test
    void processesExampleInputEndToEnd(@TempDir Path dir) throws IOException {
        Path input = dir.resolve("taps.csv");
        Path output = dir.resolve("trips.csv");
        Files.write(input, List.of(
                "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN",
                "1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559",
                "2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559",
                "3, 22-01-2023 09:20:00, ON, Stop3, Company1, Bus36, 4111111111111111",
                "4, 23-01-2023 08:00:00, ON, Stop1, Company1, Bus37, 4111111111111111",
                "5, 23-01-2023 08:02:00, OFF, Stop1, Company1, Bus37, 4111111111111111",
                "6, 24-01-2023 16:30:00, OFF, Stop2, Company1, Bus37, 5500005555555559"));

        int tripCount = TripProcessorApplication.run(input, output);

        assertEquals(3, tripCount);
        assertEquals(List.of(
                        "Started, Finished, DurationSecs, FromStopId, ToStopId, ChargeAmount, CompanyId, BusID, PAN, Status",
                        "22-01-2023 09:20:00, , 0, Stop3, , $7.30, Company1, Bus36, 4111111111111111, INCOMPLETE",
                        "22-01-2023 13:00:00, 22-01-2023 13:05:00, 300, Stop1, Stop2, $3.25, Company1, Bus37, 5500005555555559, COMPLETED",
                        "23-01-2023 08:00:00, 23-01-2023 08:02:00, 120, Stop1, Stop1, $0.00, Company1, Bus37, 4111111111111111, CANCELLED"),
                Files.readAllLines(output));
    }
}
