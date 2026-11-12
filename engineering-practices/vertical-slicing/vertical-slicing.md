# Vertical Slicing

**Cut a big problem into thin slices. Each slice goes end to end and gives something a user can see.**

Each slice is *vertical*: it crosses every layer (input, logic, output), a little bit of each, instead of building one whole layer at a time.

> **Goes with:** [Baby Steps](../baby-steps/baby-steps.md). Slicing decides *what* to build next. Baby steps decide *how* you walk there.

---

## Horizontal vs vertical

**Horizontal (by layer):**

```
Sprint 1: database tables
Sprint 2: services
Sprint 3: REST API
Sprint 4: UI          ← first time a user sees anything
```

Nothing works until everything works. Feedback arrives at the end, when changes cost the most.

**Vertical (by slice):**

```
Slice 1: tiny UI + tiny logic + tiny output   ← user sees it
Slice 2: one more rule, all the way through   ← user sees it
Slice 3: ...
```

Something works after the first slice. Every slice is a chance to learn, change direction or stop.

---

## The Elephant Carpaccio kata

**Elephant carpaccio** is an exercise by **Alistair Cockburn**. Henrik Kniberg later wrote the facilitation guide that made it popular.

The name says it all:

- The **elephant** is a big user story.
- **Carpaccio** is meat sliced so thin you can almost see through it.
- Each slice is still **elephant**: thin, but a complete piece of the whole.

### The problem

Build a retail calculator.

**Input:** number of items, price per item, and a 2-letter state code.
**Output:** the total price, with discount and tax applied.

| Order value | Discount |   | State | Tax   |
|-------------|----------|---|-------|-------|
| $1,000      | 3%       |   | UT    | 6.85% |
| $5,000      | 5%       |   | NV    | 8.00% |
| $7,000      | 7%       |   | TX    | 6.25% |
| $10,000     | 10%      |   | AL    | 4.00% |
| $50,000     | 15%      |   | CA    | 8.25% |

It looks like a 30-minute problem, and that is the trap.

### The rules of the kata

1. **Slice first (about 10 min).** Write down 10 to 20 slices. No code yet.
2. **Build (about 40 min)** in short iterations of about 8 minutes.
3. **Demo every slice** to the "product owner" (the facilitator or another pair).
4. A slice only counts if **a user can see it**. "I created the `Tax` class" is not a slice.
5. **Debrief:** how many slices did you deliver? How many did you plan?

### A typical slice list

| #  | Slice                                                     | What the user sees                  |
|----|-----------------------------------------------------------|-------------------------------------|
| 1  | Walking skeleton: run the app, print a hardcoded total    | Something runs                      |
| 2  | Read items and price, print `items × price`               | A real order value                  |
| 3  | Add UT tax (hardcoded, the only state)                    | Price with tax, for one state       |
| 4  | Read the state code, support UT and NV                    | The state changes the result        |
| 5  | Support all 5 states                                      | Full tax table                      |
| 6  | 3% discount for orders of $1,000 or more                  | First discount                      |
| 7  | 5% discount for $5,000 or more                            | Discounts grow with the order       |
| 8  | All discount tiers                                        | Full discount table                 |
| 9  | Show the breakdown: subtotal, discount, tax, total        | The user understands the number     |
| 10 | Reject an unknown state with a clear message              | Friendly errors                     |

Notice what is **not** on the list: "design the data model", "create the services", "build the UI". Each row touches input, logic and output, a little at a time.

### What teams usually learn

- The first try gives 3 or 4 "slices" that are really layers.
- After the debrief, the same team can find **15 or more** real slices.
- Teams with thin slices deliver **more** in the same 40 minutes, not less. Most of their time goes into working software, not into integration surprises at the end.
- The product owner can say *"stop, slices 1 to 7 are enough to ship"*. Horizontal teams can't give that choice.

---

## Slicing tips that work beyond the kata

Split a story along one of these lines:

- **Hardcode first, then generalize:** one state → two states → all states.
- **One rule at a time:** one discount tier, then the next.
- **Happy path first, errors later:** valid state first, then unknown state.
- **Simple output first, rich output later:** total first, then the breakdown.
- **Manual first, automated later:** a fixed config file before an admin screen.

**A good slice has value, can be demoed, and fits in hours, not days.**

---

## Combining both: Vertical Slicing + Baby Steps

The two techniques work at different sizes. They have the same shape, which XP calls **Self-Similarity**.

```
Story (the elephant)
 └── Slice              ← vertical slicing  (hours)
      └── Baby step     ← red / green / refactor / commit  (minutes)
```

| Question              | Vertical Slicing           | Baby Steps                |
|-----------------------|---------------------------|---------------------------|
| What does it cut?     | The problem               | The code changes          |
| Unit of work          | A demoable slice          | One test, one commit      |
| Feedback from         | The user / product owner  | The tests / the compiler  |
| Typical size          | Hours                     | Minutes                   |
| If it goes wrong      | Re-slice, re-prioritize   | Revert, take a smaller step |

### Walkthrough: slice 6, "3% discount from $1,000"

Slices 1 to 5 are done and demoed. Now we zoom into slice 6 and walk it with baby steps.

**Step 1: Red.** One example, right at the threshold.

```java
@Test
void should_applyThreePercentDiscount_when_orderValueIsOneThousand() {
    assertEquals(new BigDecimal("970.00"), discount.applyTo(new BigDecimal("1000.00")));
}
```

**Step 2: Green.** The simplest code that passes.

```java
public BigDecimal applyTo(BigDecimal orderValue) {
    return new BigDecimal("970.00");
}
```

Hardcoded, on purpose. Commit: `Discount returns discounted value for 1000`.

**Step 3: Red.** An example that breaks the fake.

```java
@CsvSource({"1000.00, 970.00", "2000.00, 1940.00"})
void should_applyThreePercentDiscount_when_orderValueIsAtLeastOneThousand(String value, String expected) { ... }
```

**Step 4: Green.** Now the real formula.

```java
return orderValue.multiply(new BigDecimal("0.97")).setScale(2, HALF_UP);
```

Commit: `Discount applies 3%`.

**Step 5: Red.** Below the threshold, no discount.

```java
@CsvSource({"999.99, 999.99"})
void should_notApplyDiscount_when_orderValueIsBelowOneThousand(String value, String expected) { ... }
```

**Step 6: Green.** Add the `if`. Commit: `No discount below 1000`.

**Step 7: Refactor.** Name the constants (`THRESHOLD`, `RATE`). Tests stay green. Commit.

**Step 8: Wire it in.** Call the discount before the tax in the calculator. Run the app with `1000 items × $1, UT` and check the total. Commit.

**Step 9: Demo the slice.** *"Orders of $1,000 or more now get 3% off."*

That is about 6 small commits in less than an hour, and the user saw something new at the end. If step 4 had gone wrong, we would revert one commit, not the whole slice.

### When to go back up a level

- **The baby steps don't fit in a slice** (the slice keeps growing): the slice is too thick. Stop and re-slice.
- **You can't find the first baby step:** the slice is not clear yet. Talk to the product owner, or pick a more hardcoded version.
- **A slice is done but nobody can see it:** it was a layer, not a slice. Fix the slice list.

---

## Summary

- **Vertical slicing** turns the elephant into thin, valuable, demoable pieces.
- **Baby steps** turn each piece into tiny, safe, reversible moves.
- Together they give you **feedback every few minutes from the code** and **every few hours from the user**.

**Cut the problem thin. Walk it in small steps.**
