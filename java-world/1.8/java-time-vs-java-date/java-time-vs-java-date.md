# `java.time` vs. `java.util.Date`

Java's original date/time API (`java.util.Date`, `java.util.Calendar`, `java.text.SimpleDateFormat`) shipped in Java 1.0/1.1 and is widely regarded as one of the worst-designed corners of the standard library. Java 8 replaced it with `java.time` (JSR-310, largely designed by the author of Joda-Time), which fixes every one of its structural problems.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java8/time`](../../../src/test/java/java8/time) (classes under `src/main/java/java8/time`). Run them with `mvn test`.

---

## What was wrong with the old API

```java
Date date = new Date(2024, 0, 15); // Deprecated constructor. Year is "years since 1900" (=1900+2024!), month is 0-indexed (0=January)

Calendar cal = Calendar.getInstance();
cal.set(Calendar.MONTH, Calendar.JANUARY); // still 0-indexed
cal.add(Calendar.DAY_OF_MONTH, 1);         // mutates cal IN PLACE
```

1. **Mutable.** `Date.setTime(...)` and `Calendar.add(...)`/`.set(...)` change the object in place. Pass a `Date` into another method and you have no guarantee it comes back unchanged — a classic source of subtle bugs when a `Date` is shared or cached.
2. **Confusing numbering.** Years are offset from 1900, months are 0-indexed (`0` = January, so December is `11`), but days of the month are 1-indexed. Nothing about the API tells you this; you just have to know.
3. **Not thread-safe.** `SimpleDateFormat` keeps internal mutable state (a `Calendar` instance) during parsing/formatting. Sharing one instance across threads corrupts results non-deterministically — a bug class subtle enough that "give every thread its own `SimpleDateFormat`" became standard folklore advice rather than something the API prevented.
4. **One type doing everything, badly.** `Date` conflates "a point in time," "a calendar date," and "a local wall-clock time" into a single mutable class, so you can't tell from the type alone which concept a given value actually represents.

---

## `java.time`: immutable, thread-safe, and precise about what each type means

Every class in `java.time` is **immutable** — every "mutating" method (`plusDays`, `withYear`, `minusHours`...) returns a **new** instance and leaves the original untouched:

```java
LocalDate original = LocalDate.of(2024, 1, 15);
LocalDate nextWeek = original.plusDays(7);

original;  // still 2024-01-15 — unchanged
nextWeek;  // 2024-01-22 — a brand new object
```

Because instances never change after construction, they're automatically thread-safe with no synchronization needed — including `DateTimeFormatter`, the immutable, thread-safe replacement for `SimpleDateFormat`.

### The core types, and what each one actually models

| Type | Represents | Example |
|---|---|---|
| `LocalDate` | A calendar date, no time, no timezone | "January 15th, 2024" — a birthday |
| `LocalTime` | A time of day, no date, no timezone | "14:30" — a recurring daily alarm |
| `LocalDateTime` | Date + time, still no timezone | "2024-01-15T14:30" — a naive timestamp |
| `ZonedDateTime` | Date + time + timezone/offset rules | "2024-01-15T14:30+01:00[Europe/Madrid]" — an actual scheduled meeting |
| `Instant` | A single point on the UTC timeline (machine time) | Nanoseconds since the epoch — what you store/compare/log |
| `Duration` | An amount of time in seconds/nanoseconds | "2 hours, 30 minutes" |
| `Period` | An amount of time in years/months/days | "1 year, 2 months" |

```java
public record Booking(LocalDate checkIn, LocalDate checkOut) {
    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public Period stay() {
        return Period.between(checkIn, checkOut);
    }
}
```

**Rule of thumb:** use `LocalDate`/`LocalDateTime` for things people think of in calendar/wall-clock terms (a birthday, a booking, "every day at 9am"); use `Instant` for machine-facing timestamps you store, compare, or log (`createdAt`, `expiresAt`); use `ZonedDateTime` only when the timezone itself is meaningful to the business logic (scheduling a meeting across timezones, daylight-saving-aware recurrence).

---

## `Duration` vs. `Period`: machine time vs. human time

Both represent "an amount of time," but they answer different questions and are **not interchangeable**:

- **`Duration`** — an exact number of seconds/nanoseconds. `Duration.ofHours(24)` is always exactly 24×60×60 seconds, full stop.
- **`Period`** — a calendar-based amount in years/months/days. `Period.ofDays(1)` means "add one calendar day," which is *usually* 24 hours — except across a daylight-saving transition, where the wall-clock day is 23 or 25 hours long.

```java
Duration.ofHours(24);   // exactly 86400 seconds, always
Period.ofDays(1);       // "the next calendar day" — 23-25 real hours across a DST change
```

Use `Duration` for machine-precise elapsed time (timeouts, benchmarking, `Instant` arithmetic). Use `Period` for human calendar arithmetic ("add one month to this due date").

---

## Interop with the legacy API

You'll still meet `java.util.Date` at the boundary of older libraries/frameworks/JDBC drivers. `Instant` is the bridge:

```java
public static ZonedDateTime toZonedDateTime(Date legacyDate, ZoneId zone) {
    return legacyDate.toInstant().atZone(zone);
}

public static Date toLegacyDate(ZonedDateTime zonedDateTime) {
    return Date.from(zonedDateTime.toInstant());
}
```

`Date.toInstant()` and `Date.from(Instant)` were added specifically to make this migration path painless — there's rarely a reason to hand-roll date math against the legacy types anymore.

**In the modern world:** all new code uses `java.time` exclusively. `Date`/`Calendar`/`SimpleDateFormat` show up only when calling into old APIs you don't control, and even then you convert to `java.time` at the boundary as early as possible rather than letting mutable, ambiguous types spread through your own code.

---

## Summary

| | `java.util.Date` / `Calendar` | `java.time` |
|---|---|---|
| Mutable? | Yes — `setTime`/`add`/`set` mutate in place | No — every operation returns a new instance |
| Thread-safe? | No (`SimpleDateFormat` especially) | Yes, by construction |
| Month numbering | 0-indexed (`0` = January) | 1-indexed (`1` = January) |
| Separates date/time/zone concepts? | No — one class does everything | Yes — `LocalDate`, `LocalTime`, `ZonedDateTime`, `Instant` are distinct types |
| Human calendar math (`+1 month`) | Manual, error-prone | `Period` |
| Exact elapsed time | Manual, error-prone | `Duration` |
