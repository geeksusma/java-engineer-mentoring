package java17.records;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    void should_beEqual_when_allComponentsAreEqual() {
        assertEquals(Money.euros("10"), new Money(new BigDecimal("10.00"), "eur"));
        assertEquals(Money.euros("10").hashCode(), new Money(new BigDecimal("10.00"), "eur").hashCode());
    }

    @Test
    void should_generateReadableToString_when_notOverridden() {
        assertEquals("Money[amount=10.50, currency=EUR]", Money.euros("10.5").toString());
    }

    @Test
    void should_exposeAccessorsWithoutGetPrefix_when_readingComponents() {
        var money = Money.euros("3");

        assertEquals(new BigDecimal("3.00"), money.amount());
        assertEquals("EUR", money.currency());
    }

    @Test
    void should_normalizeComponents_when_compactConstructorReassignsParameters() {
        var money = new Money(new BigDecimal("1.005"), "usd");

        assertEquals(new BigDecimal("1.00"), money.amount());
        assertEquals("USD", money.currency());
    }

    @Test
    void should_rejectInvalidState_when_compactConstructorValidates() {
        assertThrows(IllegalArgumentException.class, () -> Money.euros("-1"));
        assertThrows(NullPointerException.class, () -> new Money(null, "EUR"));
    }

    @Test
    void should_returnNewInstance_when_derivingAChangedCopy() {
        var original = Money.euros("10");

        var changed = original.withAmount(new BigDecimal("99"));

        assertNotSame(original, changed);
        assertEquals(Money.euros("10"), original);
        assertEquals(Money.euros("99"), changed);
        assertEquals(Money.euros("109"), original.plus(changed));
    }

    @Test
    void should_describeItsComponents_when_inspectedByReflection() {
        assertTrue(Money.class.isRecord());
        assertEquals(List.of("amount", "currency"),
                Arrays.stream(Money.class.getRecordComponents()).map(RecordComponent::getName).toList());
    }
}
