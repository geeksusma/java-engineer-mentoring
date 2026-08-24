# Checked vs. Unchecked Exceptions

Java splits `Throwable` into two families that behave very differently at compile time.

```
Throwable
├── Error                      (unchecked — JVM/environment failures, don't catch)
└── Exception
    ├── RuntimeException       (unchecked — programming errors)
    └── everything else        (checked — recoverable, expected conditions)
```

- **Checked exceptions** (`IOException`, `SQLException`, `ParseException`...) — the compiler forces you to either `catch` them or declare `throws` on the method signature. Miss this and the code doesn't compile.
- **Unchecked exceptions** (`RuntimeException` and its subclasses: `NullPointerException`, `IllegalArgumentException`, `IllegalStateException`...) — no compiler enforcement. You can throw or ignore them freely; the code compiles either way.
- **`Error`** (`OutOfMemoryError`, `StackOverflowError`) — technically unchecked, but represents conditions your code generally shouldn't try to catch or recover from at all.

```java
// Checked: must handle or declare
public void readConfig() throws IOException {
    Files.readString(Path.of("config.yml"));
}

// Unchecked: compiles fine even though it can clearly throw
public int divide(int a, int b) {
    return a / b; // ArithmeticException if b == 0 — no declaration required
}
```

---

## Why the distinction exists

The idea (from Java's early API design) was: **checked exceptions model conditions the caller can reasonably be expected to recover from**, given the right context — a file not found, a network call timing out, a row violating a database constraint. The compiler forces every caller to consciously decide: handle it here, or pass the responsibility up.

**Unchecked exceptions model programming errors** — bugs. A `NullPointerException` or `IllegalArgumentException` means the code was called incorrectly. You don't want (and can't reasonably) force every method in the call chain to declare `throws NullPointerException` — that would make `throws` clauses useless noise, since almost any method can NPE.

---

## The problem with checked exceptions in practice

In theory this is a clean separation. In practice, checked exceptions have fallen out of favor for a few concrete reasons:

1. **They leak implementation details up the call stack.** Change a method from reading a file to calling an HTTP API, and every caller's `throws` clause (and every caller-of-a-caller) has to change too — even ones that don't care *how* the failure happened, only *that* it happened.
2. **They don't compose with functional interfaces.** `Stream.map(Function<T, R>)`, `Runnable`, `Callable`'s cousins like `Supplier`/`Consumer` — none of these declare checked exceptions, so wrapping a checked-throwing method inside a lambda forces an ugly try/catch-and-rewrap right there:
   ```java
   list.stream()
       .map(path -> {
           try {
               return Files.readString(path); // IOException is checked
           } catch (IOException e) {
               throw new UncheckedIOException(e); // forced rewrap to compile
           }
       });
   ```
3. **They tempt people into empty `catch` blocks** just to make the compiler happy, silently swallowing real failures.
4. **Modern JDK APIs reflect this.** `java.time` throws unchecked `DateTimeException`. Most reactive/stream-based libraries (Reactor, RxJava) and things like `CompletableFuture` deal in unchecked exceptions or wrap everything into a single failure signal. Spring's data-access exception hierarchy (`DataAccessException`) is entirely unchecked, specifically to avoid forcing `throws SQLException` everywhere.

**In the modern world:** most new Java code leans heavily toward unchecked exceptions, even for what look like "recoverable" conditions, and reserves checked exceptions for a narrow set of cases.

---

## When to actually choose one over the other

| Use a **checked** exception when... | Use an **unchecked** exception when... |
|---|---|
| The failure is expected, happens under normal operation, and the *immediate* caller has a real, different code path to take (retry, fallback, prompt the user again) | The failure is a bug/contract violation — bad input, invalid state, broken precondition — and the fix is to change the code, not to handle it at runtime |
| You're building a **low-level library API** where forcing acknowledgement is genuinely valuable (e.g. parsing user-supplied data: `DateTimeParseException`-style feedback) | You're multiple layers away from where anyone can meaningfully react (e.g. a repository failing deep in a service call chain — the controller can't "fix" a broken SQL constraint, it can only return an HTTP 500) |
| The condition is part of the *contract* of the operation, not an accident — e.g. `Files.readString` failing because a file plausibly won't exist is core to what "read a file" means | The exception needs to flow through functional interfaces (streams, lambdas, `CompletableFuture` chains) |
| You control both the API and its (few) callers, and want the compiler to guarantee no one forgets to handle a specific, actionable failure | You're wrapping/propagating a lower-level failure just to add context and rethrow — wrap it in an unchecked exception (e.g. `UncheckedIOException`, or your own `ServiceException extends RuntimeException`) rather than re-declaring the checked type everywhere |

A practical rule of thumb: **ask "can the immediate caller do something different because of this exception, right now?"**
- Yes → checked exception, and design its type to carry the info the caller needs to react.
- No, it's just going to get logged/rethrown/turned into an error response several layers up → unchecked exception, or wrap the checked one at the boundary where it's caught (e.g. a DAO layer converting `SQLException` into an unchecked `DataAccessException`) so it stops polluting every signature above it.

```java
// Checked makes sense: the caller can genuinely recover differently
class InsufficientFundsException extends Exception {
    InsufficientFundsException(String message) { super(message); }
}

void withdraw(Account acc, BigDecimal amount) throws InsufficientFundsException {
    if (acc.balance().compareTo(amount) < 0) {
        throw new InsufficientFundsException("Balance too low for withdrawal");
    }
}
// caller can catch this and show "insufficient funds" vs a generic error

// Unchecked makes sense: this is a bug, not a recoverable business condition
void withdraw(Account acc, BigDecimal amount) {
    if (amount.signum() < 0) {
        throw new IllegalArgumentException("Amount cannot be negative");
    }
}
// there's no "recovery" — the caller passed a bad value and needs to fix their code
```

---

## Summary

| | Checked | Unchecked (`RuntimeException`) |
|---|---|---|
| Compiler enforcement | Must `catch` or `throws` | None |
| Represents | Expected, recoverable conditions | Programming errors / bugs |
| Typical examples | `IOException`, `SQLException`, `TimeoutException` | `NullPointerException`, `IllegalArgumentException`, `IllegalStateException` |
| Works well with lambdas/streams | No — requires wrapping | Yes |
| Modern JDK/framework trend | Used sparingly, mostly at low-level I/O boundaries | Preferred default for most new APIs |
| Rule of thumb | The immediate caller can meaningfully do something different | The caller can only log/propagate — fix belongs in the code, not at runtime |
