# Http Client reactive for Http/2

Before Java 11, making an HTTP call from the JDK meant reaching for `HttpURLConnection` — a blocking, verbose, HTTP/1.1-only API originally designed in 1996. Anyone who wanted HTTP/2 or non-blocking requests had to bring a third-party library (Apache HttpClient, OkHttp...). Java 11 standardized the incubating Java 9 API into `java.net.http.HttpClient`: a modern client with first-class HTTP/2 support and a non-blocking, `CompletableFuture`-based API alongside the classic blocking one.

```java
HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build();

HttpRequest request = HttpRequest.newBuilder(URI.create("https://example.com")).GET().build();

// Blocking
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

// Non-blocking / reactive
CompletableFuture<HttpResponse<String>> future =
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
```

> **Runnable examples:** every claim below has a working test backing it under [`src/test/java/java11/httpclient`](../../../src/test/java/java11/httpclient) (`ReactiveHttpClient` under `src/main/java/java11/httpclient`). Run them with `mvn test`. No HTTP client/server library is used — `ReactiveHttpClient` relies solely on `java.net.http.HttpClient`, and its tests spin up a JDK-native `com.sun.net.httpserver.HttpServer`.

---

## Blocking vs non-blocking, side by side

`HttpClient` exposes the same request through two methods that differ only in how the caller gets the result back:

```java
public String getSync(String url) throws IOException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
}

public CompletableFuture<String> getAsync(String url) {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body);
}
```

- `send(...)` blocks the calling thread until the response arrives. Simple to read, but a thread is parked doing nothing useful while waiting on I/O.
- `sendAsync(...)` returns immediately with a `CompletableFuture<HttpResponse<String>>`. The calling thread is free; the response (or failure) shows up whenever it's ready, and can be chained with `.thenApply`, `.thenCompose`, `.exceptionally`, etc.

---

## Fanning out requests concurrently

Because `sendAsync` returns a `CompletableFuture`, firing off many requests in parallel is just a matter of starting them all before waiting on any of them:

```java
public List<String> getAllConcurrently(List<String> urls) throws ExecutionException, InterruptedException {
    List<CompletableFuture<String>> pending = urls.stream()
            .map(this::getAsync)
            .toList();

    CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get();

    return pending.stream()
            .map(CompletableFuture::join)
            .toList();
}
```

Every `getAsync(url)` call starts its request immediately — the loop doesn't wait for one to finish before starting the next. `CompletableFuture.allOf(...).get()` then blocks only until *all* of them are done, not until each one finishes in turn.

The test proves this isn't just a theoretical difference: fetching four endpoints that each take ~150ms completes in well under the ~600ms a sequential approach would take, because they run in parallel.

---

## Why HTTP/2 matters here

HTTP/2 multiplexes many requests over a single TCP connection instead of opening one connection per request (or serializing requests on a small connection pool, as HTTP/1.1 pipelining effectively required in practice). `HttpClient` negotiates HTTP/2 by default when the server supports it, and it can be requested explicitly:

```java
HttpClient client = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)
        .build();
```

Combined with the async API, this means a single `HttpClient` instance can efficiently drive many concurrent requests without the "one thread per request" cost that plagued older blocking clients.

---

## Summary

| API | Call | Returns | Thread behavior |
|---|---|---|---|
| Blocking | `client.send(request, bodyHandler)` | `HttpResponse<T>` directly | Caller's thread blocks until the response arrives |
| Non-blocking / reactive | `client.sendAsync(request, bodyHandler)` | `CompletableFuture<HttpResponse<T>>` | Caller's thread is free immediately; result delivered asynchronously |

| Before Java 11 | Java 11 onward |
|---|---|
| `HttpURLConnection`: blocking only, HTTP/1.1 only, verbose API | `java.net.http.HttpClient`: blocking *and* non-blocking, HTTP/2 by default |
| HTTP/2 or async support required a third-party library | Both are standard, in `java.base`/`java.net.http` |
