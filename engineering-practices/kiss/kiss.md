# KISS — Keep It Simple, Stupid

**Write the simplest code that solves today's problem.**

> **Examples:** [`src/test/java/engineeringpractices/kiss`](../../src/test/java/engineeringpractices/kiss). Run `mvn test`.

---

## Not KISS

```java
var context = new HashMap<String, Object>();
context.put(PricingEngine.LINES, List.of(Map.of("price", "10.00", "qty", 1)));
context.put(PricingEngine.TIER, "STANDARD");
new PricingEngine(List.of("subtotal", "loyalty", "shipping")).run(context);
```

The code uses magic keys and casts, and a typo only shows up when it runs. Nobody asked for this flexibility.

## KISS

```java
public class ShippingFee {
    public BigDecimal feeFor(BigDecimal amount) {
        return amount.compareTo(FREE_SHIPPING_FROM) >= 0 ? BigDecimal.ZERO : STANDARD_FEE;
    }
}
```

Each rule gets one small class.

---

## Simple → small classes → easy tests

```java
@CsvSource({"49.99, 4.99", "50.00, 0"})
void should_chargeStandardFee_when_amountIsBelowThreshold(String amount, String fee) {
    assertEquals(new BigDecimal(fee), shippingFee.feeFor(new BigDecimal(amount)));
}
```

The test has one input, one output, no setup and no mocks.

**If a test is hard to write, simplify the code, not the test.**

---

## KISS is the heart of TDD

*"Do the simplest thing that could possibly work."* — Kent Beck

1. **Red:** 50.00 ships free.
2. **Green:** `return BigDecimal.ZERO;`
3. **Red:** 49.99 pays 4.99.
4. **Green:** add the `if`.
5. **Refactor:** name the constants.
6. **Stop.** No test asks for more.

TDD keeps the code simple, and simple code keeps TDD fast.
