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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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

    private final PrintWriter writer;
    private final BufferedReader reader;
    private final PipedInputStream clientIn;
    private final Thread serverThread;
    private final Duration sendTimeout;
    private int nextId = 1;

    private McpStdioClient(final PrintWriter writer,
                           final BufferedReader reader,
                           final PipedInputStream clientIn,
                           final Thread serverThread,
                           final Duration sendTimeout) {
        this.writer = writer;
        this.reader = reader;
        this.clientIn = clientIn;
        this.serverThread = serverThread;
        this.sendTimeout = sendTimeout;
    }

    /**
     * Creates a client connected to the given server and starts its stdio loop
     * on a virtual thread.
     *
     * @param server the server to connect to
     * @return the connected client
     */
    public static McpStdioClient of(final McpServer server) {
        return of(server, DEFAULT_SEND_TIMEOUT);
    }

    /**
     * Creates a client with a custom send timeout.
     *
     * @param server      the server to connect to
     * @param sendTimeout how long to wait for each response before throwing
     * @return the connected client
     */
    public static McpStdioClient of(final McpServer server, final Duration sendTimeout) {
        try {
            final var clientToServer = new PipedOutputStream();
            final var serverToClient = new PipedOutputStream();
            final var serverIn = new PipedInputStream(clientToServer);
            final var clientIn = new PipedInputStream(serverToClient);

            final var thread = Thread.ofVirtual().start(
                () -> server.stdioLoop(serverIn, serverToClient));

            return new McpStdioClient(
                new PrintWriter(new OutputStreamWriter(clientToServer, StandardCharsets.UTF_8), true),
                new BufferedReader(new InputStreamReader(clientIn, StandardCharsets.UTF_8)),
                clientIn,
                thread,
                sendTimeout
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
                final var line = readLineWithTimeout();
                if (line == null) {
                    throw new IllegalStateException("Server closed stdout before responding to id " + sentId);
                }
                final var msg = Json.parse(line).asObject();
                final var responseId = msg.members().get("id");
                if (responseId == null || responseId instanceof JsonNull) {
                    continue; // notification — no id
                }
                if (idMatches(responseId, sentId)) {
                    return msg;
                }
            }
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Closes the client, signals EOF to the server, and waits for its stdio loop to exit.
     */
    @Override
    public void close() {
        writer.close();
        try {
            clientIn.close();
        } catch (final IOException ignored) {
        }
        try {
            serverThread.join(5_000);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private String readLineWithTimeout() throws IOException {
        final String[] lineRef = new String[1];
        final IOException[] errorRef = new IOException[1];
        final var latch = new CountDownLatch(1);
        Thread.ofVirtual().start(() -> {
            try {
                lineRef[0] = reader.readLine();
            } catch (final IOException e) {
                errorRef[0] = e;
            } finally {
                latch.countDown();
            }
        });
        try {
            if (!latch.await(sendTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                clientIn.close(); // unblock the stuck readLine() virtual thread
                throw new IOException("Server did not respond within " + sendTimeout);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            clientIn.close(); // unblock the stuck readLine() virtual thread
            throw new IOException("Interrupted waiting for server response", e);
        }
        if (errorRef[0] != null) {
            throw errorRef[0];
        }
        return lineRef[0];
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
}
