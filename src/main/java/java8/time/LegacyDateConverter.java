package java8.time;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

/**
 * Instant is the bridge between the legacy Date/Calendar API and java.time —
 * there's rarely a reason to hand-roll date math against the old types once
 * you can convert at the boundary.
 */
public final class LegacyDateConverter {

    private LegacyDateConverter() {
    }

    public static ZonedDateTime toZonedDateTime(Date legacyDate, ZoneId zone) {
        return legacyDate.toInstant().atZone(zone);
    }

    public static Date toLegacyDate(ZonedDateTime zonedDateTime) {
        return Date.from(zonedDateTime.toInstant());
    }
}
