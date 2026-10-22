package engineeringpractices.kiss.simple;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderLineTest {

    @Test
    void should_multiplyPriceByQuantity_when_computingSubtotal() {
        assertEquals(new BigDecimal("37.50"), OrderLine.of("12.50", 3).subtotal());
    }
}
