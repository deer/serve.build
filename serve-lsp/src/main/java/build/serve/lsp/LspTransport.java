/*-
 * #%L
 * Serve LSP
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
package build.serve.lsp;

import build.base.json.Json;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.ShowMessageParams;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * LSP transport with Content-Length framing over stdio or custom streams.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class LspTransport {

    private LspTransport() {
    }

    /**
     * Creates a stdio transport that reads from System.in and writes to System.out.
     * Blocks the calling thread until shutdown.
     *
     * @param server the LSP server
     * @throws Exception if an error occurs
     */
    public static void stdio(final LspServer server) throws Exception {
        run(server, System.in, System.out);
    }

    /**
     * Creates a TCP transport that listens on the given port and handles each connection in a virtual thread.
     * Blocks the calling thread until the server socket is closed or an error occurs.
     *
     * @param server the LSP server
     * @param port   the port to listen on
     * @throws IOException if an error occurs opening or accepting on the server socket
     */
    public static void tcp(final LspServer server, final int port) throws IOException {
        try (var serverSocket = new java.net.ServerSocket(port)) {
            while (true) {
                final var socket = serverSocket.accept();
                Thread.ofVirtual().start(() -> {
                    try {
                        run(server, socket.getInputStream(), socket.getOutputStream());
                    } catch (final Exception ignored) {
                    } finally {
                        try {
                            socket.close();
                        } catch (final IOException ignored) {
                        }
                    }
                });
            }
        }
    }

    /**
     * Creates a transport from custom streams (useful for testing).
     *
     * @param server the LSP server
     * @param in     the input stream
     * @param out    the output stream
     * @throws Exception if an error occurs
     */
    public static void run(final LspServer server, final InputStream in, final OutputStream out) throws Exception {
        final var shutdownRequested = new AtomicBoolean(false);
        final var outboundId = new AtomicInteger(0);
        final var lock = new Object();

        final LspContext ctx = new LspContext() {
            @Override
            public void publishDiagnostics(final String uri, final List<Diagnostic> diagnostics) {
                final var params = JsonObject.builder()
                    .put("uri", uri)
                    .put("diagnostics", LspJson.toJson(diagnostics))
                    .build();
                sendNotification(out, lock, LspServerPushMethod.PUBLISH_DIAGNOSTICS, params);
            }

            @Override
            public void showMessage(final ShowMessageParams params) {
                sendNotification(out, lock, LspServerPushMethod.SHOW_MESSAGE, showMessageNode(params));
            }

            @Override
            public void logMessage(final ShowMessageParams params) {
                sendNotification(out, lock, LspServerPushMethod.LOG_MESSAGE, showMessageNode(params));
            }

            @Override
            public void showMessageRequest(final ShowMessageParams params) {
                sendOutboundRequest(out, lock, outboundId.incrementAndGet(),
                    "window/showMessageRequest", showMessageNode(params));
            }

            private JsonObject showMessageNode(final ShowMessageParams params) {
                return JsonObject.builder()
                    .put("type", params.type())
                    .put("message", params.message())
                    .build();
            }
        };

        while (true) {
            final var contentLength = readContentLength(in);
            if (contentLength < 0) {
                break;
            }

            final var body = in.readNBytes(contentLength);
            if (body.length < contentLength) {
                return;
            }

            final var message = Json.parse(body).asObject();
            final var methodStr = message.has("method") ? message.get("method").asString().value() : "";
            final var id = message.has("id") ? message.get("id") : null;
            final var params = message.has("params") ? message.get("params").asObject() : null;

            // Transport lifecycle — handled before typed dispatch
            if ("exit".equals(methodStr)) {
                Runtime.getRuntime().halt(shutdownRequested.get() ? 0 : 1);
                return;
            }
            if ("shutdown".equals(methodStr)) {
                shutdownRequested.set(true);
                sendResponse(out, lock, id, JsonNull.INSTANCE);
                continue;
            }

            if (id != null && !(id instanceof JsonNull)) {
                if (methodStr.isEmpty()) {
                    // Client response to a server-initiated request — ignore
                    continue;
                }
                final var methodOpt = LspRequestMethod.from(methodStr);
                if (methodOpt.isEmpty()) {
                    sendErrorResponse(out, lock, id, -32601, "Method not found");
                    continue;
                }
                try {
                    final var request = LspRequest.parse(methodOpt.get(), params);
                    switch (server.handle(request, ctx)) {
                        case LspResponse.Ok ok -> sendResponse(out, lock, id, LspJson.toJson(ok.value()));
                        case LspResponse.MethodNotFound __ ->
                            sendErrorResponse(out, lock, id, -32601, "Method not found");
                    }
                } catch (final Exception e) {
                    sendErrorResponse(out, lock, id, -32603, "Internal error");
                }
            } else {
                final var methodOpt = LspNotificationMethod.from(methodStr);
                if (methodOpt.isPresent()) {
                    try {
                        server.handle(LspNotification.parse(methodOpt.get(), params), ctx);
                    } catch (final Exception e) {
                        // silently ignore notification errors
                    }
                }
            }
        }
    }

    private static int readContentLength(final InputStream in) throws IOException {
        var contentLength = -1;
        final var lineBuffer = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) >= 0) {
            if (b == '\n') {
                final var line = lineBuffer.toString(StandardCharsets.US_ASCII).strip();
                lineBuffer.reset();
                if (line.isEmpty()) {
                    break;
                }
                if (line.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(line.substring("Content-Length: ".length()).trim());
                }
            } else if (b != '\r') {
                lineBuffer.write(b);
            }
        }
        return contentLength;
    }

    private static void sendResponse(final OutputStream out, final Object lock,
                                     final JsonValue id, final JsonValue result) {
        writeMessage(out, lock, responseBase(id).put("result", result).build());
    }

    private static void sendErrorResponse(final OutputStream out, final Object lock,
                                          final JsonValue id, final int code, final String message) {
        final var error = JsonObject.builder().put("code", code).put("message", message).build();
        writeMessage(out, lock, responseBase(id).put("error", error).build());
    }

    private static JsonObject.Builder responseBase(final JsonValue id) {
        final var b = JsonObject.builder().put("jsonrpc", "2.0");
        if (id instanceof JsonNumber n) {
            b.put("id", n.toNumber());
        } else if (id instanceof JsonString s) {
            b.put("id", s.value());
        } else {
            b.put("id", JsonNull.INSTANCE);
        }
        return b;
    }

    private static void sendNotification(final OutputStream out, final Object lock,
                                         final LspServerPushMethod method, final JsonObject params) {
        writeMessage(out, lock, JsonObject.builder()
            .put("jsonrpc", "2.0")
            .put("method", method.methodName)
            .put("params", params)
            .build());
    }

    private static void sendOutboundRequest(final OutputStream out, final Object lock,
                                            final int id, final String method, final JsonObject params) {
        writeMessage(out, lock, JsonObject.builder()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method)
            .put("params", params)
            .build());
    }

    private static void writeMessage(final OutputStream out, final Object lock, final JsonObject message) {
        try {
            final var json = message.toJsonString().getBytes(StandardCharsets.UTF_8);
            final var header = ("Content-Length: " + json.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            synchronized (lock) {
                out.write(header);
                out.write(json);
                out.flush();
            }
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }
}
