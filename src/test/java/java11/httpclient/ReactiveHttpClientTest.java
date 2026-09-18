package java11.httpclient;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.util.List;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * There's no HTTP client/server library in this project's dependencies on
 * purpose — java.net.http.HttpClient needs none. The test server here is
 * com.sun.net.httpserver.HttpServer, shipped with the JDK since Java 6.
 * Its default executor runs one exchange at a time on the caller's thread,
 * which would silently serialize every "concurrent" request and make the
 * timing proof below meaningless — so an explicit thread pool is set before
 * start() to make the /slow endpoint genuinely handle requests in parallel.
 */
class ReactiveHttpClientTest {

    private HttpServer server;
    private String baseUrl;
    private ReactiveHttpClient client;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.createContext("/hello", exchange -> respond(exchange, "hello"));
        server.createContext("/slow", exchange -> {
            sleep(150);
            respond(exchange, "slow");
        });
        server.start();

        baseUrl = "http://localhost:" + server.getAddress().getPort();
        client = new ReactiveHttpClient();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void should_returnResponseBody_when_sendingSynchronously() throws Exception {
        assertEquals("hello", client.getSync(baseUrl + "/hello"));
    }

    @Test
    void should_returnResponseBody_when_sendingAsynchronously() throws Exception {
        assertEquals("hello", client.getAsync(baseUrl + "/hello").get());
    }

    @Test
    void should_beConfiguredForHttp2_when_clientIsBuilt() {
        assertEquals(HttpClient.Version.HTTP_2, client.configuredVersion());
    }

    @Test
    void should_returnEveryBody_when_fetchingMultipleUrlsConcurrently() throws Exception {
        List<String> urls = List.of(baseUrl + "/hello", baseUrl + "/hello", baseUrl + "/hello");

        List<String> bodies = client.getAllConcurrently(urls);

        assertEquals(List.of("hello", "hello", "hello"), bodies);
    }

    @Test
    void should_completeFasterThanSequentialRequestsWould_when_fetchingSlowEndpointsConcurrently() throws Exception {
        List<String> urls = List.of(
                baseUrl + "/slow", baseUrl + "/slow", baseUrl + "/slow", baseUrl + "/slow");

        long start = System.currentTimeMillis();
        List<String> bodies = client.getAllConcurrently(urls);
        long elapsedMillis = System.currentTimeMillis() - start;

        assertEquals(4, bodies.size());
        // 4 requests x ~150ms would be ~600ms sequential; concurrent fan-out
        // should land well under that.
        assertTrue(elapsedMillis < 400, "expected concurrent fetch under 400ms, took " + elapsedMillis);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body) throws IOException {
        byte[] bytes = body.getBytes();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
