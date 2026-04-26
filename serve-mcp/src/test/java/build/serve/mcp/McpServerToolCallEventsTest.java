package build.serve.mcp;

import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldPublishToolCallEventOnSuccessfulInvocation() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> received.add(event));
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
        assertThat(event.arguments().path("text").asText()).isEqualTo("hello");
        assertThat(event.result()).isPresent();
        assertThat(event.result().get().isError()).isFalse();
        assertThat(event.error()).isEmpty();
        assertThat(event.durationMs()).isGreaterThanOrEqualTo(0L);
        assertThat(event.timestamp()).isNotNull();
    }

    @Test
    void shouldPublishToolCallEventOnToolFailure() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new ThrowingTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> received.add(event));
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
    void shouldNotAffectDispatchBehaviorWhenNoSubscribersAttached() throws Exception {
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

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("result").path("isError").asBoolean()).isFalse();
        assertThat(json.path("result").path("content").get(0).get("text").asText())
            .isEqualTo("echo: world");
    }

    @Test
    void shouldNotPublishEventForUnknownTool() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> received.add(event));
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
    void shouldCarrySessionIdOnEvent() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> received.add(event));

        try (var client = McpTestClient.start(mcpServer)) {
            client.initialize();
            client.call("echo", java.util.Map.of("text", "hi"));
        }

        assertThat(received).hasSize(1);
        assertThat(received.get(0).sessionId()).isNotNull().isNotBlank().isNotEqualTo("local");
    }

    @Test
    void shouldUseLocalSessionIdWhenNoSessionHeader() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var received = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> received.add(event));
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
    void shouldFanOutToMultipleSubscribers() throws Exception {
        final var mcpServer = McpServer.builder("test", "1.0")
            .tool(new EchoTool())
            .build();
        final var first = new CopyOnWriteArrayList<ToolCallEvent>();
        final var second = new CopyOnWriteArrayList<ToolCallEvent>();
        mcpServer.toolCallEvents().subscribe(event -> first.add(event));
        mcpServer.toolCallEvents().subscribe(event -> second.add(event));
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
        public ObjectNode inputSchema() {
            return MAPPER.createObjectNode();
        }

        @Override
        public McpToolResult call(final JsonNode arguments) {
            return McpToolResult.text("echo: " + arguments.path("text").asText());
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
        public ObjectNode inputSchema() {
            return MAPPER.createObjectNode();
        }

        @Override
        public McpToolResult call(final JsonNode arguments) {
            throw new RuntimeException("tool failure");
        }
    }
}
