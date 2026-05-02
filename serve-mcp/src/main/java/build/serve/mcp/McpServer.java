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

import build.base.flow.Publisher;
import build.base.flow.SubscriberRegistry;
import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.sse.SseEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An MCP (Model Context Protocol) server that exposes tools via JSON-RPC 2.0 over HTTP.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class McpServer {

    /**
     * ScopedValue bound to the current MCP session ID for the duration of each tool call.
     * Absent for tool calls made without a session header (e.g. local single-user use).
     */
    public static final ScopedValue<String> SESSION_ID = ScopedValue.newInstance();

    private final McpServerInfo info;
    private final Map<String, McpTool> tools;
    private final SubscriberRegistry<ToolCallEvent> toolCallEvents = new SubscriberRegistry<>();
    private final Set<String> sessions = ConcurrentHashMap.newKeySet();

    private McpServer(final Builder builder) {
        this.info = new McpServerInfo(builder.name, builder.version);

        final var toolMap = new LinkedHashMap<String, McpTool>();
        for (final var tool : builder.tools) {
            toolMap.put(tool.name(), tool);
        }
        this.tools = Map.copyOf(toolMap);
    }

    /**
     * Returns a serve.build {@link Handler} that handles the MCP endpoint.
     * <p>
     * Implements the MCP streamable HTTP transport (spec 2025-03-26). POST requests receive either
     * a JSON response or an SSE stream depending on the client's {@code Accept} header. Session IDs
     * are issued during {@code initialize} and validated on subsequent requests.
     *
     * @return the handler
     */
    public Handler handler() {
        return exchange -> {
            final var method = exchange.request().method();

            if (!"POST".equalsIgnoreCase(method)) {
                exchange.response().status(405).send("Method Not Allowed");
                return;
            }

            final var sessionId = exchange.request().header("Mcp-Session-Id");
            if (sessionId.isPresent() && !sessions.contains(sessionId.get())) {
                exchange.response().status(404).send("Session not found");
                return;
            }

            final var body = exchange.request().bodyAsString();
            final var request = Json.parse(body).asObject();

            final var rpcMethod = getString(request, "method");
            final var id = request.members().get("id");

            // Notification (no id) — respond 202
            if (id == null || id instanceof JsonNull) {
                exchange.response().status(202).send("");
                return;
            }

            final var isInitialize = "initialize".equals(rpcMethod);
            final var responseJson = switch (rpcMethod) {
                case "initialize" -> envelope(id, handleInitialize());
                case "ping" -> envelope(id, JsonObject.builder().build());
                case "tools/list" -> envelope(id, handleToolsList());
                case "tools/call" -> {
                    final var params = request.members().getOrDefault("params", JsonNull.INSTANCE);
                    yield handleToolsCall(params, sessionId.orElse("local"), id);
                }
                default -> errorEnvelope(id, -32601, "Method not found");
            };

            if (isInitialize) {
                final var newSessionId = UUID.randomUUID().toString();
                sessions.add(newSessionId);
                exchange.response().header("Mcp-Session-Id", newSessionId);
            }

            final var jsonString = responseJson.toJsonString();

            if (acceptsSse(exchange)) {
                final var sseBody = SseEvent.of("message", jsonString).serialize()
                    .getBytes(StandardCharsets.UTF_8);
                exchange.response()
                    .status(200)
                    .header("Content-Type", "text/event-stream")
                    .header("Cache-Control", "no-cache")
                    .send(sseBody);
            } else {
                exchange.response()
                    .header("Content-Type", "application/json")
                    .send(jsonString.getBytes(StandardCharsets.UTF_8));
            }
        };
    }

    private boolean acceptsSse(final Exchange exchange) {
        return exchange.request().header("Accept")
            .map(a -> a.contains("text/event-stream"))
            .orElse(false);
    }

    /**
     * Returns a {@link Publisher} of {@link ToolCallEvent}s.
     *
     * <p>Subscribers receive one event per tool invocation, published after the
     * tool returns (or throws). Subscribers are observers only — they do not
     * participate in dispatch and cannot alter the result seen by the caller.
     *
     * @return the tool-call event publisher
     */
    public Publisher<ToolCallEvent> toolCallEvents() {
        return toolCallEvents;
    }

    private JsonObject handleInitialize() {
        final var capabilities = JsonObject.builder()
            .put("tools", JsonObject.builder().put("listChanged", false).build())
            .build();

        final var serverInfo = JsonObject.builder()
            .put("name", info.name())
            .put("version", info.version())
            .build();

        return JsonObject.builder()
            .put("protocolVersion", "2025-03-26")
            .put("capabilities", capabilities)
            .put("serverInfo", serverInfo)
            .build();
    }

    private JsonObject handleToolsList() {
        final var toolsArray = JsonArray.builder();
        for (final var tool : tools.values()) {
            toolsArray.add(JsonObject.builder()
                .put("name", tool.name())
                .put("description", tool.description())
                .put("inputSchema", tool.inputSchema())
                .build());
        }

        return JsonObject.builder()
            .put("tools", toolsArray.build())
            .build();
    }

    private JsonObject handleToolsCall(final JsonValue params, final String sessionId, final JsonValue id) {
        final var paramsObj = params instanceof JsonObject p ? p : JsonObject.builder().build();
        final var toolName = getString(paramsObj, "name");
        final var arguments = paramsObj.members().getOrDefault("arguments", JsonNull.INSTANCE);

        final var tool = tools.get(toolName);
        if (tool == null) {
            return errorEnvelope(id, -32602, "Unknown tool: " + toolName);
        }

        final var start = System.currentTimeMillis();
        try {
            final var toolResult = ScopedValue.where(SESSION_ID, sessionId).call(() -> tool.call(arguments));
            final var duration = System.currentTimeMillis() - start;
            toolCallEvents.publish(ToolCallEvent.success(sessionId, toolName, arguments, toolResult, duration));
            return envelope(id, buildToolResultJson(toolResult));
        } catch (final Exception e) {
            final var duration = System.currentTimeMillis() - start;
            toolCallEvents.publish(ToolCallEvent.failure(sessionId, toolName, arguments, e, duration));
            return envelope(id, buildToolResultJson(McpToolResult.error(e.getMessage())));
        }
    }

    private static JsonObject buildToolResultJson(final McpToolResult toolResult) {
        final var contentArray = JsonArray.builder();
        for (final var content : toolResult.content()) {
            final var contentNode = switch (content) {
                case McpContent.Text text -> JsonObject.builder()
                    .put("type", "text")
                    .put("text", text.text())
                    .build();
                case McpContent.Image image -> JsonObject.builder()
                    .put("type", "image")
                    .put("data", image.data())
                    .put("mimeType", image.mimeType())
                    .build();
                case McpContent.Resource resource -> JsonObject.builder()
                    .put("type", "resource")
                    .put("resource", JsonObject.builder()
                        .put("uri", resource.uri())
                        .put("mimeType", resource.mimeType())
                        .put("blob", resource.blob())
                        .build())
                    .build();
            };
            contentArray.add(contentNode);
        }

        return JsonObject.builder()
            .put("content", contentArray.build())
            .put("isError", toolResult.isError())
            .build();
    }

    private static JsonObject envelope(final JsonValue id, final JsonValue result) {
        final var builder = JsonObject.builder().put("jsonrpc", "2.0");
        addId(builder, id);
        builder.put("result", result);
        return builder.build();
    }

    private static JsonObject errorEnvelope(final JsonValue id, final int code, final String message) {
        final var error = JsonObject.builder()
            .put("code", code)
            .put("message", message)
            .build();
        final var builder = JsonObject.builder().put("jsonrpc", "2.0");
        addId(builder, id);
        builder.put("error", error);
        return builder.build();
    }

    private static void addId(final JsonObject.Builder builder, final JsonValue id) {
        if (id instanceof JsonNumber n) {
            builder.put("id", n.toNumber());
        } else if (id instanceof JsonString s) {
            builder.put("id", s.value());
        } else {
            builder.put("id", JsonNull.INSTANCE);
        }
    }

    private static String getString(final JsonObject obj, final String key) {
        final var val = obj.members().get(key);
        return val instanceof JsonString s ? s.value() : "";
    }

    /**
     * Creates a new {@link Builder}.
     *
     * @param name    the server name
     * @param version the server version
     * @return the builder
     */
    public static Builder builder(final String name,
                                  final String version) {
        return new Builder(name, version);
    }

    /**
     * A builder for {@link McpServer}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private final String name;
        private final String version;
        private final List<McpTool> tools = new ArrayList<>();

        private Builder(final String name, final String version) {
            this.name = name;
            this.version = version;
        }

        /**
         * Adds a tool to this server.
         *
         * @param tool the tool
         * @return this builder
         */
        public Builder tool(final McpTool tool) {
            tools.add(tool);
            return this;
        }

        /**
         * Builds the {@link McpServer}.
         *
         * @return the server
         */
        public McpServer build() {
            return new McpServer(this);
        }
    }
}
