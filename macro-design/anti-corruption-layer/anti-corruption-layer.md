# Anti Corruption Layer

Sooner or later your clean model has to talk to a system you don't control: a twenty-year-old core banking system, a payment provider's SDK, an ERP, another team's service with a very different idea of what a "customer" is. If you use their model directly, their concepts, naming and quirks spread through your code. In DDD terms, your model gets **corrupted**.

An **Anti-Corruption Layer** (ACL) is a translation layer at the boundary. It talks to the other system in *its* language, and offers your domain an interface in *your* language. Nothing from the other side gets past it.

```
  our domain                       ACL                              legacy system
 (payments)                   (anticorruption)                         (legacy)

  Checkout ──▶ PaymentProvider ◀── LegacyPaymentAdapter ──────▶ LegacyPaymentTerminal
               Payment, Money,     LegacyPaymentTranslator         Map<String,String>
               PaymentResult                                       "RC" = "51", "CCY_NUM" = "978"
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/acl`](../../src/test/java/macrodesign/acl) (classes under `src/main/java/macrodesign/acl`). Run them with `mvn test`.

---

## The other side: a legacy payment terminal

`LegacyPaymentTerminal` simulates a third-party SDK we can't change. It's ugly in all the realistic ways:

```java
public Map<String, String> doTx(Map<String, String> request) throws TerminalException
```

- **Stringly typed:** request and response are `Map<String, String>`.
- **Amounts as strings of minor units:** 12.34 EUR is `"1234"`.
- **Numeric ISO-4217 currency codes:** EUR is `"978"`, not `"EUR"`.
- **Cryptic response codes:** `"00"` approved, `"51"` insufficient funds, `"05"` do not honour, `"14"` invalid card, `"91"` issuer unavailable...
- **A checked exception** with an error code string for anything technical: `TerminalException("ERR_COMMS_TIMEOUT")`.
- **Magic flags:** `TX_MODE = "S"` means "sale".

Imagine that scattered across the checkout code: `if ("51".equals(response.get("RC")))` in five places, and `amount.movePointRight(2)` which is wrong for Japanese yen.

---

## Our side: a model that speaks our language

```java
public record Money(BigDecimal amount, Currency currency) { ... }

public record Payment(String orderId, Money amount, String cardToken) {
}

public sealed interface PaymentResult {

    record Approved(String authorizationId) implements PaymentResult {
    }

    record Declined(DeclineReason reason) implements PaymentResult {
    }

    record ProviderUnavailable(String detail) implements PaymentResult {
    }
}
```

And a port shaped by **our** needs, not by the provider's API (same idea as the driven ports in [Hexagonal](../hexagonal-ports-and-adapters/hexagonal-ports-and-adapters.md)):

```java
public interface PaymentProvider {

    PaymentResult charge(Payment payment);
}
```

The domain code is short and reads like the business talks. The exhaustive `switch` over the sealed result (see the [Sealed Classes](../../java-world/1.17/sealed-classes/sealed-classes.md) lesson) means a new outcome can't be forgotten:

```java
public Outcome pay(String orderId, Money amount, String cardToken) {
    return switch (payments.charge(new Payment(orderId, amount, cardToken))) {
        case PaymentResult.Approved approved -> Outcome.PAID;
        case PaymentResult.Declined declined -> Outcome.ASK_FOR_ANOTHER_CARD;
        case PaymentResult.ProviderUnavailable unavailable -> Outcome.TRY_AGAIN_LATER;
    };
}
```

---

## The ACL: adapter + translator

An ACL usually has two parts.

### The translator: pure functions, both directions

All the knowledge about the other model lives in one class, with no I/O, so it's trivial to test:

```java
public static Map<String, String> toLegacyRequest(Payment payment) {
    var currency = payment.amount().currency();
    var minorUnits = payment.amount().amount()
            .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN)
            .movePointRight(currency.getDefaultFractionDigits());
    return Map.of(
            "TX_MODE", "S",                                                  // S = sale
            "MERCH_REF", payment.orderId(),
            "AMT_MINOR", minorUnits.toPlainString(),                         // 12.34 EUR -> "1234"
            "CCY_NUM", String.format("%03d", currency.getNumericCode()),      // EUR -> "978"
            "PAN_TOKEN", payment.cardToken());
}
```

Notice the currency rule: minor units depend on the currency. The translator gets it right once, and a test pins it down:

```java
@CsvSource({
        "10,     EUR, 1000",
        "0.995,  EUR, 100",
        "1500,   JPY, 1500",
        "1.234,  BHD, 1234"
})
void should_respectEachCurrencysMinorUnits_when_convertingAmounts(String amount, String currency, String minor)
```

And back, from response codes to our language:

```java
public static PaymentResult toDomainResult(Map<String, String> response) {
    return switch (response.getOrDefault("RC", "")) {
        case "00" -> new PaymentResult.Approved(response.get("AUTH_ID"));
        case "51" -> new PaymentResult.Declined(DeclineReason.INSUFFICIENT_FUNDS);
        case "05" -> new PaymentResult.Declined(DeclineReason.REFUSED_BY_ISSUER);
        case "14" -> new PaymentResult.Declined(DeclineReason.INVALID_CARD);
        case "91", "96" -> new PaymentResult.ProviderUnavailable("issuer or switch down");
        default -> new PaymentResult.Declined(DeclineReason.UNKNOWN);
    };
}
```

The `default` branch is a decision, not an afterthought: an **unknown code fails safe** (it's declined, never treated as approved).

### The adapter: implements our port, calls their system

```java
public class LegacyPaymentAdapter implements PaymentProvider {

    @Override
    public PaymentResult charge(Payment payment) {
        try {
            var response = terminal.doTx(LegacyPaymentTranslator.toLegacyRequest(payment));
            return LegacyPaymentTranslator.toDomainResult(response);
        } catch (TerminalException e) {
            return new PaymentResult.ProviderUnavailable(e.getMessage());
        }
    }
}
```

**Errors** get translated too: the checked `TerminalException` becomes a `ProviderUnavailable` result, so the domain never sees a legacy exception type.

```java
@CsvSource({
        "tok_ok,     PAID",
        "tok_broke,  ASK_FOR_ANOTHER_CARD",
        "tok_stolen, ASK_FOR_ANOTHER_CARD",
        "tok_weird,  ASK_FOR_ANOTHER_CARD",
        "tok_down,   TRY_AGAIN_LATER"
})
void should_decideInDomainTerms_when_legacyTerminalAnswers(String cardToken, Outcome expected) {
    assertEquals(expected, checkout.pay("SO-1", Money.of("19.99", "EUR"), cardToken));
}
```

For larger systems, an ACL may also include a **facade** that simplifies a sprawling API (ten legacy calls behind one of ours), and caching or retries around a slow system.

---

## Enforcing it

The whole point is that the legacy model stops at the ACL:

```java
@Test
void should_neverSeeLegacyTypes_when_inTheDomain() {
    classesIn("macrodesign.acl.payments").shouldNotDependOn("macrodesign.acl.legacy", "macrodesign.acl.anticorruption");
}

@Test
void should_notKnowAboutUs_when_beingTheLegacySystem() {
    classesIn("macrodesign.acl.legacy").shouldNotDependOn("macrodesign.acl.payments", "macrodesign.acl.anticorruption");
}
```

---

## Benefits

- **The domain stays clean.** Our code says `Declined(INSUFFICIENT_FUNDS)`, not `"51"`.
- **One place to learn the other system.** The translator and its tests *are* the documentation of the legacy protocol.
- **Replaceable provider.** Switching to a new payment provider means a new adapter implementing `PaymentProvider`; `Checkout` doesn't change.
- **Strangler fig migrations.** When replacing a legacy system piece by piece, the ACL lets new code live next to the old system without inheriting its model, until the old one can be switched off.

## Costs

- **More code:** mapping both ways, plus tests for it.
- **Lost detail:** translation can drop information you'll want later (keep the raw code in logs, or as an optional field).
- **Not always worth it:** if the upstream model is good and stable, being a **conformist** (just using it) is cheaper. Use an ACL when the other model is messy, unstable, or conceptually different from yours.

---

## ACL vs Adapter vs Bounded Context

- An **adapter** (Hexagonal) connects a *technology* to a port. An **ACL** connects a different *model* — it's an adapter whose main job is semantic translation.
- An ACL is one of the ways [bounded contexts](../bounded-context/bounded-context.md) integrate: the downstream context protects itself from the upstream one.

---

## Summary

| Element | In this example |
|---|---|
| Foreign model | `LegacyPaymentTerminal`: string maps, minor units, `"978"`, `"RC"="51"`, checked exception |
| Our model | `Money`, `Payment`, sealed `PaymentResult`, `DeclineReason` |
| Port (our terms) | `PaymentProvider.charge(Payment)` |
| Translator | `LegacyPaymentTranslator` — pure, both directions, unknown codes fail safe |
| Adapter | `LegacyPaymentAdapter` — calls the terminal, translates data **and** errors |
| Rule | Domain never depends on legacy types; legacy never knows about us |
| When to use | The other model is messy, unstable or conceptually different from yours |
