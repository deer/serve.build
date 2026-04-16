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

import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
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

    private TestServer server;

    @BeforeEach
    void setUp() {
        final var handler = McpServer.builder("test-server", "1.0.0")
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
            .build()
            .handler();

        final var router = RouterBuilder.create()
            .route("/mcp", handler)
            .build();

        server = TestServer.of(router);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void initializeTest() throws Exception {
        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.get("jsonrpc").asText()).isEqualTo("2.0");
        assertThat(json.get("id").asInt()).isEqualTo(1);
        assertThat(json.path("result").path("protocolVersion").asText()).isEqualTo("2025-03-26");
        assertThat(json.path("result").path("capabilities").path("tools").path("listChanged").asBoolean()).isFalse();
        assertThat(json.path("result").path("serverInfo").path("name").asText()).isEqualTo("test-server");
        assertThat(json.path("result").path("serverInfo").path("version").asText()).isEqualTo("1.0.0");
    }

    @Test
    void toolsListTest() throws Exception {
        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\",\"params\":{}}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.get("id").asInt()).isEqualTo(2);

        final var tools = json.path("result").path("tools");
        assertThat(tools.isArray()).isTrue();
        assertThat(tools.size()).isEqualTo(1);
        assertThat(tools.get(0).get("name").asText()).isEqualTo("get_weather");
        assertThat(tools.get(0).get("description").asText()).isEqualTo("Get weather for a location");
        assertThat(tools.get(0).path("inputSchema").path("type").asText()).isEqualTo("object");
    }

    @Test
    void toolsCallTest() throws Exception {
        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"get_weather\",\"arguments\":{\"location\":\"Berlin\"}}}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.get("id").asInt()).isEqualTo(3);

        final var result = json.path("result");
        assertThat(result.path("isError").asBoolean()).isFalse();
        assertThat(result.path("content").get(0).get("type").asText()).isEqualTo("text");
        assertThat(result.path("content").get(0).get("text").asText()).isEqualTo("Weather in Berlin: sunny, 72°F");
    }

    @Test
    void toolsCallUnknownTest() throws Exception {
        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"nonexistent\",\"arguments\":{}}}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.get("id").asInt()).isEqualTo(4);
        assertThat(json.path("error").path("code").asInt()).isEqualTo(-32602);
        assertThat(json.path("error").path("message").asText()).contains("Unknown tool");
    }

    @Test
    void notificationIgnoredTest() {
        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}")
            .send()
            .assertStatus(202);
    }

    @Test
    void unknownMethodTest() throws Exception {
        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"unknown/method\",\"params\":{}}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.get("id").asInt()).isEqualTo(5);
        assertThat(json.path("error").path("code").asInt()).isEqualTo(-32601);
        assertThat(json.path("error").path("message").asText()).isEqualTo("Method not found");
    }

    @Test
    void getReturns405Test() {
        server.get("/mcp")
            .send()
            .assertStatus(405);
    }
}
