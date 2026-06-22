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
package build.serve.mcp;

import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonObject;
import build.base.telemetry.Diagnostic;
import build.base.telemetry.Information;
import build.base.telemetry.Telemetry;
import build.base.telemetry.TelemetryRecorderFactory;
import build.base.telemetry.Warning;
import build.base.telemetry.foundation.AbstractTelemetryRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpServer#stdioLoop(java.io.InputStream, java.io.OutputStream)}.
 *
 * @author reed.vonredwitz
 * @since May-2026
 */
class McpStdioTests {

    private McpServer server;

    @BeforeEach
    void setUp() {
        final var location = ToolParam.string("location", "City name or zip code");
        server = McpServer.builder("test-server", "1.0.0")
            .tool(ToolDef.of("get_weather", "Get weather for a location")
                .param(location)
                .handle(args -> McpToolResult.text("Weather in " + location.extract(args) + ": sunny, 72°F")))
            .build();
    }

    @Test
    void shouldHandleInitialize() {
        final var response = sendOne("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}");
        assertThat(response.getString("jsonrpc")).isEqualTo("2.0");
        assertThat(response.get("result").asObject().getString("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(response.get("result").asObject().get("serverInfo").asObject().getString("name"))
            .isEqualTo("test-server");
    }

    @Test
    void shouldHandleToolsList() {
        final var response = sendOne("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}");
        final var tools = (JsonArray) response.get("result").asObject().get("tools");
        assertThat(tools.values()).hasSize(1);
        assertThat(tools.values().get(0).asObject().getString("name")).isEqualTo("get_weather");
    }

    @Test
    void shouldHandleToolsCall() {
        final var response = sendOne("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"get_weather\",\"arguments\":{\"location\":\"Berlin\"}}}");
        final var result = response.get("result").asObject();
        assertThat(result.get("isError").asBoolean().value()).isFalse();
        final var text = ((JsonArray) result.get("content")).values().get(0).asObject().getString("text");
        assertThat(text).isEqualTo("Weather in Berlin: sunny, 72°F");
    }

    @Test
    void shouldReturnErrorForUnknownTool() {
        final var response = sendOne("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"no_such_tool\",\"arguments\":{}}}");
        assertThat(response.get("error").asObject().getString("message")).contains("Unknown tool");
    }

    @Test
    void shouldContinueAfterMalformedJsonLine() {
        final var lines = String.join("\n",
            "not valid json at all",
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}"
        ) + "\n";

        final var out = new ByteArrayOutputStream();
        server.stdioLoop(toStream(lines), out);

        final var responses = out.toString(StandardCharsets.UTF_8).lines()
            .filter(l -> !l.isBlank())
            .map(Json::parse)
            .toList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).asObject().get("result").asObject()
            .get("tools")).isInstanceOf(JsonArray.class);
    }

    @Test
    void shouldSilentlyIgnoreNotifications() {
        final var out = new ByteArrayOutputStream();
        final var input = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}\n";
        server.stdioLoop(toStream(input), out);
        assertThat(out.toString(StandardCharsets.UTF_8).strip()).isEmpty();
    }

    @Test
    void shouldHandleMultipleRequestsInSequence() {
        final var lines = String.join("\n",
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}",
            "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}"
        ) + "\n";

        final var out = new ByteArrayOutputStream();
        server.stdioLoop(toStream(lines), out);

        final var responses = out.toString(StandardCharsets.UTF_8).lines()
            .filter(l -> !l.isBlank())
            .map(Json::parse)
            .toList();

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).asObject().get("result").asObject()
            .getString("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(responses.get(1).asObject().get("result").asObject()
            .get("tools")).isInstanceOf(JsonArray.class);
    }

    @Test
    void shouldUseLocalSessionId() {
        final var received = new java.util.concurrent.CopyOnWriteArrayList<ToolCallEvent>();
        server.toolCallEvents().subscribe(received::add);

        sendOne("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
            + "\"params\":{\"name\":\"get_weather\",\"arguments\":{\"location\":\"Paris\"}}}");

        assertThat(received).hasSize(2);
        assertThat(received.get(0).sessionId()).isEqualTo("local");
        assertThat(received.get(1).sessionId()).isEqualTo("local");
    }

    @Test
    void shouldRoundTripJsonThroughToolResult() {
        final var jsonServer = McpServer.builder("json-server", "1.0.0")
            .tool(ToolDef.of("list_users", "Returns a list of users")
                .handle(args -> McpToolResult.json(
                    JsonObject.builder()
                        .put("users", JsonArray.builder()
                            .add("alice")
                            .add("bob")
                            .build())
                        .build())))
            .build();

        try (var client = McpStdioClient.of(jsonServer)) {
            final var users = client.call("list_users", Map.of())
                .json().asObject().get("users").asArray();
            assertThat(users.values()).hasSize(2);
            assertThat(users.values().get(0).asString().value()).isEqualTo("alice");
            assertThat(users.values().get(1).asString().value()).isEqualTo("bob");
        }
    }

    /**
     * The default recorder writes info/diagnostic to System.out. In real stdio deployments,
     * System.out IS the protocol channel, so any non-JSON recorder output corrupts the NDJSON stream.
     * This test replicates that scenario by redirecting System.out before building the server,
     * then passing that same stream to stdioLoop.
     */
    @Test
    void shouldNotWriteNonJsonBytesToProtocolStream() {
        final var realOut = System.out;
        try {
            final var capturedOut = new ByteArrayOutputStream();
            final var capturedPrintStream = new java.io.PrintStream(capturedOut);
            System.setOut(capturedPrintStream);

            final var stdioServer = McpServer.builder("stdio-server", "1.0.0")
                .tool(ToolDef.of("ping", "ping").handle(args -> McpToolResult.text("pong")))
                .build();

            stdioServer.stdioLoop(
                toStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}\n"),
                capturedPrintStream);

            capturedPrintStream.flush();
            final var output = capturedOut.toString(StandardCharsets.UTF_8);

            output.lines()
                .filter(l -> !l.isBlank())
                .forEach(line -> {
                    try {
                        Json.parse(line);
                    } catch (final RuntimeException e) {
                        org.junit.jupiter.api.Assertions.fail(
                            "Non-JSON line written to protocol stream: " + line);
                    }
                });
        } finally {
            System.setOut(realOut);
        }
    }

    @Test
    void shouldRecordDiagnosticsForEachDispatchedRequest() {
        final var capturing = new CapturingTelemetryRecorder();
        final var tracingServer = McpServer.builder("tracing-server", "1.0.0")
            .recorder(capturing)
            .build();

        final var out = new ByteArrayOutputStream();
        tracingServer.stdioLoop(
            toStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}\n"),
            out);

        assertThat(capturing.diagnostics()).anyMatch(m -> m.contains("received: initialize"));
        assertThat(capturing.diagnostics()).anyMatch(m -> m.contains("sent response to: initialize"));
    }

    @Test
    void shouldRecordInfoOnCleanEof() {
        final var capturing = new CapturingTelemetryRecorder();
        final var tracingServer = McpServer.builder("tracing-server", "1.0.0")
            .recorder(capturing)
            .build();

        tracingServer.stdioLoop(
            toStream("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}\n"),
            new ByteArrayOutputStream());

        assertThat(capturing.infos()).anyMatch(m -> m.contains("clean EOF"));
    }

    @Test
    void shouldRecordWarnOnUnexpectedIoException() {
        final var capturing = new CapturingTelemetryRecorder();
        final var tracingServer = McpServer.builder("tracing-server", "1.0.0")
            .recorder(capturing)
            .build();

        final var brokenStream = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("connection reset by peer");
            }
        };

        tracingServer.stdioLoop(brokenStream, new ByteArrayOutputStream());

        assertThat(capturing.warns()).anyMatch(m -> m.contains("unexpectedly"));
    }

    private JsonObject sendOne(final String line) {
        final var out = new ByteArrayOutputStream();
        server.stdioLoop(toStream(line + "\n"), out);
        return Json.parse(out.toString(StandardCharsets.UTF_8).strip()).asObject();
    }

    private static ByteArrayInputStream toStream(final String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    private static final class CapturingTelemetryRecorder extends AbstractTelemetryRecorder {

        private final List<Telemetry> captured = new CopyOnWriteArrayList<>();

        CapturingTelemetryRecorder() {
            super(URI.create("test://capture"));
        }

        @Override
        protected <T extends Telemetry> T record(final T telemetry) {
            captured.add(telemetry);
            return telemetry;
        }

        @Override
        public TelemetryRecorderFactory factory() {
            return uri -> this;
        }

        List<String> diagnostics() {
            return captured.stream().filter(t -> t instanceof Diagnostic).map(Object::toString).toList();
        }

        List<String> infos() {
            return captured.stream().filter(t -> t instanceof Information).map(Object::toString).toList();
        }

        List<String> warns() {
            return captured.stream().filter(t -> t instanceof Warning).map(Object::toString).toList();
        }
    }
}
