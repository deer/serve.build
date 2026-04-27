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

import build.serve.lsp.params.CodeActionParams;
import build.serve.lsp.params.DidChangeParams;
import build.serve.lsp.params.DidCloseParams;
import build.serve.lsp.params.DidOpenParams;
import build.serve.lsp.params.DidSaveParams;
import build.serve.lsp.params.DocumentSymbolParams;
import build.serve.lsp.params.ExecuteCommandParams;
import build.serve.lsp.params.FoldingRangeParams;
import build.serve.lsp.params.FormattingParams;
import build.serve.lsp.params.InitializeParams;
import build.serve.lsp.params.InlayHintParams;
import build.serve.lsp.params.RangeFormattingParams;
import build.serve.lsp.params.ReferenceParams;
import build.serve.lsp.params.RenameParams;
import build.serve.lsp.params.SelectionRangeParams;
import build.serve.lsp.params.TextDocumentPositionParams;
import build.serve.lsp.params.WorkspaceSymbolParams;
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
                sendNotification(mapper, out, lock, "textDocument/publishDiagnostics", params);
            }

            @Override
            public void showMessage(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, "window/showMessage", node);
            }

            @Override
            public void logMessage(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, "window/logMessage", node);
            }

            @Override
            public void showMessageRequest(final ShowMessageParams params) {
                final var node = mapper.createObjectNode();
                node.put("type", params.type());
                node.put("message", params.message());
                sendNotification(mapper, out, lock, "window/showMessageRequest", node);
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
            final var method = message.path("method").asText("");
            final var id = message.get("id");
            final var params = message.get("params");

            if ("exit".equals(method)) {
                final var code = shutdownRequested.get() ? 0 : 1;
                Runtime.getRuntime().halt(code);
                return;
            }

            if ("shutdown".equals(method)) {
                shutdownRequested.set(true);
                sendResponse(mapper, out, lock, id, mapper.nullNode());
                continue;
            }

            if (id != null && !id.isNull()) {
                // Request
                final var result = dispatchRequest(server, mapper, method, params, ctx);
                if ("METHOD_NOT_FOUND".equals(result)) {
                    sendErrorResponse(mapper, out, lock, id, -32601, "Method not found");
                } else if (result != null) {
                    sendResponse(mapper, out, lock, id, mapper.valueToTree(result));
                } else {
                    sendResponse(mapper, out, lock, id, mapper.nullNode());
                }
            } else {
                // Notification
                dispatchNotification(server, mapper, method, params, ctx);
            }
        }
    }

    private static Object dispatchRequest(final LspServer server, final ObjectMapper mapper,
                                          final String method, final JsonNode params, final LspContext ctx) {
        try {
            return switch (method) {
                case "initialize" -> {
                    if (server.onInitialize != null) {
                        final var p = mapper.treeToValue(params, InitializeParams.class);
                        yield server.onInitialize.apply(p);
                    }
                    yield null;
                }
                case "textDocument/hover" -> {
                    if (server.onHover != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onHover.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/completion" -> {
                    if (server.onCompletion != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onCompletion.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/definition" -> {
                    if (server.onDefinition != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onDefinition.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/declaration" -> {
                    if (server.onDeclaration != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onDeclaration.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/typeDefinition" -> {
                    if (server.onTypeDefinition != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onTypeDefinition.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/implementation" -> {
                    if (server.onImplementation != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onImplementation.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/references" -> {
                    if (server.onReferences != null) {
                        final var p = mapper.treeToValue(params, ReferenceParams.class);
                        yield server.onReferences.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/documentHighlight" -> {
                    if (server.onDocumentHighlight != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onDocumentHighlight.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/documentSymbol" -> {
                    if (server.onDocumentSymbol != null) {
                        final var p = mapper.treeToValue(params, DocumentSymbolParams.class);
                        yield server.onDocumentSymbol.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/codeAction" -> {
                    if (server.onCodeAction != null) {
                        final var p = mapper.treeToValue(params, CodeActionParams.class);
                        yield server.onCodeAction.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/signatureHelp" -> {
                    if (server.onSignatureHelp != null) {
                        final var p = mapper.treeToValue(params, TextDocumentPositionParams.class);
                        yield server.onSignatureHelp.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/rename" -> {
                    if (server.onRename != null) {
                        final var p = mapper.treeToValue(params, RenameParams.class);
                        yield server.onRename.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/formatting" -> {
                    if (server.onFormatting != null) {
                        final var p = mapper.treeToValue(params, FormattingParams.class);
                        yield server.onFormatting.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/rangeFormatting" -> {
                    if (server.onRangeFormatting != null) {
                        final var p = mapper.treeToValue(params, RangeFormattingParams.class);
                        yield server.onRangeFormatting.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/foldingRange" -> {
                    if (server.onFoldingRange != null) {
                        final var p = mapper.treeToValue(params, FoldingRangeParams.class);
                        yield server.onFoldingRange.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/selectionRange" -> {
                    if (server.onSelectionRange != null) {
                        final var p = mapper.treeToValue(params, SelectionRangeParams.class);
                        yield server.onSelectionRange.apply(p, ctx);
                    }
                    yield null;
                }
                case "textDocument/inlayHint" -> {
                    if (server.onInlayHint != null) {
                        final var p = mapper.treeToValue(params, InlayHintParams.class);
                        yield server.onInlayHint.apply(p, ctx);
                    }
                    yield null;
                }
                case "workspace/symbol" -> {
                    if (server.onWorkspaceSymbol != null) {
                        final var p = mapper.treeToValue(params, WorkspaceSymbolParams.class);
                        yield server.onWorkspaceSymbol.apply(p, ctx);
                    }
                    yield null;
                }
                case "workspace/executeCommand" -> {
                    if (server.onExecuteCommand != null) {
                        final var p = mapper.treeToValue(params, ExecuteCommandParams.class);
                        yield server.onExecuteCommand.apply(p, ctx);
                    }
                    yield null;
                }
                default -> "METHOD_NOT_FOUND";
            };
        } catch (final Exception e) {
            return null;
        }
    }

    private static void dispatchNotification(final LspServer server, final ObjectMapper mapper,
                                             final String method, final JsonNode params, final LspContext ctx) {
        try {
            switch (method) {
                case "initialized" -> { /* no-op */ }
                case "textDocument/didOpen" -> {
                    if (server.onDidOpen != null) {
                        final var p = mapper.treeToValue(params, DidOpenParams.class);
                        server.onDidOpen.accept(p, ctx);
                    }
                }
                case "textDocument/didChange" -> {
                    if (server.onDidChange != null) {
                        final var p = mapper.treeToValue(params, DidChangeParams.class);
                        server.onDidChange.accept(p, ctx);
                    }
                }
                case "textDocument/didClose" -> {
                    if (server.onDidClose != null) {
                        final var p = mapper.treeToValue(params, DidCloseParams.class);
                        server.onDidClose.accept(p, ctx);
                    }
                }
                case "textDocument/didSave" -> {
                    if (server.onDidSave != null) {
                        final var p = mapper.treeToValue(params, DidSaveParams.class);
                        server.onDidSave.accept(p, ctx);
                    }
                }
                default -> { /* silently ignore unknown notifications */ }
            }
        } catch (final Exception e) {
            // Silently ignore notification errors
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
                                         final Object lock, final String method, final ObjectNode params) {
        final var notification = mapper.createObjectNode();
        notification.put("jsonrpc", "2.0");
        notification.put("method", method);
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
