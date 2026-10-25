package com.littlepay.csv;

import com.littlepay.model.Tap;
import com.littlepay.model.TapType;
import com.littlepay.util.Formatters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and parses tap events from the taps.csv input file.
 */
public class TapReader {

    private static final Logger LOG = LoggerFactory.getLogger(TapReader.class);

    private static final int COLUMN_COUNT = 7;

    public List<Tap> read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<Tap> taps = read(reader);
            LOG.info("Read {} taps from {}", taps.size(), path);
            return taps;
        }
    }

    public List<Tap> read(Reader reader) throws IOException {
        BufferedReader lines = new BufferedReader(reader);
        List<Tap> taps = new ArrayList<>();
        if (lines.readLine() == null) {
            LOG.warn("Input is empty, expected a header row");
            return taps;
        }
        String line;
        int lineNumber = 1;
        while ((line = lines.readLine()) != null) {
            lineNumber++;
            if (!line.isBlank()) {
                taps.add(parse(line, lineNumber));
            }
        }
        return taps;
    }

    private static Tap parse(String line, int lineNumber) {
        String[] fields = line.split(",");
        if (fields.length != COLUMN_COUNT) {
            throw new IllegalArgumentException(
                    "Line " + lineNumber + ": expected " + COLUMN_COUNT + " columns but found " + fields.length);
        }
        try {
            return new Tap(
                    Long.parseLong(fields[0].trim()),
                    Formatters.parseDateTime(fields[1]),
                    TapType.valueOf(fields[2].trim()),
                    fields[3].trim(),
                    fields[4].trim(),
                    fields[5].trim(),
                    fields[6].trim());
        } catch (IllegalArgumentException | DateTimeException e) {
            throw new IllegalArgumentException("Line " + lineNumber + ": " + e.getMessage(), e);
        }
    }
}
