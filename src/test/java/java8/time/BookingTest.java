package java8.time;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookingTest {

    @Test
    void should_countCalendarDaysBetweenCheckInAndCheckOut_when_computingNights() {
        Booking booking = new Booking(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 1, 20));

        assertEquals(5, booking.nights());
    }

    @Test
    void should_expressTheStayAsYearsMonthsAndDays_when_computingPeriod() {
        Booking booking = new Booking(LocalDate.of(2024, 1, 15), LocalDate.of(2024, 3, 20));

        assertEquals(Period.of(0, 2, 5), booking.stay());
    }
}
