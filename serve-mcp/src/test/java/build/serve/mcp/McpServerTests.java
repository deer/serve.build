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
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
                        List.of(McpContent.Resource.blob("output.mid", "audio/midi", "TVRoZA==")));
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
    void shouldReturn400ForGetWithoutSession() {
        client.get("/mcp").send().assertStatus(400);
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
    void shouldSerializeAudioContent() {
        try (final var audioClient = McpTestClient.start(McpServer.builder("audio-server", "1.0.0")
            .tool(new McpTool() {
                @Override
                public String name() {
                    return "get_audio";
                }

                @Override
                public String description() {
                    return "Returns audio";
                }

                @Override
                public JsonObject inputSchema() {
                    return McpTools.schema(Map.of(), List.of());
                }

                @Override
                public McpToolResult call(final JsonValue arguments) {
                    return new McpToolResult(List.of(new McpContent.Audio("UklGRg==", "audio/wav")), false);
                }
            })
            .build())) {

            audioClient.initialize();
            final var result = audioClient.call("get_audio", Map.of()).asObject();
            final var content = ((JsonArray) result.get("content")).values().get(0).asObject();
            assertThat(content.getString("type")).isEqualTo("audio");
            assertThat(content.getString("data")).isEqualTo("UklGRg==");
            assertThat(content.getString("mimeType")).isEqualTo("audio/wav");
        }
    }

    @Test
    void shouldSerializeTextEmbeddedResource() {
        try (final var textClient = McpTestClient.start(McpServer.builder("text-server", "1.0.0")
            .tool(new McpTool() {
                @Override
                public String name() {
                    return "get_doc";
                }

                @Override
                public String description() {
                    return "Returns a doc";
                }

                @Override
                public JsonObject inputSchema() {
                    return McpTools.schema(Map.of(), List.of());
                }

                @Override
                public McpToolResult call(final JsonValue arguments) {
                    return McpToolResult.withResources("Here is the doc.",
                        List.of(McpContent.Resource.text("readme.txt", "text/plain", "Hello!")));
                }
            })
            .build())) {

            textClient.initialize();
            final var result = textClient.call("get_doc", Map.of()).asObject();
            final var content = ((JsonArray) result.get("content")).values();
            final var resource = content.get(1).asObject();
            assertThat(resource.getString("type")).isEqualTo("resource");
            final var resourceObj = resource.get("resource").asObject();
            assertThat(resourceObj.getString("uri")).isEqualTo("readme.txt");
            assertThat(resourceObj.getString("text")).isEqualTo("Hello!");
            assertThat(resourceObj.members()).doesNotContainKey("blob");
        }
    }

    @Test
    void shouldAllowRequestWithPermittedOrigin() throws IOException {
        try (final var originClient = McpTestClient.start(McpServer.builder("origin-server", "1.0.0")
            .allowOrigin("https://app.example.com")
            .build())) {

            assertThat(rawPost(originClient.port(), Map.of("Origin", "https://app.example.com"))).isEqualTo(200);
        }
    }

    @Test
    void shouldRejectRequestWithDisallowedOrigin() throws IOException {
        try (final var originClient = McpTestClient.start(McpServer.builder("origin-server", "1.0.0")
            .allowOrigin("https://app.example.com")
            .build())) {

            assertThat(rawPost(originClient.port(), Map.of("Origin", "https://evil.example.com"))).isEqualTo(403);
        }
    }

    @Test
    void shouldAllowRequestWithNoOriginWhenOriginsConfigured() throws IOException {
        try (final var originClient = McpTestClient.start(McpServer.builder("origin-server", "1.0.0")
            .allowOrigin("https://app.example.com")
            .build())) {

            assertThat(rawPost(originClient.port(), Map.of())).isEqualTo(200);
        }
    }

    @Test
    void shouldIncludeInstructionsInInitializeResult() {
        try (final var instrClient = McpTestClient.start(McpServer.builder("instr-server", "1.0.0")
            .instructions("Use the get_weather tool to answer weather questions.")
            .build())) {

            final var result = instrClient.initialize().asObject().get("result").asObject();
            assertThat(result.getString("instructions")).isEqualTo("Use the get_weather tool to answer weather questions.");
        }
    }

    @Test
    void shouldOmitInstructionsWhenNotSet() {
        final var result = client.initialize().asObject().get("result").asObject();
        assertThat(result.members()).doesNotContainKey("instructions");
    }

    @Test
    void shouldIncludeAnnotationsInToolsList() {
        try (final var annClient = McpTestClient.start(McpServer.builder("ann-server", "1.0.0")
            .tool(new McpTool() {
                @Override
                public String name() {
                    return "delete_record";
                }

                @Override
                public String description() {
                    return "Deletes a record";
                }

                @Override
                public JsonObject inputSchema() {
                    return McpTools.schema(Map.of(), List.of());
                }

                @Override
                public McpToolResult call(final JsonValue arguments) {
                    return McpToolResult.text("deleted");
                }

                @Override
                public java.util.Optional<McpToolAnnotations> annotations() {
                    return java.util.Optional.of(McpToolAnnotations.builder()
                        .destructiveHint(true)
                        .idempotentHint(false)
                        .readOnlyHint(false)
                        .audience(List.of("assistant"))
                        .build());
                }
            })
            .build())) {

            annClient.initialize();
            final var tool = ((JsonArray) annClient.listTools()).values().get(0).asObject();
            final var ann = tool.get("annotations").asObject();
            assertThat(ann.get("destructiveHint").asBoolean().value()).isTrue();
            assertThat(ann.get("idempotentHint").asBoolean().value()).isFalse();
            assertThat(ann.get("readOnlyHint").asBoolean().value()).isFalse();
            final var audience = (JsonArray) ann.get("audience");
            assertThat(audience.values()).hasSize(1);
            assertThat(audience.values().get(0).asString().value()).isEqualTo("assistant");
        }
    }

    @Test
    void shouldOmitAnnotationsWhenNotSet() {
        client.initialize();
        final var tool = ((JsonArray) client.listTools()).values().get(0).asObject();
        assertThat(tool.members()).doesNotContainKey("annotations");
    }

    @Test
    void shouldIncludeSizeInResourcesList() {
        try (final var sizeClient = McpTestClient.start(McpServer.builder("size-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///data.bin";
                }

                @Override
                public String name() {
                    return "Data";
                }

                @Override
                public java.util.Optional<String> description() {
                    return java.util.Optional.empty();
                }

                @Override
                public java.util.Optional<String> mimeType() {
                    return java.util.Optional.of("application/octet-stream");
                }

                @Override
                public java.util.Optional<Long> size() {
                    return java.util.Optional.of(1024L);
                }

                @Override
                public McpResourceContent read() {
                    return new McpResourceContent.Blob(uri(), "application/octet-stream", "");
                }
            })
            .build())) {

            sizeClient.initialize();
            final var resource = ((JsonArray) sizeClient.listResources()).values().get(0).asObject();
            assertThat(resource.get("size").asNumber().toNumber().longValue()).isEqualTo(1024L);
        }
    }

    @Test
    void shouldOmitSizeWhenNotProvided() {
        try (final var nosizeClient = McpTestClient.start(McpServer.builder("nosize-server", "1.0.0")
            .resource(new McpResource() {
                @Override
                public String uri() {
                    return "file:///data.bin";
                }

                @Override
                public String name() {
                    return "Data";
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
                    return new McpResourceContent.Text(uri(), "text/plain", "");
                }
            })
            .build())) {

            nosizeClient.initialize();
            final var resource = ((JsonArray) nosizeClient.listResources()).values().get(0).asObject();
            assertThat(resource.members()).doesNotContainKey("size");
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

    @Test
    void shouldReturn200ForDeleteRegardlessOfSessionId() {
        final var mcp = McpServer.builder("s", "1").build();
        final var srv = TestServer.of(
            RouterBuilder.create()
                .route("/mcp", mcp.handler()).build());
        try (srv) {
            assertThat(srv.delete("/mcp").send().status()).isEqualTo(200);
            assertThat(srv.delete("/mcp").header("Mcp-Session-Id", "unknown").send().status()).isEqualTo(200);
        }
    }

    @Test
    void shouldRejectDeletedSessionOnNextRequest() {
        final var mcp = McpServer.builder("s", "1").build();
        final var srv = TestServer.of(
            RouterBuilder.create()
                .route("/mcp", mcp.handler()).build());
        try (srv) {
            final var initResp = srv.post("/mcp")
                .header("Content-Type", "application/json")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
                .send();
            final var sessionId = initResp.header("Mcp-Session-Id");

            srv.delete("/mcp").header("Mcp-Session-Id", sessionId).send().assertStatus(200);

            final var followUp = srv.post("/mcp")
                .header("Content-Type", "application/json")
                .header("Mcp-Session-Id", sessionId)
                .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"ping\"}")
                .send();
            assertThat(followUp.status()).isEqualTo(404);
        }
    }

    @Test
    void shouldRejectInitializeWhenSessionCapReached() {
        final var mcp = McpServer.builder("s", "1").maxSessions(1).build();
        final var srv = TestServer.of(
            RouterBuilder.create()
                .route("/mcp", mcp.handler()).build());
        try (srv) {
            srv.post("/mcp")
                .header("Content-Type", "application/json")
                .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}")
                .send().assertStatus(200);
            final var second = srv.post("/mcp")
                .header("Content-Type", "application/json")
                .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\",\"params\":{}}")
                .send();
            assertThat(second.status()).isEqualTo(503);
        }
    }

    @Test
    void shouldSanitizeExceptionMessageInToolError() {
        final var badTool = new McpTool() {
            @Override
            public String name() {
                return "bad_tool";
            }

            @Override
            public String description() {
                return "throws";
            }

            @Override
            public JsonObject inputSchema() {
                return McpTools.schema(Map.of(), List.of());
            }

            @Override
            public McpToolResult call(final JsonValue arguments) throws Exception {
                throw new RuntimeException("error\r\nX-Injected: evil");
            }
        };
        final var errorServer = McpServer.builder("s", "1").tool(badTool).build();
        try (var errorClient = McpTestClient.start(errorServer)) {
            errorClient.initialize();
            final var result = errorClient.call("bad_tool", Map.of());
            final var content = ((JsonArray) result.asObject().members().get("content")).values();
            final var text = content.get(0).asObject().getString("text");
            assertThat(text).doesNotContain("\r").doesNotContain("\n");
            assertThat(text).contains("error");
        }
    }

    /**
     * Sends a raw HTTP POST to /mcp using a plain socket, bypassing Java's restricted-header
     * filtering so that headers like Origin can be set freely.
     */
    private static int rawPost(final int port, final Map<String, String> extraHeaders) throws IOException {
        final var body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\",\"params\":{}}";
        final var bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        final var sb = new StringBuilder();
        sb.append("POST /mcp HTTP/1.1\r\n");
        sb.append("Host: 127.0.0.1:").append(port).append("\r\n");
        sb.append("Content-Type: application/json\r\n");
        sb.append("Content-Length: ").append(bodyBytes.length).append("\r\n");
        for (final var entry : extraHeaders.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append("\r\n");
        }
        sb.append("\r\n").append(body);

        try (var socket = new Socket("127.0.0.1", port);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
            socket.getOutputStream().write(sb.toString().getBytes(StandardCharsets.US_ASCII));
            socket.getOutputStream().flush();
            final var statusLine = in.readLine();
            return Integer.parseInt(statusLine.split(" ")[1]);
        }
    }
}
