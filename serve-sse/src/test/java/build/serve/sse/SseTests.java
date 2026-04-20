package build.serve.sse;

import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import build.serve.testing.TestSseEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SseTests {

    @Test
    void basicTextStream() {
        try (var server = TestServer.of(RouterBuilder.create()
            .get("/events", SseUpgrade.sse(emitter -> {
                emitter.send("one");
                emitter.send("two");
                emitter.send("three");
            })).build());
             var stream = server.sse("/events")) {
            var events = stream.collectAll(Duration.ofSeconds(5));
            assertThat(events).extracting(TestSseEvent::data)
                .containsExactly("one", "two", "three");
        }
    }

    @Test
    void eventWithId() {
        try (var server = TestServer.of(RouterBuilder.create()
            .get("/events", SseUpgrade.sse(emitter ->
                emitter.send(new SseEvent("hello", "42", null, null))))
            .build());
             var stream = server.sse("/events")) {
            var events = stream.collectAll(Duration.ofSeconds(5));
            assertThat(events).hasSize(1);
            assertThat(events.get(0).data()).isEqualTo("hello");
            assertThat(events.get(0).id()).contains("42");
        }
    }

    @Test
    void eventWithType() {
        try (var server = TestServer.of(RouterBuilder.create()
            .get("/events", SseUpgrade.sse(emitter ->
                emitter.send(SseEvent.of("tick", "count=0"))))
            .build());
             var stream = server.sse("/events")) {
            var events = stream.collectAll(Duration.ofSeconds(5));
            assertThat(events).hasSize(1);
            assertThat(events.get(0).data()).isEqualTo("count=0");
            assertThat(events.get(0).event()).contains("tick");
        }
    }

    @Test
    void emitterCloseEndsStream() {
        try (var server = TestServer.of(RouterBuilder.create()
            .get("/events", SseUpgrade.sse(emitter -> {
                emitter.send("before");
                emitter.close();
            })).build());
             var stream = server.sse("/events")) {
            var events = stream.collectAll(Duration.ofSeconds(5));
            assertThat(events).extracting(TestSseEvent::data).containsExactly("before");
        }
    }

    @Test
    void connectionDropHandled() throws Exception {
        var detected = new CompletableFuture<Boolean>();
        var started = new CountDownLatch(1);

        try (var server = TestServer.of(RouterBuilder.create()
            .get("/events", SseUpgrade.sse(emitter -> {
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
            })).build())) {
            var stream = server.sse("/events");
            started.await(5, TimeUnit.SECONDS);
            Thread.sleep(50);
            stream.close();
        }

        assertThat(detected.get(5, TimeUnit.SECONDS)).isTrue();
    }
}
