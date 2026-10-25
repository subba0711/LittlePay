# LittlePay Trip Calculator

Reads bus tap events from `taps.csv`, matches them into trips, and writes the trips with their fares to `trips.csv`.

| Trip       | Fare  |
|------------|-------|
| Stop1 ↔ Stop2 | $3.25 |
| Stop2 ↔ Stop3 | $5.50 |
| Stop1 ↔ Stop3 | $7.30 |

- **COMPLETED**: tap on and tap off at different stops. Charged the fare between them.
- **INCOMPLETE**: tap on with no tap off. Charged the highest fare from the tap-on stop.
- **CANCELLED**: tap on and tap off at the same stop. Charged $0.00.

## Requirements

- Java 21
- Maven

## Build and run

```
mvn package
java -jar target/littlepay.jar taps.csv trips.csv
```

Both arguments are optional and default to `taps.csv` and `trips.csv` in the current directory.

## Tests

```
mvn test
```

## Example

`taps.csv` in the repo root is the example input, and `trips.csv` is the output it produces.

## Assumptions

- The input file is well formed and has a header row.
- Tap IDs are unique. A repeated row (same ID and data) is ignored with a warning, so it is not charged twice. The same ID with different data stops the run with an error.
- Taps are matched per PAN in date-time order, not file order. Taps in the same second are ordered ON before OFF, then by tap ID.
- A tap off only completes a trip if it is on the same company and bus as the tap on. Otherwise the tap on becomes an INCOMPLETE trip and the tap off is ignored.
- There is no time limit between a tap on and its tap off. A tap on one day and a tap off days later still make one trip.
- A second tap on before a tap off makes the first tap on an INCOMPLETE trip.
- A tap off with no matching tap on is ignored and logged as a warning.
- Only Stop1, Stop2 and Stop3 exist. A tap at any other stop is ignored and logged as a warning, and the rest of the file is still processed. If that tap was a tap off, its tap on becomes an INCOMPLETE trip.
- All times are UTC as given in the input. No time zone or daylight saving conversion is done.
- INCOMPLETE trips have an empty `Finished` and `ToStopId`, and a duration of 0.
- Trips are written in order of start time.
