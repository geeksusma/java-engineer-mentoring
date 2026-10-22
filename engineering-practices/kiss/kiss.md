# KISS — Keep It Simple, Stupid

**Pick the simplest solution that solves today's problem.** Not the cleverest, not the most generic.

> **Runnable examples:** [`src/test/java/engineeringpractices/kiss`](../../src/test/java/engineeringpractices/kiss). Run them with `mvn test`.

---

## The problem

Three pricing rules: sum the lines, GOLD customers get 10% off, shipping is free from 50.00 (otherwise 4.99).

### Not KISS: a "flexible" engine

```java
var context = new HashMap<String, Object>();
context.put(PricingEngine.LINES, List.of(Map.of("price", "10.00", "qty", 1)));
context.put(PricingEngine.TIER, "STANDARD");

new PricingEngine(List.of("subtotal", "loyalty", "shipping")).run(context);
```

Magic keys, casts, rules named by `String`. Swap the order or misspell `"loyality"` and you find out at runtime. Nobody asked for this flexibility (**YAGNI**).

### KISS: one small class per rule

```java
public class ShippingFee {

    private static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("50");
    private static final BigDecimal STANDARD_FEE = new BigDecimal("4.99");

    public BigDecimal feeFor(BigDecimal amount) {
        return amount.compareTo(FREE_SHIPPING_FROM) >= 0 ? BigDecimal.ZERO : STANDARD_FEE;
    }
}
```

Same for `LoyaltyDiscount` and `OrderLine`. `OrderTotal` just wires them, so the order is plain code:

```java
var discounted = loyaltyDiscount.applyTo(subtotal, tier);
return discounted.add(shippingFee.feeFor(discounted));
```

No interfaces, no factories, no mocks. Add them when a second need shows up, not before.

---

## Simple → small classes → easy tests

```java
@CsvSource({
        "0.00,   4.99",
        "49.99,  4.99",
        "50.00,  0",
        "120.00, 0"
})
void should_chargeStandardFee_when_amountIsBelowFreeShippingThreshold(String amount, String expectedFee) {
    assertEquals(new BigDecimal(expectedFee), shippingFee.feeFor(new BigDecimal(amount)));
}
```

One input, one output, no setup. With the engine, you'd build the whole context to test one rule.

- Rules in one big method **multiply** test cases (2 × 2 × 2...). Small classes **add** them (2 + 2 + 2...).
- **A hard test is a design smell.** Big setup, lots of mocks, unrelated data → the code isn't simple. Fix the code, not the test.

---

## KISS is the heart of TDD

Green phase rule (Kent Beck): *"Do the simplest thing that could possibly work."*

1. Test: 50.00 ships free → `return BigDecimal.ZERO;` (yes, really)
2. Test: 49.99 pays 4.99 → now add the `if`.
3. Refactor: name the constants.
4. **Stop.** No test asks for countries or express shipping, so they don't exist.

TDD keeps the code simple (you only write what a test needs). Simple code keeps TDD fast (the next test is easy to write).

---

## Remember

- Simple ≠ short. Easy to read wins.
- No pattern without a problem.
- "We might need it later" → then add it later.
