package build.serve.lsp;

import build.base.json.Json;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.serve.lsp.types.ApplyWorkspaceEditResult;
import build.serve.lsp.types.CompletionItem;
import build.serve.lsp.types.CompletionItemKind;
import build.serve.lsp.types.ConfigurationItem;
import build.serve.lsp.types.Hover;
import build.serve.lsp.types.Range;
import build.serve.lsp.types.ServerCapabilities;
import build.serve.lsp.types.ServerCapability;
import build.serve.lsp.types.ShowMessageParams;
import build.serve.lsp.types.TextEdit;
import build.serve.lsp.types.WorkDoneProgress;
import build.serve.lsp.types.WorkspaceEdit;
import build.serve.lsp.types.WorkspaceFolder;
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
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class LspTransportTests {

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
            final var result = response.get("result").asObject();
            assertThat(result.get("hoverProvider").asBoolean().value()).isTrue();
            assertThat(result.get("completionProvider").asBoolean().value()).isTrue();
        }
    }

    @Test
    void initializedNotificationTest() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("initialized", "{}");
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

            final var result = response.get("result").asObject();
            assertThat(result.get("contents").asObject().get("kind").asString().value()).isEqualTo("markdown");
            assertThat(result.get("contents").asObject().get("value").asString().value()).isEqualTo("Hello **world**");
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

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(2);
            assertThat(result.values().get(0).asObject().get("label").asString().value()).isEqualTo("println");
        }
    }

    @Test
    void publishDiagnosticsTest() throws Exception {
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> {
                ctx.publishDiagnostics(params.textDocument().uri(), List.of(
                    build.serve.lsp.types.Diagnostic.error(
                        build.serve.lsp.types.Range.of(0, 0, 0, 5), "Syntax error")
                ));
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var notification = client.readMessage();
            assertThat(notification.get("method").asString().value()).isEqualTo("textDocument/publishDiagnostics");
            assertThat(notification.get("params").asObject().get("uri").asString().value()).isEqualTo("file:///test.java");
            assertThat(notification.get("params").asObject().get("diagnostics").asArray().values()).hasSize(1);
        }
    }

    @Test
    void shouldCorrelateOutboundRequestWithClientResult() throws Exception {
        final var resultRef = new AtomicReference<JsonValue>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) ->
                ctx.sendRequest("workspace/applyEdit", JsonObject.builder().put("label", "test").build())
                    .thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("method").asString().value()).isEqualTo("workspace/applyEdit");
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"result\":{\"applied\":true}}");

            // round-trip a follow-up request to guarantee the server has processed the response above,
            // since the server thread reads messages from the same stream in order
            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get().asObject().get("applied").asBoolean().value()).isTrue();
        }
    }

    @Test
    void shouldFailFutureWhenClientRespondsToOutboundRequestWithError() throws Exception {
        final var errorRef = new AtomicReference<Throwable>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) ->
                ctx.sendRequest("workspace/applyEdit", JsonObject.builder().build())
                    .whenComplete((result, error) -> errorRef.set(error)))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"error\":{\"code\":-32000,\"message\":\"declined\"}}");

            // round-trip a follow-up request to guarantee the server has processed the response above,
            // since the server thread reads messages from the same stream in order
            client.sendRequest(99, "shutdown", null);
            assertThat(errorRef.get()).isInstanceOf(LspClientErrorException.class);
            final var error = (LspClientErrorException) errorRef.get();
            assertThat(error.code()).isEqualTo(-32000);
            assertThat(error.getMessage()).isEqualTo("declined");
        }
    }

    @Test
    void shouldApplyWorkspaceEditWithoutLabel() throws Exception {
        final var resultRef = new AtomicReference<ApplyWorkspaceEditResult>();
        final var edit = new WorkspaceEdit(Map.of("file:///test.java",
            List.of(new TextEdit(Range.of(0, 0, 0, 5), "hello"))));
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.applyEdit(edit).thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("method").asString().value()).isEqualTo("workspace/applyEdit");
            final var params = outboundRequest.get("params").asObject();
            assertThat(params.has("label")).isFalse();
            assertThat(params.get("edit").asObject().get("changes").asObject().has("file:///test.java")).isTrue();
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"result\":{\"applied\":true}}");

            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get().applied()).isTrue();
            assertThat(resultRef.get().failureReason()).isNull();
        }
    }

    @Test
    void shouldApplyWorkspaceEditWithLabelAndReportFailure() throws Exception {
        final var resultRef = new AtomicReference<ApplyWorkspaceEditResult>();
        final var edit = new WorkspaceEdit(Map.of());
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.applyEdit("rename foo to bar", edit).thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("params").asObject().get("label").asString().value())
                .isEqualTo("rename foo to bar");
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":" + outboundId
                    + ",\"result\":{\"applied\":false,\"failureReason\":\"stale document\"}}");

            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get().applied()).isFalse();
            assertThat(resultRef.get().failureReason()).isEqualTo("stale document");
        }
    }

    @Test
    void shouldRequestConfiguration() throws Exception {
        final var resultRef = new AtomicReference<List<JsonValue>>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.configuration(List.of(new ConfigurationItem(null, "myServer.trace")))
                .thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("method").asString().value()).isEqualTo("workspace/configuration");
            final var items = outboundRequest.get("params").asObject().get("items").asArray().values();
            assertThat(items).hasSize(1);
            assertThat(items.get(0).asObject().get("section").asString().value()).isEqualTo("myServer.trace");
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"result\":[\"verbose\"]}");

            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get()).hasSize(1);
            assertThat(resultRef.get().get(0).asString().value()).isEqualTo("verbose");
        }
    }

    @Test
    void shouldRequestWorkspaceFolders() throws Exception {
        final var resultRef = new AtomicReference<List<WorkspaceFolder>>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.workspaceFolders().thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("method").asString().value()).isEqualTo("workspace/workspaceFolders");
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"id\":" + outboundId
                + ",\"result\":[{\"uri\":\"file:///proj\",\"name\":\"proj\"}]}");

            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get()).containsExactly(new WorkspaceFolder("file:///proj", "proj"));
        }
    }

    @Test
    void shouldReturnEmptyListWhenClientHasNoWorkspaceFolders() throws Exception {
        final var resultRef = new AtomicReference<List<WorkspaceFolder>>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.workspaceFolders().thenAccept(resultRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"result\":null}");

            client.sendRequest(99, "shutdown", null);
            assertThat(resultRef.get()).isEmpty();
        }
    }

    @Test
    void shutdownExitTest() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(response.has("result")).isTrue();
        }
    }

    @Test
    void shouldInvokeShutdownHookBeforeRespondingToShutdown() throws Exception {
        final var hookRan = new AtomicBoolean(false);
        final var server = LspServer.builder()
            .onShutdown(ctx -> hookRan.set(true))
            .build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(hookRan.get()).isTrue();
            assertThat(response.has("result")).isTrue();
        }
    }

    @Test
    void shouldPassUsableContextToShutdownHook() throws Exception {
        final var server = LspServer.builder()
            .onShutdown(ctx -> ctx.logMessage(new ShowMessageParams(1, "shutting down")))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.writeMessage("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"shutdown\"}");

            final var notification = client.readMessage();
            assertThat(notification.get("method").asString().value()).isEqualTo("window/logMessage");

            final var response = client.readMessage();
            assertThat(response.has("result")).isTrue();
        }
    }

    @Test
    void shouldStillRespondToShutdownWhenHookThrows() throws Exception {
        final var server = LspServer.builder()
            .onShutdown(ctx -> {
                throw new RuntimeException("boom");
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(response.has("result")).isTrue();
            assertThat(response.has("error")).isFalse();
        }
    }

    @Test
    void shouldRespondToShutdownWithoutHookRegistered() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequest(1, "shutdown", null);
            assertThat(response.has("result")).isTrue();
        }
    }

    @Test
    void shouldCloseConnectionWhenContentLengthExceedsCap() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            client.writeHeaderOnly(Integer.MAX_VALUE);

            assertThat(client.awaitServerThreadDone(2000)).isTrue();
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

            assertThat(response.get("result").asObject().get("hoverProvider").asBoolean().value()).isTrue();
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
                    assertThat(response.get("result").asObject().get("completionProvider").asBoolean().value()).isTrue();
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
            assertThat(response.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32601);
            assertThat(response.get("error").asObject().get("message").asString().value()).isEqualTo("Method not found");
        }
    }

    @Test
    void shouldEchoStringIdInResponse() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTestClient(server)) {
            final var response = client.sendRequestWithStringId("req-abc", "textDocument/hover",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":0,\"character\":0}}");

            assertThat(response.getString("id")).isEqualTo("req-abc");
            assertThat(response.has("error")).isFalse();
        }
    }

    @Test
    void shouldProcessSecondRequestWhileFirstIsStillBlocked() throws Exception {
        final var release = new CountDownLatch(1);
        final var server = LspServer.builder()
            .onHover((params, ctx) -> {
                try {
                    release.await();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Hover.of("first");
            })
            .onCompletion((params, ctx) -> List.of(CompletionItem.of("second", CompletionItemKind.METHOD)))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"textDocument/hover\","
                    + "\"params\":{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":0,\"character\":0}}}");
            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"textDocument/completion\","
                    + "\"params\":{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":0,\"character\":0}}}");

            final var second = client.readMessage();
            assertThat(second.get("id").asNumber().toNumber().intValue()).isEqualTo(2);

            release.countDown();
            final var first = client.readMessage();
            assertThat(first.get("id").asNumber().toNumber().intValue()).isEqualTo(1);
        }
    }

    @Test
    void shouldRespondWithRequestCancelledWhenClientCancelsInFlightRequest() throws Exception {
        final var started = new CountDownLatch(1);
        final var server = LspServer.builder()
            .onHover((params, ctx) -> {
                started.countDown();
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return Hover.of("should not be delivered");
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"textDocument/hover\","
                    + "\"params\":{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":0,\"character\":0}}}");

            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"method\":\"$/cancelRequest\",\"params\":{\"id\":1}}");

            final var response = client.readMessage();
            assertThat(response.has("error")).isTrue();
            assertThat(response.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32800);
        }
    }

    @Test
    void shouldRespondWithRequestCancelledWhenHandlerLetsInterruptedExceptionEscapeUnwrapped() throws Exception {
        final var started = new CountDownLatch(1);
        final var server = LspServer.builder()
            .onHover((params, ctx) -> {
                started.countDown();
                sleepPropagatingInterrupt(5000);
                return Hover.of("should not be delivered");
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            client.writeMessage(
                "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"textDocument/hover\","
                    + "\"params\":{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":0,\"character\":0}}}");

            assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"method\":\"$/cancelRequest\",\"params\":{\"id\":1}}");

            final var response = client.readMessage();
            assertThat(response.has("error")).isTrue();
            assertThat(response.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32800);
        }
    }

    /**
     * Sleeps, letting a caught {@link InterruptedException} escape unwrapped and without
     * re-interrupting the thread — simulating a handler whose blocking call's interruption isn't
     * handled specially, rather than the well-behaved catch-and-reinterrupt idiom.
     */
    private static void sleepPropagatingInterrupt(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            sneakyThrow(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(final Throwable t) throws T {
        throw (T) t;
    }

    @Test
    void shouldIgnoreCancelRequestForUnknownId() throws Exception {
        final var server = LspServer.builder()
            .onHover((params, ctx) -> Hover.of("hi"))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.writeMessage("{\"jsonrpc\":\"2.0\",\"method\":\"$/cancelRequest\",\"params\":{\"id\":999}}");

            final var response = client.sendRequest(1, "textDocument/hover",
                """
                    {"textDocument":{"uri":"file:///test.java"},"position":{"line":0,"character":0}}
                    """);

            assertThat(response.has("error")).isFalse();
            assertThat(response.get("result").asObject().get("contents").asObject().get("value").asString().value())
                .isEqualTo("hi");
        }
    }

    @Test
    void shouldSendProgressNotifications() throws Exception {
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> {
                ctx.progress(JsonString.of("tok-1"), WorkDoneProgress.Begin.of("Indexing"));
                ctx.progress(JsonString.of("tok-1"), WorkDoneProgress.Report.of(50));
                ctx.progress(JsonString.of("tok-1"), WorkDoneProgress.End.of());
            })
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var begin = client.readMessage();
            assertThat(begin.get("method").asString().value()).isEqualTo("$/progress");
            assertThat(begin.get("params").asObject().get("token").asString().value()).isEqualTo("tok-1");
            final var beginValue = begin.get("params").asObject().get("value").asObject();
            assertThat(beginValue.get("kind").asString().value()).isEqualTo("begin");
            assertThat(beginValue.get("title").asString().value()).isEqualTo("Indexing");

            final var report = client.readMessage();
            final var reportValue = report.get("params").asObject().get("value").asObject();
            assertThat(reportValue.get("kind").asString().value()).isEqualTo("report");
            assertThat(reportValue.get("percentage").asNumber().toNumber().intValue()).isEqualTo(50);

            final var end = client.readMessage();
            assertThat(end.get("params").asObject().get("value").asObject().get("kind").asString().value())
                .isEqualTo("end");
        }
    }

    @Test
    void shouldCreateWorkDoneProgressAndReturnToken() throws Exception {
        final var tokenRef = new AtomicReference<JsonValue>();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> ctx.createWorkDoneProgress().thenAccept(tokenRef::set))
            .build();

        try (final var client = new LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                """
                    {"textDocument":{"uri":"file:///test.java","languageId":"java","version":1,"text":"hello"}}
                    """);

            final var outboundRequest = client.readMessage();
            assertThat(outboundRequest.get("method").asString().value()).isEqualTo("window/workDoneProgress/create");
            final var requestedToken = outboundRequest.get("params").asObject().get("token").asString().value();
            final var outboundId = outboundRequest.get("id").asNumber().toNumber().intValue();

            client.writeMessage("{\"jsonrpc\":\"2.0\",\"id\":" + outboundId + ",\"result\":null}");

            client.sendRequest(99, "shutdown", null);
            assertThat(tokenRef.get().asString().value()).isEqualTo(requestedToken);
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

        JsonObject sendRequest(final int id, final String method, final String params) throws Exception {
            final var json = params != null
                ? String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\",\"params\":%s}", id, method, params)
                : String.format("{\"jsonrpc\":\"2.0\",\"id\":%d,\"method\":\"%s\"}", id, method);
            writeMessage(json);
            return readMessage();
        }

        JsonObject sendRequestWithStringId(final String id, final String method, final String params) throws Exception {
            final var json = params != null
                ? String.format("{\"jsonrpc\":\"2.0\",\"id\":\"%s\",\"method\":\"%s\",\"params\":%s}", id, method, params)
                : String.format("{\"jsonrpc\":\"2.0\",\"id\":\"%s\",\"method\":\"%s\"}", id, method);
            writeMessage(json);
            return readMessage();
        }

        void sendNotification(final String method, final String params) throws Exception {
            final var json = String.format("{\"jsonrpc\":\"2.0\",\"method\":\"%s\",\"params\":%s}", method, params);
            writeMessage(json);
            Thread.sleep(100);
        }

        JsonObject readMessage() throws Exception {
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
            return Json.parse(new String(body)).asObject();
        }

        void writeMessage(final String json) throws IOException {
            final var bytes = json.getBytes(StandardCharsets.UTF_8);
            final var header = ("Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            this.out.write(header);
            this.out.write(bytes);
            this.out.flush();
        }

        void writeHeaderOnly(final long claimedLength) throws IOException {
            final var header = ("Content-Length: " + claimedLength + "\r\n\r\n").getBytes(StandardCharsets.UTF_8);
            this.out.write(header);
            this.out.flush();
        }

        boolean awaitServerThreadDone(final long millis) throws InterruptedException {
            serverThread.join(millis);
            return !serverThread.isAlive();
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
