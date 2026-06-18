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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

    private JsonObject sendOne(final String line) {
        final var out = new ByteArrayOutputStream();
        server.stdioLoop(toStream(line + "\n"), out);
        return Json.parse(out.toString(StandardCharsets.UTF_8).strip()).asObject();
    }

    private static ByteArrayInputStream toStream(final String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
