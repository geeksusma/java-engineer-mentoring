package java8.time;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LegacyDateConverterTest {

    @Test
    void should_preserveTheSameInstant_when_convertingALegacyDateToZonedDateTimeAndBack() {
        Date legacyDate = Date.from(Instant.parse("2024-01-15T10:00:00Z"));

        ZonedDateTime zonedDateTime = LegacyDateConverter.toZonedDateTime(legacyDate, ZoneId.of("Europe/Madrid"));
        Date roundTripped = LegacyDateConverter.toLegacyDate(zonedDateTime);

        assertEquals(legacyDate, roundTripped);
        assertEquals(11, zonedDateTime.getHour(), "Madrid is UTC+1 in January");
    }
}
