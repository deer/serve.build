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

import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.ShowMessageParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
        final var mapper = new ObjectMapper();
        final var shutdownRequested = new AtomicBoolean(false);
        final var lock = new Object();

        final LspContext ctx = new LspContext() {
            @Override
            public void publishDiagnostics(final String uri, final List<Diagnostic> diagnostics) {
                final var params = mapper.createObjectNode();
                params.put("uri", uri);
                params.set("diagnostics", mapper.valueToTree(diagnostics));
                sendNotification(mapper, out, lock, LspServerPushMethod.PUBLISH_DIAGNOSTICS, params);
            }

            @Override
            public void showMessage(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, LspServerPushMethod.SHOW_MESSAGE, node);
            }

            @Override
            public void logMessage(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, LspServerPushMethod.LOG_MESSAGE, node);
            }

            @Override
            public void showMessageRequest(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, LspServerPushMethod.SHOW_MESSAGE_REQUEST, node);
            }
        };

        final var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

        while (true) {
            final var contentLength = readContentLength(reader);
            if (contentLength < 0) {
                break;
            }

            final var body = new char[contentLength];
            var read = 0;
            while (read < contentLength) {
                final var n = reader.read(body, read, contentLength - read);
                if (n < 0) {
                    return;
                }
                read += n;
            }

            final var message = mapper.readTree(new String(body));
            final var methodStr = message.path("method").asText("");
            final var id = message.get("id");
            final var params = message.get("params");

            // Transport lifecycle — handled before typed dispatch
            if ("exit".equals(methodStr)) {
                Runtime.getRuntime().halt(shutdownRequested.get() ? 0 : 1);
                return;
            }
            if ("shutdown".equals(methodStr)) {
                shutdownRequested.set(true);
                sendResponse(mapper, out, lock, id, mapper.nullNode());
                continue;
            }

            if (id != null && !id.isNull()) {
                final var methodOpt = LspRequestMethod.from(methodStr);
                if (methodOpt.isEmpty()) {
                    sendErrorResponse(mapper, out, lock, id, -32601, "Method not found");
                    continue;
                }
                try {
                    final var request = LspRequest.parse(methodOpt.get(), mapper, params);
                    switch (server.handle(request, ctx)) {
                        case LspResponse.Ok ok -> sendResponse(mapper, out, lock, id, mapper.valueToTree(ok.value()));
                        case LspResponse.MethodNotFound __ ->
                            sendErrorResponse(mapper, out, lock, id, -32601, "Method not found");
                    }
                } catch (final Exception e) {
                    sendResponse(mapper, out, lock, id, mapper.nullNode());
                }
            } else {
                final var methodOpt = LspNotificationMethod.from(methodStr);
                if (methodOpt.isPresent()) {
                    try {
                        server.handle(LspNotification.parse(methodOpt.get(), mapper, params), ctx);
                    } catch (final Exception e) {
                        // silently ignore notification errors
                    }
                }
            }
        }
    }

    private static int readContentLength(final BufferedReader reader) throws IOException {
        var contentLength = -1;
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            if (line.startsWith("Content-Length: ")) {
                contentLength = Integer.parseInt(line.substring("Content-Length: ".length()).trim());
            }
        }
        return contentLength;
    }

    private static void sendResponse(final ObjectMapper mapper, final OutputStream out,
                                     final Object lock, final JsonNode id, final JsonNode result) {
        final var response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id.isNumber()) {
            response.put("id", id.asInt());
        } else {
            response.put("id", id.asText());
        }
        response.set("result", result);
        writeMessage(mapper, out, lock, response);
    }

    private static void sendErrorResponse(final ObjectMapper mapper, final OutputStream out,
                                          final Object lock, final JsonNode id, final int code, final String message) {
        final var response = mapper.createObjectNode();
        response.put("jsonrpc", "2.0");
        if (id.isNumber()) {
            response.put("id", id.asInt());
        } else {
            response.put("id", id.asText());
        }
        final var error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        response.set("error", error);
        writeMessage(mapper, out, lock, response);
    }

    private static void sendNotification(final ObjectMapper mapper, final OutputStream out,
                                         final Object lock, final LspServerPushMethod method,
                                         final ObjectNode params) {
        final var notification = mapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method.methodName);
        notification.set("params", params);
        writeMessage(mapper, out, lock, notification);
    }

    private static void writeMessage(final ObjectMapper mapper, final OutputStream out,
                                     final Object lock, final ObjectNode message) {
        try {
            final var json = mapper.writeValueAsBytes(message);
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
