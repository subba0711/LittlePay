package com.littlepay.csv;

import com.littlepay.model.Tap;
import com.littlepay.model.TapType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TapReaderTest {

    private static final String HEADER = "ID, DateTimeUTC, TapType, StopId, CompanyId, BusID, PAN\n";

    private final TapReader reader = new TapReader();

    @Test
    void readsTapsSkippingHeader() throws IOException {
        List<Tap> taps = reader.read(new StringReader(HEADER
                + "1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559\n"
                + "2, 22-01-2023 13:05:00, OFF, Stop2, Company1, Bus37, 5500005555555559\n"));

        assertEquals(List.of(
                new Tap(1, LocalDateTime.of(2023, 1, 22, 13, 0, 0), TapType.ON,
                        "Stop1", "Company1", "Bus37", "5500005555555559"),
                new Tap(2, LocalDateTime.of(2023, 1, 22, 13, 5, 0), TapType.OFF,
                        "Stop2", "Company1", "Bus37", "5500005555555559")),
                taps);
    }

    @Test
    void ignoresBlankLines() throws IOException {
        List<Tap> taps = reader.read(new StringReader(HEADER
                + "\n"
                + "1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559\n"
                + "   \n"));

        assertEquals(1, taps.size());
    }

    @Test
    void headerOnlyReturnsNoTaps() throws IOException {
        assertTrue(reader.read(new StringReader(HEADER)).isEmpty());
    }

    @Test
    void emptyInputReturnsNoTaps() throws IOException {
        assertTrue(reader.read(new StringReader("")).isEmpty());
    }

    @Test
    void wrongColumnCountThrowsWithLineNumber() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> reader.read(new StringReader(HEADER
                        + "1, 22-01-2023 13:00:00, ON, Stop1, Company1, Bus37, 5500005555555559\n"
                        + "2, 22-01-2023 13:05:00, OFF, Stop2, Company1\n")));

        assertTrue(e.getMessage().startsWith("Line 3:"), e.getMessage());
    }

    @Test
    void readsFromFile(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("taps.csv");
        Files.writeString(file, HEADER
                + "3, 22-01-2023 09:20:00, ON, Stop3, Company1, Bus36, 4111111111111111\n");

        assertEquals(List.of(new Tap(3, LocalDateTime.of(2023, 1, 22, 9, 20, 0), TapType.ON,
                        "Stop3", "Company1", "Bus36", "4111111111111111")),
                reader.read(file));
    }
}
