package java8.time;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;

public record Booking(LocalDate checkIn, LocalDate checkOut) {

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public Period stay() {
        return Period.between(checkIn, checkOut);
    }
}
