package java8.time;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Duration is exact machine time (seconds/nanoseconds). Period is human
 * calendar time (years/months/days) and isn't reducible to a fixed number
 * of seconds — it means something different depending on the calendar.
 */
class DurationVsPeriodTest {

    @Test
    void should_representExactlyEightySixThousandFourHundredSeconds_when_creatingADurationOfADay() {
        assertEquals(86_400, Duration.ofHours(24).toSeconds());
    }

    @Test
    void should_notExposeASecondsBasedValue_when_workingWithAPeriod() {
        Period oneCalendarDay = Period.ofDays(1);

        assertEquals(0, oneCalendarDay.getYears());
        assertEquals(0, oneCalendarDay.getMonths());
        assertEquals(1, oneCalendarDay.getDays());
        // Period intentionally has no toSeconds()/toHours() — "one calendar day"
        // isn't a fixed duration once daylight saving is in the picture.
    }
}
