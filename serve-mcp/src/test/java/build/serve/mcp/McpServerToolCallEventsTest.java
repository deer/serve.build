package build.serve.mcp;

import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link McpServer} tool-call event publishing.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
class McpServerToolCallEventsTest {

    private TestServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldPublishToolCallEventOnSuccessfulInvocation() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(received::add);
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"hello\"}}}")
            .send()
            .assertStatus(200);

        assertThat(received).hasSize(1);
        final var event = received.get(0);
        assertThat(event.toolName()).isEqualTo("echo");
        assertThat(event.arguments().asObject().getString("text")).isEqualTo("hello");
        assertThat(event.result()).isPresent();
        assertThat(event.result().get().isError()).isFalse();
        assertThat(event.error()).isEmpty();
        assertThat(event.durationMs()).isGreaterThanOrEqualTo(0L);
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void shouldPublishToolCallEventOnToolFailure() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new ThrowingTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(received::add);
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"thrower\",\"arguments\":{}}}")
            .send()
            .assertStatus(200);

        assertThat(received).hasSize(1);
        final var event = received.get(0);
        assertThat(event.toolName()).isEqualTo("thrower");
        assertThat(event.result()).isEmpty();
        assertThat(event.error()).isPresent();
        assertThat(event.error().get()).isInstanceOf(RuntimeException.class);
        assertThat(event.durationMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void shouldNotAffectDispatchBehaviorWhenNoSubscribersAttached() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        final var response = server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"world\"}}}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(json.get("result").asObject().get("isError").asBoolean().value()).isFalse();
        assertThat(((JsonArray) json.get("result").asObject().get("content"))
            .values().getFirst().asObject().getString("text")).isEqualTo("echo: world");
    }

    @Test
    void shouldNotPublishEventForUnknownTool() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(received::add);
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"no-such-tool\",\"arguments\":{}}}")
            .send()
            .assertStatus(200);

        assertThat(received).isEmpty();
    }

    @Test
    void shouldCarrySessionIdOnEvent() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(received::add);

        try (var client = McpTestClient.start(mcpServer)) {
            client.initialize();
            client.call("echo", java.util.Map.of("text", "hi"));
        }

        assertThat(received).hasSize(1);
        assertThat(received.get(0).sessionId()).isNotNull().isNotBlank().isNotEqualTo("local");
    }

    @Test
    void shouldUseLocalSessionIdWhenNoSessionHeader() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(received::add);
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"no-session\"}}}")
            .send()
            .assertStatus(200);

        assertThat(received).hasSize(1);
        assertThat(received.get(0).sessionId()).isEqualTo("local");
    }

    @Test
    void shouldFanOutToMultipleSubscribers() {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var first = new CopyOnWriteArrayList<ToolCallEvent>();
        final var second = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(first::add);
        mcpServer.toolCallEvents().subscribe(second::add);
        server = TestServer.of(RouterBuilder.create().route("/mcp", mcpServer.handler()).build());

        server.post("/mcp")
            .header("Content-Type", "application/json")
            .body("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\","
                + "\"params\":{\"name\":\"echo\",\"arguments\":{\"text\":\"fan-out\"}}}")
            .send()
            .assertStatus(200);

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(first.get(0).toolName()).isEqualTo("echo");
        assertThat(second.get(0).toolName()).isEqualTo("echo");
    }

    private static final class EchoTool implements McpTool {

        @Override
        public String name() {
            return "echo";
        }

        @Override
        public String description() {
            return "Echoes the input text";
        }

        @Override
        public JsonObject inputSchema() {
            return JsonObject.builder().build();
        }

        @Override
        public McpToolResult call(final JsonValue arguments) {
            final var text = arguments.asObject().members().getOrDefault("text",
                JsonNull.INSTANCE);
            final var textStr = text instanceof JsonString s ? s.value() : "";
            return McpToolResult.text("echo: " + textStr);
        }
    }

    private static final class ThrowingTool implements McpTool {

        @Override
        public String name() {
            return "thrower";
        }

        @Override
        public String description() {
            return "Always throws an exception";
        }

        @Override
        public JsonObject inputSchema() {
            return JsonObject.builder().build();
        }

        @Override
        public McpToolResult call(final JsonValue arguments) {
            throw new RuntimeException("tool failure");
        }
    }
}
