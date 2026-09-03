package java8.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * LocalDate never changes after construction — every "mutating" method
 * returns a new instance. Calendar does the opposite: passing it to another
 * method hands out a reference to the SAME mutable object, so a change made
 * there is visible to every other holder of that reference. That aliasing
 * footgun is exactly what java.time's immutability removes.
 */
class ImmutabilityTest {

    @Test
    void should_leaveTheOriginalUnchanged_when_callingPlusDaysOnALocalDate() {
        LocalDate original = LocalDate.of(2024, 1, 15);

        LocalDate nextWeek = original.plusDays(7);

        assertEquals(LocalDate.of(2024, 1, 15), original, "original must be untouched");
        assertEquals(LocalDate.of(2024, 1, 22), nextWeek, "a new instance should carry the result");
        assertNotEquals(original, nextWeek);
    }

    @Test
    void should_bePropagatedToEveryHolderOfTheReference_when_mutatingASharedCalendarInPlace() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(2024, Calendar.JANUARY, 15, 0, 0, 0);
        int dayBeforeMutation = calendar.get(Calendar.DAY_OF_MONTH);

        addOneWeek(calendar); // some other method received a reference to the SAME object

        assertNotEquals(dayBeforeMutation, calendar.get(Calendar.DAY_OF_MONTH),
                "the caller's own Calendar instance changed just because it was handed to another method");
        assertEquals(22, calendar.get(Calendar.DAY_OF_MONTH));
    }

    private static void addOneWeek(Calendar calendar) {
        calendar.add(Calendar.DAY_OF_MONTH, 7);
    }
}
