/*-
 * #%L
 * Serve Testing
 * %%
 * Copyright (C) 2026 Reed von Redwitz
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package build.serve.testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A streaming SSE client for integration tests.
 *
 * <p>Connects to an SSE endpoint and parses events into a queue as they arrive.
 * Use {@link #collect(int, Duration)} to wait for N events, or
 * {@link #collectAll(Duration)} to wait for the stream to close.</p>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class TestSseStream implements AutoCloseable {

    private final BlockingQueue<TestSseEvent> queue = new LinkedBlockingQueue<>();
    private final CompletableFuture<Void> done = new CompletableFuture<>();
    private volatile InputStream stream;

    private TestSseStream() {
    }

    static TestSseStream connect(final URI uri) {
        final var sseStream = new TestSseStream();
        final var client = HttpClient.newHttpClient();
        final var request = HttpRequest.newBuilder().uri(uri).GET().build();

        Thread.ofVirtual().start(() -> {
            try {
                final var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                sseStream.stream = response.body();
                try (var reader = new BufferedReader(
                    new InputStreamReader(sseStream.stream, StandardCharsets.UTF_8))) {
                    StringBuilder data = null;
                    String event = null;
                    String id = null;
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty()) {
                            if (data != null) {
                                sseStream.queue.add(
                                    new TestSseEvent(data.toString(), Optional.ofNullable(event),
                                        Optional.ofNullable(id)));
                            }
                            data = null;
                            event = null;
                            id = null;
                        } else if (line.startsWith("data: ")) {
                            if (data == null) {
                                data = new StringBuilder();
                            } else {
                                data.append('\n');
                            }
                            data.append(line.substring(6));
                        } else if (line.startsWith("event: ")) {
                            event = line.substring(7);
                        } else if (line.startsWith("id: ")) {
                            id = line.substring(4);
                        }
                    }
                }
                sseStream.done.complete(null);
            } catch (final IOException e) {
                sseStream.done.complete(null);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                sseStream.done.complete(null);
            }
        });

        return sseStream;
    }

    /**
     * Blocks until exactly {@code count} events have been received or the timeout elapses.
     *
     * @param count   the number of events to collect
     * @param timeout the maximum time to wait
     * @return the collected events in arrival order
     */
    public List<TestSseEvent> collect(final int count, final Duration timeout) {
        final var result = new ArrayList<TestSseEvent>(count);
        for (int i = 0; i < count; i++) {
            try {
                final var e = queue.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
                assertThat(e)
                    .as("expected %d SSE events but only received %d within %s", count, result.size(), timeout)
                    .isNotNull();
                result.add(e);
            } catch (final InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("interrupted while waiting for SSE events", ex);
            }
        }
        return result;
    }

    /**
     * Blocks until the stream closes (server-side), then returns all received events.
     *
     * @param timeout the maximum time to wait for the stream to close
     * @return all events received before the stream closed
     */
    public List<TestSseEvent> collectAll(final Duration timeout) {
        try {
            done.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new AssertionError("SSE stream did not complete within " + timeout, e);
        }
        final var result = new ArrayList<TestSseEvent>();
        queue.drainTo(result);
        return result;
    }

    @Override
    public void close() {
        final var s = stream;
        if (s != null) {
            try {
                s.close();
            } catch (final IOException ignored) {
            }
        }
    }
}
