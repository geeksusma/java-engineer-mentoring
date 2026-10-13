# Clean Architecture

Robert C. Martin's **Clean Architecture** (2012 blog post, 2017 book) is a synthesis of Hexagonal, Onion and similar architectures. They all agree on the same thing — *business rules in the middle, details outside, dependencies pointing inwards* — and Clean Architecture gives it a name and a picture:

```
   ┌──────────────────────────────────────────────────────────┐
   │ Frameworks & Drivers     web, DB, UI, devices, CleanApp   │
   │   ┌──────────────────────────────────────────────────┐   │
   │   │ Interface Adapters   controllers, presenters,     │   │
   │   │                      gateways                     │   │
   │   │   ┌──────────────────────────────────────────┐   │   │
   │   │   │ Use Cases        application rules        │   │   │
   │   │   │   ┌──────────────────────────────────┐   │   │   │
   │   │   │   │ Entities   enterprise rules       │   │   │   │
   │   │   │   └──────────────────────────────────┘   │   │   │
   │   │   └──────────────────────────────────────────┘   │   │
   │   └──────────────────────────────────────────────────┘   │
   └──────────────────────────────────────────────────────────┘
                     all source-code dependencies ──▶ inwards
```

> **The Dependency Rule:** source code dependencies can only point inwards. Nothing in an inner circle can know anything about something in an outer circle.

If you've done the [Hexagonal](../hexagonal-ports-and-adapters/hexagonal-ports-and-adapters.md) lesson, most of this will feel familiar. This lesson focuses on what Clean Architecture **adds**: two kinds of business rules, request/response models, and the **presenter** that lets the use case push its result outwards without knowing who's listening.

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/macrodesign/clean`](../../src/test/java/macrodesign/clean) (classes under `src/main/java/macrodesign/clean`). Run them with `mvn test`.

---

## The example: registering a player

A game platform lets people register with a nickname. One package per circle:

```
macrodesign.clean
├── entities                  ← Player, PlayerId, Nickname, InvalidNicknameException
├── usecases                  ← RegisterPlayerInteractor
│                               RegisterPlayerInputBoundary / OutputBoundary
│                               RegisterPlayerRequest / Response
│                               PlayerGateway
├── interfaceadapters
│   ├── controllers           ← RegisterPlayerController
│   ├── presenters            ← RegisterPlayerPresenter, RegisterPlayerViewModel
│   └── gateways              ← PlayerRepository
└── frameworks
    ├── database              ← PlayerTable (our pretend database driver)
    └── CleanApp              ← main component: wires everything, plays the web framework
```

---

## Entities: enterprise business rules

Rules that would be true **for every application** of the business — the website, the game launcher, the mobile app. What a valid nickname is, for example:

```java
public record Nickname(String value) {

    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9_]{3,16}");

    public Nickname {
        if (value == null || !VALID.matcher(value).matches()) {
            throw new InvalidNicknameException("nickname must be 3-16 letters, digits or underscores");
        }
    }

    public String canonical() {
        return value.toLowerCase(Locale.ROOT);
    }
}
```

Entities know nothing — not use cases, not databases:

```java
classesIn(ENTITIES).shouldNotDependOn(USE_CASES, INTERFACE_ADAPTERS, FRAMEWORKS);
```

---

## Use cases: application business rules

Rules specific to **this application**: the steps of "register a player". The interactor orchestrates entities and gateways:

```java
@Override
public void register(RegisterPlayerRequest request) {
    Nickname nickname;
    try {
        nickname = new Nickname(request.nickname());
    } catch (InvalidNicknameException e) {
        presenter.presentInvalidNickname(e.getMessage());
        return;
    }
    if (players.existsByNickname(nickname)) {
        presenter.presentNicknameTaken(nickname.value());
        return;
    }
    var player = new Player(players.nextId(), nickname, request.email());
    players.save(player);
    presenter.presentRegistered(new RegisterPlayerResponse(player.id().value(), nickname.value()));
}
```

Two things stand out.

### Request and response models are plain data

```java
public record RegisterPlayerRequest(String nickname, String email) {
}

public record RegisterPlayerResponse(String playerId, String nickname) {
}
```

Data crossing a boundary is **simple data structures**: no entities going out, no framework objects (`HttpServletRequest`, JPA entities) coming in. A `Player` never leaves the use case, so outer layers can't build logic on it.

### The use case returns `void`

In the Hexagonal lesson, `placeOrder` *returned* an `Order`. Here `register` returns nothing; it calls an **output boundary** instead:

```java
public interface RegisterPlayerOutputBoundary {

    void presentRegistered(RegisterPlayerResponse response);

    void presentInvalidNickname(String reason);

    void presentNicknameTaken(String nickname);
}
```

This is the subtle part of Clean Architecture. **Control flow** goes outwards (use case → presenter), but the **source-code dependency** still points inwards, because the presenter implements an interface that the use case owns. It's Dependency Inversion again, used this time on the way out.

It also gives each outcome its own method — the use case states *what happened*, never *how to show it*. Use cases are easy to test with a spy presenter:

```java
@Test
void should_presentNicknameTaken_when_nicknameDiffersOnlyInCase() {
    interactor.register(new RegisterPlayerRequest("Neo", "neo@example.com"));

    interactor.register(new RegisterPlayerRequest("NEO", "other@example.com"));

    assertEquals("taken NEO", presenter.calls.getLast());
    assertEquals(1, players.saved.size());
}
```

```java
classesIn(USE_CASES).shouldNotDependOn(INTERFACE_ADAPTERS, FRAMEWORKS);
```

---

## Interface adapters: converting data between worlds

### Controller: outside format → request model

```java
public void submit(Map<String, String> form) {
    registerPlayer.register(new RegisterPlayerRequest(form.get("nickname"), form.get("email")));
}
```

It returns nothing either: answering is the presenter's job.

### Presenter: response model → view model

```java
@Override
public void presentNicknameTaken(String nickname) {
    viewModel = new RegisterPlayerViewModel(409, "Sorry, '" + nickname + "' is already taken");
}
```

The **view model** holds everything the view needs, already formatted (status codes, messages, dates as strings). The view itself becomes so dumb it barely needs testing.

### Gateway: entities ↔ database rows

```java
@Override
public void save(Player player) {
    table.insert(new PlayerTable.Row(player.id().value(), player.nickname().value(),
            player.nickname().canonical(), player.email()));
}
```

`PlayerRepository` implements the `PlayerGateway` the use case owns, and is the only place that knows both entities and rows. All the SQL of a real app would live in this circle.

Controllers and presenters never touch the database driver:

```java
classesIn(INTERFACE_ADAPTERS + ".controllers", INTERFACE_ADAPTERS + ".presenters")
        .shouldNotDependOn(FRAMEWORKS);
```

---

## Frameworks & drivers: the details

The outermost circle is the tools: the web framework, the database, the UI. Uncle Bob calls them **details** — important to run the system, irrelevant to what the system *is*. You write little code here, mostly glue.

`PlayerTable` plays the database driver. It knows rows and columns, nothing about players:

```java
classesIn(FRAMEWORKS + ".database").shouldNotDependOn(ENTITIES, USE_CASES, INTERFACE_ADAPTERS);
```

`CleanApp` is the **main component**: the dirtiest part of the system, which creates everything and hands the "request" to the controller. Presenters hold per-request state, so it builds a fresh one per request:

```java
public RegisterPlayerViewModel handleRegistration(Map<String, String> form) {
    var presenter = new RegisterPlayerPresenter();
    var interactor = new RegisterPlayerInteractor(new PlayerRepository(table), presenter);
    new RegisterPlayerController(interactor).submit(form);
    return presenter.viewModel();
}
```

```java
assertEquals(new RegisterPlayerViewModel(201, "Welcome, Neo! (P-1)"),
        app.handleRegistration(Map.of("nickname", "Neo", "email", "neo@example.com")));
assertEquals(new RegisterPlayerViewModel(409, "Sorry, 'neo' is already taken"),
        app.handleRegistration(Map.of("nickname", "neo", "email", "other@example.com")));
```

---

## Clean vs Hexagonal

They are the same idea, described differently:

| Hexagonal | Clean Architecture |
|---|---|
| Inside / outside | Concentric circles |
| Domain | Entities (enterprise rules) |
| Application service | Use case interactor (application rules) |
| Driving port | Input boundary |
| Driven port | Gateway interface — **and** output boundary |
| Adapters | Interface adapters (controllers, presenters, gateways) + frameworks & drivers |
| Use case usually *returns* a result | Use case usually *pushes* a result to a presenter |

In practice, most "Clean Architecture" Spring projects skip the presenter and let the use case return a response model, which makes them hexagonal in all but name. That's fine. Use the presenter when the same use case feeds several very different outputs (JSON, HTML, a CLI, a report), or when you want the outcomes to be explicit methods rather than exceptions and status flags.

---

## Common mistakes

- **Counting packages instead of checking arrows.** Four folders called `entities/usecases/adapters/frameworks` mean nothing if `usecases` imports `jakarta.persistence`. The tests above are what make it Clean.
- **Entities as JPA entities.** Then the innermost circle depends on the outermost one.
- **Leaking entities out.** Returning a `Player` to the controller lets the web layer depend on enterprise rules' internals. Return a response model.
- **One interactor calling another.** Use cases are independent units; share logic through entities or a domain service instead.
- **Ceremony everywhere.** A boundary pair, two models and a presenter for a "get by id" is a lot of code. Apply the full pattern where the rules are worth protecting.

---

## Summary

| Circle | Holds | May depend on |
|---|---|---|
| Entities | Enterprise rules (`Nickname`, `Player`) | Nothing |
| Use cases | Application rules (`RegisterPlayerInteractor`), boundaries, request/response models, gateway interfaces | Entities |
| Interface adapters | Controllers, presenters + view models, gateway implementations | Use cases, entities, (gateways) drivers |
| Frameworks & drivers | Web, DB driver, main component | Anything — it's the glue |
