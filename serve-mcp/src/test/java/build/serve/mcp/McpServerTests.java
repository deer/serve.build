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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpServer}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
class McpServerTests {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private McpTestClient client;

    @BeforeEach
    void setUp() {
        final var mcpServer = McpServer.builder("test-server", "1.0.0")
            .tool(new McpTool() {
                @Override
                public String name() {
                    return "get_weather";
                }

                @Override
                public String description() {
                    return "Get weather for a location";
                }

                @Override
                public ObjectNode inputSchema() {
                    return McpTools.schema(MAPPER,
                        Map.of("location", "City name or zip code"),
                        List.of("location"));
                }

                @Override
                public McpToolResult call(final JsonNode arguments) {
                    final var location = arguments.get("location").asText();
                    return McpToolResult.text("Weather in " + location + ": sunny, 72°F");
                }
            })
            .build();

        client = McpTestClient.start(mcpServer);
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void initializeTest() throws Exception {
        final var json = client.initialize();
        assertThat(json.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(json.path("result").path("protocolVersion").asText()).isEqualTo("2025-03-26");
        assertThat(json.path("result").path("capabilities").path("tools").path("listChanged").asBoolean()).isFalse();
        assertThat(json.path("result").path("serverInfo").path("name").asText()).isEqualTo("test-server");
        assertThat(json.path("result").path("serverInfo").path("version").asText()).isEqualTo("1.0.0");
    }

    @Test
    void toolsListTest() throws Exception {
        final var tools = client.listTools();
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isEqualTo(1);
        assertThat(tools.get(0).get("name").asText()).isEqualTo("get_weather");
        assertThat(tools.get(0).get("description").asText()).isEqualTo("Get weather for a location");
        assertThat(tools.get(0).path("inputSchema").path("type").asText()).isEqualTo("object");
    }

    @Test
    void toolsCallTest() throws Exception {
        final var result = client.call("get_weather", Map.of("location", "Berlin"));
        assertThat(result.path("isError").asBoolean()).isFalse();
        assertThat(result.path("content").get(0).get("type").asText()).isEqualTo("text");
        assertThat(result.path("content").get(0).get("text").asText()).isEqualTo("Weather in Berlin: sunny, 72°F");
    }

    @Test
    void toolsCallUnknownTest() throws Exception {
        final var json = client.send("tools/call",
            Map.of("name", "nonexistent", "arguments", Map.of()));
        assertThat(json.path("error").path("code").asInt()).isEqualTo(-32602);
        assertThat(json.path("error").path("message").asText()).contains("Unknown tool");
    }

    @Test
    void notificationIgnoredTest() {
        client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
            .send()
            .assertStatus(202);
    }

    @Test
    void unknownMethodTest() throws Exception {
        final var json = client.send("unknown/method", Map.of());
        assertThat(json.path("error").path("code").asInt()).isEqualTo(-32601);
        assertThat(json.path("error").path("message").asText()).isEqualTo("Method not found");
    }

    @Test
    void shouldReturn405ForGet() {
        client.get("/mcp").send().assertStatus(405);
    }

    // --- ping ---

    @Test
    void shouldRespondToPing() throws Exception {
        final var json = client.send("ping", Map.of());
        assertThat(json.path("result").isObject()).isTrue();
    }

    // --- session management ---

    @Test
    void shouldReturnSessionIdOnInitialize() throws Exception {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        assertThat(response.header("Mcp-Session-Id")).isNotBlank();
    }

    @Test
    void shouldAcceptValidSessionId() throws Exception {
        client.initialize();
        // subsequent calls via client automatically include the session ID
        final var tools = client.listTools();
        assertThat(tools.isArray()).isTrue();
    }

    @Test
    void shouldReturn404ForUnknownSessionId() {
        client.post("/mcp")
            .header("Content-Type", "application/json")
            .header("Mcp-Session-Id", "unknown-session-id")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}")
            .send()
            .assertStatus(404);
    }

    // --- SSE transport ---

    @Test
    void shouldRespondWithSseWhenAcceptHeaderIncludesEventStream() throws Exception {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        assertThat(response.contentType()).contains("text/event-stream");
        assertThat(response.body()).startsWith("event: message\ndata: ");
        assertThat(response.body()).endsWith("\n\n");
    }

    @Test
    void shouldIncludeValidJsonInSseDataField() throws Exception {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        final var dataLine = response.body().lines()
            .filter(l -> l.startsWith("data: "))
            .findFirst()
            .orElseThrow();
        final var json = MAPPER.readTree(dataLine.substring("data: ".length()));
        assertThat(json.path("result").path("protocolVersion").asText()).isEqualTo("2025-03-26");
    }

    @Test
    void shouldReturnJsonWhenAcceptHeaderIsAbsent() throws Exception {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        assertThat(response.contentType()).contains("application/json");
        MAPPER.readTree(response.body()); // parses without error
    }
}
