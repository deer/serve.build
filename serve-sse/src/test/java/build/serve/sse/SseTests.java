package build.serve.sse;

import build.serve.foundation.routing.RouterBuilder;
import build.serve.transport.http.HttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SseTests {

    private HttpTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.stop(0);
        }
    }

    @Test
    void basicTextStream() throws Exception {
        startWith(emitter -> {
            emitter.send("one");
            emitter.send("two");
            emitter.send("three");
        });

        var body = get("/events");

        assertThat(body).contains("data: one\n");
        assertThat(body).contains("data: two\n");
        assertThat(body).contains("data: three\n");
    }

    @Test
    void eventWithId() throws Exception {
        startWith(emitter ->
            emitter.send(new SseEvent("hello", "42", null, null))
        );

        var body = get("/events");

        assertThat(body).contains("id: 42\n");
        assertThat(body).contains("data: hello\n");
    }

    @Test
    void eventWithType() throws Exception {
        startWith(emitter ->
            emitter.send(SseEvent.of("tick", "count=0"))
        );

        var body = get("/events");

        assertThat(body).contains("event: tick\n");
        assertThat(body).contains("data: count=0\n");
    }

    @Test
    void emitterCloseEndsStream() throws Exception {
        startWith(emitter -> {
            emitter.send("before");
            emitter.close();
        });

        var body = get("/events");

        assertThat(body).contains("data: before\n");
    }

    @Test
    void connectionDropHandled() throws Exception {
        var detected = new CompletableFuture<Boolean>();
        var started = new CountDownLatch(1);

        startWith(emitter -> {
            started.countDown();
            try {
                for (int i = 0; i < 1000; i++) {
                    if (!emitter.isOpen()) {
                        detected.complete(true);
                        return;
                    }
                    try {
                        emitter.send("tick " + i);
                    } catch (final Exception e) {
                        detected.complete(!emitter.isOpen());
                        return;
                    }
                    Thread.sleep(10);
                }
                detected.complete(false);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                detected.complete(false);
            }
        });

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(uri("/events"))
            .GET()
            .build();

        // Send request then cancel quickly
        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        started.await(5, TimeUnit.SECONDS);
        Thread.sleep(50);
        future.cancel(true);

        assertThat(detected.get(5, TimeUnit.SECONDS)).isTrue();
    }

    // --- Helpers ---

    private void startWith(final SseHandler sseHandler) throws Exception {
        var router = RouterBuilder.create()
            .get("/events", SseUpgrade.sse(sseHandler))
            .build();

        transport = new HttpTransport(new InetSocketAddress("127.0.0.1", 0), 0, router);
        transport.start();
    }

    private URI uri(final String path) {
        var port = transport.address().getPort();
        return URI.create("http://127.0.0.1:" + port + path);
    }

    private String get(final String path) throws Exception {
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(uri(path))
            .GET()
            .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
            .hasValue("text/event-stream");

        return response.body();
    }
}
