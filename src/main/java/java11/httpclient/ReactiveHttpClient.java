package java11.httpclient;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class ReactiveHttpClient {

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .executor(Executors.newFixedThreadPool(8))
            .build();

    public HttpClient.Version configuredVersion() {
        return client.version();
    }

    public String getSync(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString()).body();
    }

    public CompletableFuture<String> getAsync(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body);
    }

    public List<String> getAllConcurrently(List<String> urls) throws ExecutionException, InterruptedException {
        List<CompletableFuture<String>> pending = urls.stream()
                .map(this::getAsync)
                .toList();

        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0])).get();

        return pending.stream()
                .map(CompletableFuture::join)
                .toList();
    }
}
