package java17.records;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    /**
     * Compact constructor: no parameter list, no field assignments. It runs
     * before the generated assignments, so it can validate and normalize the
     * parameters, and whatever they hold at the end is what gets stored.
     */
    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must not be negative: " + amount);
        }
        amount = amount.setScale(2, RoundingMode.HALF_EVEN);
        currency = currency.toUpperCase(Locale.ROOT);
    }

    public static Money euros(String amount) {
        return new Money(new BigDecimal(amount), "EUR");
    }

    public Money plus(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("cannot add " + other.currency + " to " + currency);
        }
        return new Money(amount.add(other.amount), currency);
    }

    /**
     * Records are immutable, so "changing" one means building a copy — the
     * so-called wither.
     */
    public Money withAmount(BigDecimal newAmount) {
        return new Money(newAmount, currency);
    }
}
