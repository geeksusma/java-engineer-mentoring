# Baby Steps

**Make the smallest change that moves you forward, check it, and then take the next one.**

> **Goes with:** [Vertical Slicing](../vertical-slicing/vertical-slicing.md). Slicing decides *what* to build next. Baby steps decide *how* you walk there.

---

## Where it comes from: eXtreme Programming

Baby Steps is one of the principles of **eXtreme Programming (XP)**. Kent Beck lists it in *Extreme Programming Explained* (2nd ed., 2004), together with Flow, Feedback, Reflection and Self-Similarity.

XP turns the dials of good practice up to the maximum. If short feedback is good, make it as short as possible:

| XP practice              | Baby step version                         |
|--------------------------|-------------------------------------------|
| Test-Driven Development  | One failing test at a time                |
| Continuous Integration   | Integrate several times a day             |
| Small Releases           | Ship small increments often               |
| Refactoring              | Many tiny safe moves, not one big rewrite |

> *"What's the least you could do that is recognizably in the right direction?"* — Kent Beck

---

## Big step vs baby steps

Same task for two developers: **"add state tax to the order total."** Both make the same two mistakes along the way: a wrong rounding mode and a typo in the NV rate (`0.8` instead of `0.08`).

**Ana takes one big step.** She changes everything, then runs the tests.

```
10:00  add `state` to Order, change calculator signature,
       add the 5-state tax table, parse state from input,
       update the output format
11:30  run tests → 9 red
```

Nine failures, three causes mixed together:

- 6 tests fail because the calculator signature changed. That is noise, not a bug.
- 2 fail because of rounding.
- 1 fails because of the NV typo, but it's hidden among the others.

She spends the next hour working out which failure belongs to which change. `git reset` would throw away 90 minutes of work.

**Bruno takes baby steps.** He runs the tests after every small change.

```
10:00  tax for UT only, hardcoded                    → green, commit
10:06  apply tax after the subtotal                  → red: 99.99 in UT, 106.84 expected, 106.83 actual
                                                       only the rounding changed → fix it → green, commit
10:12  read the state code, support UT and NV        → red: NV total is wrong
                                                       only the NV rate changed → typo found → green, commit
10:18  remaining 3 states                            → green, commit
```

Each failure shows up **alone**, right after the change that caused it. He never has to debug. The answer is always "the thing I just did." The worst case is reverting 6 minutes of work.

|                               | Ana (big step)      | Bruno (baby steps)   |
|-------------------------------|---------------------|----------------------|
| Time until first feedback     | 90 minutes          | 6 minutes            |
| Failing tests at once         | 9                   | 1                    |
| Where the bug is              | Somewhere in 90 min | In the last change   |
| Cost of `git reset`           | 90 minutes          | 6 minutes            |

**Both made the same mistakes. Bruno found his in seconds. The size of the step is the size of your risk.**

---

## TDD Flow

1. **Pick** the smallest next behaviour.
2. **Red:** write one test for it.
3. **Green:** the simplest code that passes (see [KISS](../kiss/kiss.md)).
4. **Refactor** while everything is green.
5. **Commit.** You now have a safe point to come back to.
6. **Repeat.**

If a step goes wrong, **don't debug, revert** and take a smaller step.

---

## How small is small?

A useful rule: **you should be able to go back to green in a couple of minutes.**

Signs your steps are too big:

- You have not run the tests in the last 10 minutes.
- You have more than one failing test.
- Your commit message needs the word "and".
- You are afraid of `git reset --hard`.

---

## Baby steps need a small target

Baby steps work for the code, but only if the goal is small too. If the story is "build the order calculator", there is no obvious first step.

That is the job of **[Vertical Slicing](../vertical-slicing/vertical-slicing.md)**: cut the big problem into thin slices, then walk each slice with baby steps.
