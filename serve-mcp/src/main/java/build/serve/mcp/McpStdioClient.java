package build.serve.mcp;

/*-
 * #%L
 * Serve MCP
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

import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonBoolean;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A client that drives an {@link McpServer} over its stdio transport.
 *
 * <p>Spins up a virtual thread running {@link McpServer#stdioLoop} and communicates
 * with it via piped streams. Useful for integration tests and local tool invocation
 * without any HTTP plumbing.
 *
 * <pre>{@code
 * try (var client = McpStdioClient.of(server)) {
 *     var result = client.call("my_tool", Map.of("arg", "value"));
 *     var data   = result.json().asObject();
 * }
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public final class McpStdioClient implements AutoCloseable {

    private static final Duration DEFAULT_SEND_TIMEOUT = Duration.ofSeconds(30);
    private static final Consumer<JsonObject> NO_OP_NOTIFICATION_HANDLER = n -> {
    };

    private final PrintWriter writer;
    private final LineQueue lineQueue;
    private final Thread serverThread;
    private final Duration sendTimeout;
    private final Consumer<JsonObject> notificationHandler;
    private int nextId = 1;

    private McpStdioClient(final PrintWriter writer,
                           final LineQueue lineQueue,
                           final Thread serverThread,
                           final Duration sendTimeout,
                           final Consumer<JsonObject> notificationHandler) {
        this.writer = writer;
        this.lineQueue = lineQueue;
        this.serverThread = serverThread;
        this.sendTimeout = sendTimeout;
        this.notificationHandler = notificationHandler;
    }

    /**
     * Creates a client connected to the given server and starts its stdio loop
     * on a virtual thread.
     *
     * @param server the server to connect to
     * @return the connected client
     */
    public static McpStdioClient of(final McpServer server) {
        return of(server, DEFAULT_SEND_TIMEOUT, NO_OP_NOTIFICATION_HANDLER);
    }

    /**
     * Creates a client with a custom send timeout.
     *
     * @param server      the server to connect to
     * @param sendTimeout how long to wait for each response before throwing
     * @return the connected client
     */
    public static McpStdioClient of(final McpServer server, final Duration sendTimeout) {
        return of(server, sendTimeout, NO_OP_NOTIFICATION_HANDLER);
    }

    /**
     * Creates a client with a custom send timeout and a handler for server-pushed notifications.
     *
     * <p>The handler is called (on the calling thread, inside {@link #send}) for each
     * server-pushed notification received before the matching response — for example,
     * {@code notifications/progress} messages from a long-running tool call.
     *
     * @param server              the server to connect to
     * @param sendTimeout         how long to wait for each response before throwing
     * @param notificationHandler called for each server-pushed notification (no {@code id} field)
     * @return the connected client
     */
    public static McpStdioClient of(final McpServer server,
                                    final Duration sendTimeout,
                                    final Consumer<JsonObject> notificationHandler) {
        try {
            final var clientToServer = new PipedOutputStream();
            final var serverIn = new PipedInputStream(clientToServer);
            final var lineQueue = new LineQueue();

            final var thread = Thread.ofVirtual().start(
                () -> server.stdioLoop(serverIn, lineQueue));

            return new McpStdioClient(
                new PrintWriter(new OutputStreamWriter(clientToServer, StandardCharsets.UTF_8), true),
                lineQueue,
                thread,
                sendTimeout,
                notificationHandler
            );
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Calls the named tool and returns the deserialized {@link McpToolResult}.
     *
     * @param toolName  the tool name
     * @param arguments the arguments to pass
     * @return the tool result
     */
    public McpToolResult call(final String toolName, final Map<String, Object> arguments) {
        final var params = Map.<String, Object>of("name", toolName, "arguments", arguments);
        final var response = send("tools/call", params);
        return McpToolResult.fromJson(response.get("result").asObject());
    }

    /**
     * Sends a raw JSON-RPC request and returns the full response object.
     * Skips any server-pushed notifications (no {@code id} field) that arrive before
     * the matching response. Throws if no response arrives within the send timeout.
     *
     * @param method the JSON-RPC method name
     * @param params the parameters
     * @return the full response envelope
     */
    public JsonObject send(final String method, final Map<String, Object> params) {
        final int sentId = nextId++;
        writer.println(rpc(method, sentId, params));
        try {
            while (true) {
                final var line = lineQueue.poll(sendTimeout);
                if (line == null) {
                    throw new UncheckedIOException(new IOException("Server did not respond within " + sendTimeout));
                }
                final var msg = Json.parse(line).asObject();
                final var responseId = msg.members().get("id");
                if (responseId == null || responseId instanceof JsonNull) {
                    notificationHandler.accept(msg);
                    continue;
                }
                if (idMatches(responseId, sentId)) {
                    return msg;
                }
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedIOException(new IOException("Interrupted waiting for server response", e));
        }
    }

    /**
     * Closes the client, signals EOF to the server, and waits for its stdio loop to exit.
     */
    @Override
    public void close() {
        writer.close();
        try {
            serverThread.join(5_000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean idMatches(final JsonValue responseId, final int sentId) {
        if (responseId instanceof JsonNumber n) {
            return n.toNumber().intValue() == sentId;
        }
        if (responseId instanceof JsonString s) {
            return s.value().equals(String.valueOf(sentId));
        }
        return false;
    }

    private static String rpc(final String method, final int id, final Map<String, Object> params) {
        return JsonObject.builder()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", toJsonValue(params))
            .build()
            .toJsonString();
    }

    @SuppressWarnings("unchecked")
    private static JsonValue toJsonValue(final Object value) {
        if (value == null) {
            return JsonNull.INSTANCE;
        }
        if (value instanceof String s) {
            return JsonString.of(s);
        }
        if (value instanceof Number n) {
            return JsonNumber.of(n);
        }
        if (value instanceof Boolean b) {
            return JsonBoolean.of(b);
        }
        if (value instanceof Map<?, ?> m) {
            final var builder = JsonObject.builder();
            for (final var entry : m.entrySet()) {
                builder.put((String) entry.getKey(), toJsonValue(entry.getValue()));
            }
            return builder.build();
        }
        if (value instanceof List<?> l) {
            final var builder = JsonArray.builder();
            for (final var item : l) {
                builder.add(toJsonValue(item));
            }
            return builder.build();
        }
        throw new IllegalArgumentException("Unsupported type: " + value.getClass());
    }

    /**
     * An {@link OutputStream} that accumulates bytes and enqueues complete lines
     * (delimited by {@code \n}) into a {@link LinkedBlockingQueue}.
     *
     * <p>The server's {@code mcp-stdio-writer} virtual thread writes to this directly,
     * bypassing {@link java.io.PipedInputStream} and its {@code synchronized} monitor.
     * Callers read via {@link #poll}, which uses {@link java.util.concurrent.locks.LockSupport#park}
     * and is safe to call from virtual threads without pinning a carrier.
     */
    private static final class LineQueue extends OutputStream {

        private final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();
        private final StringBuilder current = new StringBuilder();

        @Override
        public void write(final int b) {
            if (b == '\n') {
                lines.offer(current.toString());
                current.setLength(0);
            } else if (b != '\r') {
                current.append((char) b);
            }
        }

        @Override
        public void write(final byte[] b, final int off, final int len) {
            for (int i = off; i < off + len; i++) {
                write(b[i] & 0xFF);
            }
        }

        String poll(final Duration timeout) throws InterruptedException {
            return lines.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
