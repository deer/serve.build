package build.serve.lsp;

import build.serve.lsp.types.CompletionItem;
import build.serve.lsp.types.CompletionItemKind;
import build.serve.lsp.types.Hover;
import build.serve.lsp.types.ServerCapabilities;
import build.serve.lsp.types.ServerCapability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LspTransportTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void initializeTest() throws Exception {
        final var server = LspServer.builder()
            .onInitialize(params -> ServerCapabilities.of(ServerCapability.HOVER, ServerCapability.COMPLETION))
            .build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "initialize",
                """
                    {"rootUri":"file:///test","capabilities":{}}
                    """);

            assertThat(response.has("result")).isTrue();
            final var result = response.get("result");
            assertThat(result.path("hoverProvider").asBoolean()).isTrue();
            assertThat(result.path("completionProvider").asBoolean()).isTrue();
        }
    }

    @Test
    void initializedNotificationTest() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("initialized", "{}");
            // Send a request after to verify server is still running
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(response.has("result")).isTrue();
        }
    }

    @Test
    void hoverTest() throws Exception {
        final var server = LspServer.builder()
            .onHover((params, ctx) -> Hover.of("Hello **world**"))
            .build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/hover",
                """
                    {"textDocument":{"uri":"file:///test.java"},"position":{"line":0,"character":0}}
                    """);

            final var result = response.get("result");
            assertThat(result.path("contents").path("kind").asText()).isEqualTo("markdown");
            assertThat(result.path("contents").path("value").asText()).isEqualTo("Hello **world**");
        }
    }

    @Test
    void completionTest() throws Exception {
        final var server = LspServer.builder()
            .onCompletion((params, ctx) -> List.of(
                CompletionItem.of("println", CompletionItemKind.METHOD),
                CompletionItem.of("print", CompletionItemKind.METHOD)
            ))
            .build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/completion",
                """
                    {"textDocument":{"uri":"file:///test.java"},"position":{"line":0,"character":0}}
                    """);

            final var result = response.get("result");
            assertThat(result.isArray()).isTrue();
            assertThat(result.size()).isEqualTo(2);
            assertThat(result.get(0).path("label").asText()).isEqualTo("println");
        }
    }

    @Test
    void publishDiagnosticsTest() throws Exception {
        final var diagnosticsReceived = new CountDownLatch(1);
        final var receivedNotification = new AtomicReference<JsonNode>();

        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> {
                ctx.publishDiagnostics(params.textDocument().uri(), List.of(
                    build.serve.lsp.types.Diagnostic.error(
                        build.serve.lsp.types.Range.of(0, 0, 0, 5), "Syntax error")
                ));
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            // Send didOpen notification - handler will publish diagnostics
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            // Read the diagnostics notification from server
            final var notification = client.readMessage();
            assertThat(notification.path("method").asText()).isEqualTo("textDocument/publishDiagnostics");
            assertThat(notification.path("params").path("uri").asText()).isEqualTo("file:///test.java");
            assertThat(notification.path("params").path("diagnostics").size()).isEqualTo(1);
        }
    }

    @Test
    void shutdownExitTest() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(response.has("result")).isTrue();
            // After shutdown, we could send exit but that calls Runtime.halt()
            // so we just verify shutdown responded correctly
        }
    }

    @Test
    void shouldAcceptTcpConnectionAndHandleRequest() throws Exception {
        final var server = LspServer.builder()
            .onInitialize(params -> ServerCapabilities.of(ServerCapability.HOVER))
            .build();

        final int port;
        try (var s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        final var serverThread = Thread.ofVirtual().start(() -> {
            try {
                LspTransport.tcp(server, port);
            } catch (final IOException ignored) {
            }
        });

        Thread.sleep(100);

        try (var socket = new Socket("localhost", port)) {
            final var client = new LspTestClient(socket);
            final var response = client.sendRequest(1, "initialize",
                """
                    {"rootUri":"file:///test","capabilities":{}}
                    """);

            assertThat(response.path("result").path("hoverProvider").asBoolean()).isTrue();
        }

        serverThread.interrupt();
    }

    @Test
    void shouldHandleMultipleConcurrentTcpConnections() throws Exception {
        final var server = LspServer.builder()
            .onInitialize(params -> ServerCapabilities.of(ServerCapability.COMPLETION))
            .build();

        final int port;
        try (var s = new ServerSocket(0)) {
            port = s.getLocalPort();
        }

        final var serverThread = Thread.ofVirtual().start(() -> {
            try {
                LspTransport.tcp(server, port);
            } catch (final IOException ignored) {
            }
        });

        Thread.sleep(100);

        final var latch = new CountDownLatch(3);
        for (var i = 0; i < 3; i++) {
            final var id = i + 1;
            Thread.ofVirtual().start(() -> {
                try (var socket = new Socket("localhost", port)) {
                    final var client = new LspTestClient(socket);
                    final var response = client.sendRequest(id, "initialize",
                        """
                            {"rootUri":"file:///test","capabilities":{}}
                            """);
                    assertThat(response.path("result").path("completionProvider").asBoolean()).isTrue();
                    latch.countDown();
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        latch.await();
        serverThread.interrupt();
    }

    @Test
    void unknownMethodTest() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "custom/unknownMethod",
                """
                    {"foo":"bar"}
                    """);

            assertThat(response.has("error")).isTrue();
            assertThat(response.path("error").path("code").asInt()).isEqualTo(-32601);
            assertThat(response.path("error").path("message").asText()).isEqualTo("Method not found");
        }
    }

    /**
     * A test client that handles Content-Length framing for LSP messages.
     */
    static final class LspTestClient implements AutoCloseable {

        private final java.io.OutputStream out;
        private final BufferedReader clientReader;
        private final AutoCloseable closeable;
        private final Thread serverThread;

        LspTestClient(final LspServer server) throws IOException {
            final var clientToServer = new PipedOutputStream();
            final var serverToClient = new PipedOutputStream();
            final var serverIn = new PipedInputStream(clientToServer);
            final var clientIn = new PipedInputStream(serverToClient);
            this.out = clientToServer;
            this.clientReader = new BufferedReader(new InputStreamReader(clientIn, StandardCharsets.UTF_8));
            this.closeable = () -> {
                clientToServer.close();
                clientIn.close();
            };

            this.serverThread = Thread.ofVirtual().start(() -> {
                try {
                    LspTransport.run(server, serverIn, serverToClient);
                } catch (final Exception e) {
                    // Expected when streams close
                }
            });
        }

        LspTestClient(final Socket socket) throws IOException {
            this.out = socket.getOutputStream();
            this.clientReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.closeable = socket;
            this.serverThread = null;
        }

        JsonNode sendRequest(final int id, final String method, final String params) throws Exception {
            final var json = params != null
                ? String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}", id, method, params)
                : String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\"}", id, method);
            writeMessage(json);
            return readMessage();
        }

        void sendNotification(final String method, final String params) throws Exception {
            final var json = String.format("{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":%s}", method, params);
            writeMessage(json);
            // Small delay to let server process
            Thread.sleep(100);
        }

        JsonNode readMessage() throws Exception {
            final var contentLength = readContentLength();
            final var body = new char[contentLength];
            var read = 0;
            while (read < contentLength) {
                final var n = clientReader.read(body, read, contentLength - read);
                if (n < 0) {
                    throw new IOException("Unexpected end of stream");
                }
                read += n;
            }
            return MAPPER.readTree(new String(body));
        }

        private void writeMessage(final String json) throws IOException {
            final var bytes = json.getBytes(StandardCharsets.UTF_8);
            final var header = ("Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            this.out.write(header);
            this.out.write(bytes);
            this.out.flush();
        }

        private int readContentLength() throws IOException {
            var contentLength = -1;
            String line;
            while ((line = clientReader.readLine()) != null) {
                if (line.isEmpty()) {
                    break;
                }
                if (line.startsWith("Content-Length: ")) {
                    contentLength = Integer.parseInt(line.substring("Content-Length: ".length()).trim());
                }
            }
            return contentLength;
        }

        @Override
        public void close() throws Exception {
            this.closeable.close();
            if (this.serverThread != null) {
                this.serverThread.interrupt();
            }
        }
    }
}
