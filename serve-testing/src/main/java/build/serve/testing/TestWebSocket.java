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

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A WebSocket test client with blocking receive methods.
 *
 * <p>Wraps the JDK {@link WebSocket} client with a queue-based message inbox, so test code
 * can simply call {@link #nextText()} or {@link #nextBinary()} without managing futures or
 * latches directly.</p>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class TestWebSocket implements AutoCloseable {

    private sealed interface Frame {
        record Text(String data) implements Frame {
        }

        record Binary(byte[] data) implements Frame {
        }
    }

    private final WebSocket ws;
    private final BlockingQueue<Frame> inbox;
    private final CompletableFuture<Void> closed;

    private TestWebSocket(final WebSocket ws,
                          final BlockingQueue<Frame> inbox,
                          final CompletableFuture<Void> closed) {
        this.ws = ws;
        this.inbox = inbox;
        this.closed = closed;
    }

    static TestWebSocket connect(final URI wsUri, final Map<String, String> headers) {
        final var inbox = new LinkedBlockingQueue<Frame>();
        final var closed = new CompletableFuture<Void>();
        final var textBuffer = new StringBuilder();
        final var binaryBuffer = new ByteArrayOutputStream();

        final var client = HttpClient.newHttpClient();
        final var builder = client.newWebSocketBuilder();
        headers.forEach(builder::header);

        final var ws = builder.buildAsync(wsUri, new WebSocket.Listener() {
            @Override
            public CompletionStage<?> onText(final WebSocket ws, final CharSequence data, final boolean last) {
                textBuffer.append(data);
                if (last) {
                    inbox.add(new Frame.Text(textBuffer.toString()));
                    textBuffer.setLength(0);
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onBinary(final WebSocket ws, final ByteBuffer data, final boolean last) {
                final var bytes = new byte[data.remaining()];
                data.get(bytes);
                binaryBuffer.write(bytes, 0, bytes.length);
                if (last) {
                    inbox.add(new Frame.Binary(binaryBuffer.toByteArray()));
                    binaryBuffer.reset();
                }
                ws.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(final WebSocket ws, final int statusCode, final String reason) {
                closed.complete(null);
                return null;
            }
        }).join();

        return new TestWebSocket(ws, inbox, closed);
    }

    /**
     * Sends a text message to the server.
     *
     * @param message the message to send
     * @return this client for chaining
     */
    public TestWebSocket sendText(final String message) {
        ws.sendText(message, true).join();
        return this;
    }

    /**
     * Sends a binary message to the server.
     *
     * @param data the bytes to send
     * @return this client for chaining
     */
    public TestWebSocket sendBinary(final byte[] data) {
        ws.sendBinary(ByteBuffer.wrap(data), true).join();
        return this;
    }

    /**
     * Blocks until a text message arrives (5-second timeout).
     *
     * @return the message text
     */
    public String nextText() {
        return nextText(Duration.ofSeconds(5));
    }

    /**
     * Blocks until a text message arrives or the timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @return the message text
     */
    public String nextText(final Duration timeout) {
        final var frame = poll(timeout);
        assertThat(frame).as("expected a text frame but got binary").isInstanceOf(Frame.Text.class);
        return ((Frame.Text) frame).data();
    }

    /**
     * Blocks until a binary message arrives (5-second timeout).
     *
     * @return the message bytes
     */
    public byte[] nextBinary() {
        return nextBinary(Duration.ofSeconds(5));
    }

    /**
     * Blocks until a binary message arrives or the timeout elapses.
     *
     * @param timeout the maximum time to wait
     * @return the message bytes
     */
    public byte[] nextBinary(final Duration timeout) {
        final var frame = poll(timeout);
        assertThat(frame).as("expected a binary frame but got text").isInstanceOf(Frame.Binary.class);
        return ((Frame.Binary) frame).data();
    }

    /**
     * Asserts that the server closes the connection within 5 seconds.
     *
     * @return this client for chaining
     */
    public TestWebSocket assertClosed() {
        return assertClosed(Duration.ofSeconds(5));
    }

    /**
     * Asserts that the server closes the connection within the given timeout.
     *
     * @param timeout the maximum time to wait
     * @return this client for chaining
     */
    public TestWebSocket assertClosed(final Duration timeout) {
        try {
            closed.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (final Exception e) {
            throw new AssertionError("WebSocket was not closed within " + timeout, e);
        }
        return this;
    }

    @Override
    public void close() {
        try {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "").join();
        } catch (final Exception ignored) {
        }
    }

    private Frame poll(final Duration timeout) {
        try {
            final var frame = inbox.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            assertThat(frame).as("expected a WebSocket frame within %s but received none", timeout)
                .isNotNull();
            return frame;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("interrupted while waiting for WebSocket frame", e);
        }
    }
}
