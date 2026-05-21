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
import build.base.json.JsonValue;
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
                public JsonObject inputSchema() {
                    return McpTools.schema(
                        Map.of("location", "City name or zip code"),
                        List.of("location"));
                }

                @Override
                public McpToolResult call(final JsonValue arguments) {
                    final var location = arguments.asObject().getString("location");
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
    void initializeTest() {
        final var json = client.initialize().asObject();
        assertThat(json.getString("jsonrpc")).isEqualTo("2.0");
        final var result = json.get("result").asObject();
        assertThat(result.getString("protocolVersion")).isEqualTo("2025-03-26");
        assertThat(result.get("capabilities").asObject().get("tools").asObject().get("listChanged").asBoolean().value()).isFalse();
        assertThat(result.get("serverInfo").asObject().getString("name")).isEqualTo("test-server");
        assertThat(result.get("serverInfo").asObject().getString("version")).isEqualTo("1.0.0");
    }

    @Test
    void toolsListTest() {
        final var tools = client.listTools();
        assertThat(tools).isInstanceOf(JsonArray.class);
        assertThat(((JsonArray) tools).values()).hasSize(1);
        final var tool = ((JsonArray) tools).values().get(0).asObject();
        assertThat(tool.getString("name")).isEqualTo("get_weather");
        assertThat(tool.getString("description")).isEqualTo("Get weather for a location");
        assertThat(tool.get("inputSchema").asObject().getString("type")).isEqualTo("object");
    }

    @Test
    void toolsCallTest() {
        final var result = client.call("get_weather", Map.of("location", "Berlin")).asObject();
        assertThat(result.get("isError").asBoolean().value()).isFalse();
        final var content = ((JsonArray) result.get("content")).values().get(0).asObject();
        assertThat(content.getString("type")).isEqualTo("text");
        assertThat(content.getString("text")).isEqualTo("Weather in Berlin: sunny, 72°F");
    }

    @Test
    void shouldSerializeResourceContent() {
        try (final var resourceClient = McpTestClient.start(McpServer.builder("res-server", "1.0.0")
            .tool(new McpTool() {
                @Override
                public String name() {
                    return "get_file";
                }

                @Override
                public String description() {
                    return "Returns a binary file";
                }

                @Override
                public JsonObject inputSchema() {
                    return McpTools.schema(Map.of(), List.of());
                }

                @Override
                public McpToolResult call(final JsonValue arguments) {
                    return McpToolResult.withResources("Here is the file.",
                        List.of(new McpContent.Resource("output.mid", "audio/midi", "TVRoZA==")));
                }
            })
            .build())) {

            resourceClient.initialize();
            final var result = resourceClient.call("get_file", Map.of()).asObject();
            assertThat(result.get("isError").asBoolean().value()).isFalse();

            final var content = ((JsonArray) result.get("content")).values();
            final var text = content.get(0).asObject();
            assertThat(text.getString("type")).isEqualTo("text");
            assertThat(text.getString("text")).isEqualTo("Here is the file.");

            final var resource = content.get(1).asObject();
            assertThat(resource.getString("type")).isEqualTo("resource");
            final var resourceObj = resource.get("resource").asObject();
            assertThat(resourceObj.getString("uri")).isEqualTo("output.mid");
            assertThat(resourceObj.getString("mimeType")).isEqualTo("audio/midi");
            assertThat(resourceObj.getString("blob")).isEqualTo("TVRoZA==");
        }
    }

    @Test
    void toolsCallUnknownTest() {
        final var json = client.send("tools/call",
            Map.of("name", "nonexistent", "arguments", Map.of())).asObject();
        assertThat(json.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32602);
        assertThat(json.get("error").asObject().getString("message")).contains("Unknown tool");
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
    void unknownMethodTest() {
        final var json = client.send("unknown/method", Map.of()).asObject();
        assertThat(json.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32601);
        assertThat(json.get("error").asObject().getString("message")).isEqualTo("Method not found");
    }

    @Test
    void shouldReturn404ForGetWithoutSession() {
        client.get("/mcp").send().assertStatus(404);
    }

    @Test
    void shouldRespondToPing() {
        final var json = client.send("ping", Map.of()).asObject();
        assertThat(json.get("result")).isInstanceOf(JsonObject.class);
    }

    @Test
    void shouldReturnSessionIdOnInitialize() {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        assertThat(response.header("Mcp-Session-Id")).isNotBlank();
    }

    @Test
    void shouldAcceptValidSessionId() {
        client.initialize();
        final var tools = client.listTools();
        assertThat(tools).isInstanceOf(JsonArray.class);
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

    @Test
    void shouldRespondWithSseWhenAcceptHeaderIncludesEventStream() {
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
    void shouldIncludeValidJsonInSseDataField() {
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
        final var json = Json.parse(dataLine.substring("data: ".length())).asObject();
        assertThat(json.get("result").asObject().getString("protocolVersion")).isEqualTo("2025-03-26");
    }

    @Test
    void shouldReturnJsonWhenAcceptHeaderIsAbsent() {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
            .send()
            .assertStatus(200);
        assertThat(response.contentType()).contains("application/json");
        Json.parse(response.body()); // parses without error
    }

    @Test
    void shouldAdvertiseResourcesCapabilityOnInitialize() {
        final var result = client.initialize().asObject().get("result").asObject();
        final var resources = result.get("capabilities").asObject().get("resources").asObject();
        assertThat(resources.get("subscribe").asBoolean().value()).isTrue();
        assertThat(resources.get("listChanged").asBoolean().value()).isFalse();
    }

    @Test
    void shouldListResources() {
        try (final var resourceClient = McpTestClient.start(McpServer.builder("res-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///data/config.json";
                }

                @Override
                public String name() {
                    return "Config";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.of("App config");
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("application/json");
                }

                @Override
                public McpResourceContent read() {
                    return new McpResourceContent.Text(uri(), "application/json", "{}");
                }
            })
            .build())) {

            resourceClient.initialize();
            final var resources = (JsonArray) resourceClient.listResources();
            assertThat(resources.values()).hasSize(1);
            final var r = resources.values().get(0).asObject();
            assertThat(r.getString("uri")).isEqualTo("file:///data/config.json");
            assertThat(r.getString("name")).isEqualTo("Config");
            assertThat(r.getString("description")).isEqualTo("App config");
            assertThat(r.getString("mimeType")).isEqualTo("application/json");
        }
    }

    @Test
    void shouldReadTextResource() {
        try (final var resourceClient = McpTestClient.start(McpServer.builder("res-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///readme.txt";
                }

                @Override
                public String name() {
                    return "Readme";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("text/plain");
                }

                @Override
                public McpResourceContent read() {
                    return new McpResourceContent.Text(uri(), "text/plain", "Hello, world!");
                }
            })
            .build())) {

            resourceClient.initialize();
            final var response = resourceClient.readResource("file:///readme.txt").asObject();
            assertThat(response.members()).containsKey("result");
            final var contents = (JsonArray) response.get("result").asObject().get("contents");
            assertThat(contents.values()).hasSize(1);
            final var content = contents.values().get(0).asObject();
            assertThat(content.getString("uri")).isEqualTo("file:///readme.txt");
            assertThat(content.getString("mimeType")).isEqualTo("text/plain");
            assertThat(content.getString("text")).isEqualTo("Hello, world!");
        }
    }

    @Test
    void shouldReadBlobResource() {
        try (final var resourceClient = McpTestClient.start(McpServer.builder("res-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///image.png";
                }

                @Override
                public String name() {
                    return "Image";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("image/png");
                }

                @Override
                public McpResourceContent read() {
                    return new McpResourceContent.Blob(uri(), "image/png", "iVBORw0KGgo=");
                }
            })
            .build())) {

            resourceClient.initialize();
            final var response = resourceClient.readResource("file:///image.png").asObject();
            final var content = ((JsonArray) response.get("result").asObject().get("contents")).values().get(0).asObject();
            assertThat(content.getString("mimeType")).isEqualTo("image/png");
            assertThat(content.getString("blob")).isEqualTo("iVBORw0KGgo=");
            assertThat(content.members()).doesNotContainKey("text");
        }
    }

    @Test
    void shouldReturnErrorForUnknownResource() {
        try (final var resourceClient = McpTestClient.start(McpServer.builder("res-server", "1.0.0").build())) {
            resourceClient.initialize();
            final var response = resourceClient.readResource("file:///missing.txt").asObject();
            assertThat(response.members()).containsKey("error");
            assertThat(response.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32002);
            assertThat(response.get("error").asObject().getString("message")).contains("missing.txt");
        }
    }

    @Test
    void shouldListEmptyResourcesWhenNoneRegistered() {
        final var resources = (JsonArray) client.listResources();
        assertThat(resources.values()).isEmpty();
    }

    @Test
    void shouldListResourceTemplates() {
        try (final var templateClient = McpTestClient.start(McpServer.builder("tmpl-server", "1.0.0")
            .template(new McpResourceTemplate() {
                @Override
                public String uriTemplate() {
                    return "file:///{path}";
                }

                @Override
                public String name() {
                    return "File";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.of("Any file");
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("text/plain");
                }

                @Override
                public McpResourceContent read(final String uri) {
                    return new McpResourceContent.Text(uri, "text/plain", "contents of " + uri);
                }
            })
            .build())) {

            templateClient.initialize();
            final var result = templateClient.send("resources/templates/list", java.util.Map.of()).asObject()
                .get("result").asObject();
            final var templates = (JsonArray) result.get("resourceTemplates");
            assertThat(templates.values()).hasSize(1);
            final var t = templates.values().get(0).asObject();
            assertThat(t.getString("uriTemplate")).isEqualTo("file:///{path}");
            assertThat(t.getString("name")).isEqualTo("File");
            assertThat(t.getString("description")).isEqualTo("Any file");
            assertThat(t.getString("mimeType")).isEqualTo("text/plain");
        }
    }

    @Test
    void shouldReadResourceMatchedByTemplate() {
        try (final var templateClient = McpTestClient.start(McpServer.builder("tmpl-server", "1.0.0")
            .template(new McpResourceTemplate() {
                @Override
                public String uriTemplate() {
                    return "db://{table}/{id}";
                }

                @Override
                public String name() {
                    return "DB Row";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("application/json");
                }

                @Override
                public McpResourceContent read(final String uri) {
                    return new McpResourceContent.Text(uri, "application/json", "{\"uri\":\"" + uri + "\"}");
                }
            })
            .build())) {

            templateClient.initialize();
            final var response = templateClient.readResource("db://users/42").asObject();
            final var content = ((JsonArray) response.get("result").asObject().get("contents"))
                .values().get(0).asObject();
            assertThat(content.getString("uri")).isEqualTo("db://users/42");
            assertThat(content.getString("text")).contains("db://users/42");
        }
    }

    @Test
    void shouldPreferExactResourceOverTemplate() {
        try (final var mixedClient = McpTestClient.start(McpServer.builder("mixed-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///special.txt";
                }

                @Override
                public String name() {
                    return "Special";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.empty();
                }

                @Override
                public McpResourceContent read() {
                    return new McpResourceContent.Text(uri(), "text/plain", "exact match");
                }
            })
            .template(new McpResourceTemplate() {
                @Override
                public String uriTemplate() {
                    return "file:///{path}";
                }

                @Override
                public String name() {
                    return "File";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.empty();
                }

                @Override
                public McpResourceContent read(final String uri) {
                    return new McpResourceContent.Text(uri, "text/plain", "template match");
                }
            })
            .build())) {

            mixedClient.initialize();
            final var content = ((JsonArray) mixedClient.readResource("file:///special.txt")
                .asObject().get("result").asObject().get("contents"))
                .values().get(0).asObject();
            assertThat(content.getString("text")).isEqualTo("exact match");
        }
    }

    @Test
    void shouldReturnErrorForUriMatchingNoTemplateOrResource() {
        final var response = client.readResource("file:///missing.txt").asObject();
        assertThat(response.members()).containsKey("error");
        assertThat(response.get("error").asObject().get("code").asNumber().toNumber().intValue()).isEqualTo(-32002);
    }

    @Test
    void shouldSubscribeAndReceiveNotification() throws InterruptedException {
        final var mcpServer = McpServer.builder("sub-server", "1.0.0").build();
        try (final var subClient = McpTestClient.start(mcpServer)) {
            subClient.initialize();

            try (final var stream = subClient.sseStream()) {
                Thread.sleep(100); // let SSE connection establish

                final var subResponse = subClient.send("resources/subscribe",
                    java.util.Map.of("uri", "file:///data.txt")).asObject();
                assertThat(subResponse.members()).containsKey("result");

                mcpServer.notifyResourceChanged("file:///data.txt");

                final var events = stream.collect(1, java.time.Duration.ofSeconds(5));
                assertThat(events).hasSize(1);
                final var notification = Json.parse(events.get(0).data()).asObject();
                assertThat(notification.getString("method")).isEqualTo("notifications/resources/updated");
                assertThat(notification.get("params").asObject().getString("uri")).isEqualTo("file:///data.txt");
            }
        }
    }

    @Test
    void shouldNotReceiveNotificationAfterUnsubscribe() throws InterruptedException {
        final var mcpServer = McpServer.builder("sub-server", "1.0.0").build();
        try (final var subClient = McpTestClient.start(mcpServer)) {
            subClient.initialize();

            try (final var stream = subClient.sseStream()) {
                Thread.sleep(100);

                subClient.send("resources/subscribe", java.util.Map.of("uri", "file:///data.txt"));
                subClient.send("resources/unsubscribe", java.util.Map.of("uri", "file:///data.txt"));

                mcpServer.notifyResourceChanged("file:///data.txt");

                assertThat(stream.poll(java.time.Duration.ofMillis(300))).isEmpty();
            }
        }
    }

    @Test
    void shouldNotNotifyUnsubscribedUri() throws InterruptedException {
        final var mcpServer = McpServer.builder("sub-server", "1.0.0").build();
        try (final var subClient = McpTestClient.start(mcpServer)) {
            subClient.initialize();

            try (final var stream = subClient.sseStream()) {
                Thread.sleep(100);

                subClient.send("resources/subscribe", java.util.Map.of("uri", "file:///a.txt"));

                mcpServer.notifyResourceChanged("file:///b.txt");

                assertThat(stream.poll(java.time.Duration.ofMillis(300))).isEmpty();
            }
        }
    }

    @Test
    void shouldEchoStringIdInResponse() {
        final var response = client.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":\"req-abc\",\"method\":\"ping\",\"params\":{}}")
            .send()
            .assertStatus(200);
        final var json = Json.parse(response.body()).asObject();
        assertThat(json.getString("id")).isEqualTo("req-abc");
    }
}
