package engineeringpractices.kiss.simple;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShippingFeeTest {

    private final ShippingFee shippingFee = new ShippingFee();

    @ParameterizedTest
    @CsvSource({
            "0.00,   4.99",
            "49.99,  4.99",
            "50.00,  0",
            "120.00, 0"
    })
    void should_chargeStandardFee_when_amountIsBelowFreeShippingThreshold(String amount, String expectedFee) {
        assertEquals(new BigDecimal(expectedFee), shippingFee.feeFor(new BigDecimal(amount)));
    }
}
