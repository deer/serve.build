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
import build.serve.sse.SseEmitter;
import build.serve.sse.SseEvent;
import build.serve.sse.SseUpgrade;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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
    private final Map<String, McpResource> resources;
    private final List<TemplateEntry> templates;
    private final SubscriberRegistry<ToolCallEvent> toolCallEvents = new SubscriberRegistry<>();
    private final ConcurrentHashMap<String, SessionState> sessions = new ConcurrentHashMap<>();

    private McpServer(final Builder builder) {
        this.info = new McpServerInfo(builder.name, builder.version);

        final var toolMap = new LinkedHashMap<String, McpTool>();
        for (final var tool : builder.tools) {
            toolMap.put(tool.name(), tool);
        }
        this.tools = Map.copyOf(toolMap);

        final var resourceMap = new LinkedHashMap<String, McpResource>();
        for (final var resource : builder.resources) {
            resourceMap.put(resource.uri(), resource);
        }
        this.resources = Map.copyOf(resourceMap);

        final var templateList = new ArrayList<TemplateEntry>();
        for (final var template : builder.templates) {
            templateList.add(new TemplateEntry(template, uriTemplateToPattern(template.uriTemplate())));
        }
        this.templates = List.copyOf(templateList);
    }

    private static final class SessionState {
        final java.util.Set<String> subscriptions = ConcurrentHashMap.newKeySet();
        volatile SseEmitter emitter;
    }

    private record TemplateEntry(McpResourceTemplate template, Pattern pattern) {
    }

    private static Pattern uriTemplateToPattern(final String uriTemplate) {
        final var sb = new StringBuilder();
        final var m = Pattern.compile("\\{\\+?([^}]+)}").matcher(uriTemplate);
        int pos = 0;
        while (m.find()) {
            sb.append(Pattern.quote(uriTemplate.substring(pos, m.start())));
            sb.append(m.group().startsWith("{+") ? ".+" : "[^/]+");
            pos = m.end();
        }
        sb.append(Pattern.quote(uriTemplate.substring(pos)));
        return Pattern.compile(sb.toString());
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

            if ("GET".equalsIgnoreCase(method)) {
                handleSseConnection(exchange);
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                exchange.response().status(405).send("Method Not Allowed");
                return;
            }

            final var sessionId = exchange.request().header("Mcp-Session-Id");
            if (sessionId.isPresent() && !sessions.containsKey(sessionId.get())) {
                exchange.response().status(404).send("Session not found");
                return;
            }

            final var body = exchange.request().bodyAsString();
            final var request = Json.parse(body).asObject();
            final var isInitialize = "initialize".equals(getString(request, "method"));

            final var maybeResponse = dispatch(request, sessionId.orElse("local"));

            if (maybeResponse.isEmpty()) {
                exchange.response().status(202).send("");
                return;
            }

            if (isInitialize) {
                final var newSessionId = UUID.randomUUID().toString();
                sessions.put(newSessionId, new SessionState());
                exchange.response().header("Mcp-Session-Id", newSessionId);
            }

            final var jsonString = maybeResponse.get().toJsonString();

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

    /**
     * Runs the MCP stdio transport, reading newline-delimited JSON-RPC from {@code in} and
     * writing newline-delimited JSON responses to {@code out}. Blocks until {@code in} reaches EOF.
     * <p>
     * All tool calls use the session ID {@code "local"}.
     *
     * @param in  the input stream (e.g. {@code System.in})
     * @param out the output stream (e.g. {@code System.out})
     */
    public void stdioLoop(final InputStream in,
                          final OutputStream out) {
        final var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        final var writer = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8), true);
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                final var request = Json.parse(line).asObject();
                dispatch(request, "local").ifPresent(response -> writer.println(response.toJsonString()));
            }
        } catch (final IOException e) {
            // EOF or closed stream — exit normally
        }
    }

    private Optional<JsonObject> dispatch(final JsonObject request,
                                          final String sessionId) {
        final var rpcMethod = getString(request, "method");
        final var id = request.members().get("id");

        if (id == null || id instanceof JsonNull) {
            return Optional.empty();
        }

        final var response = switch (rpcMethod) {
            case "initialize" -> envelope(id, handleInitialize());
            case "ping" -> envelope(id, JsonObject.builder().build());
            case "tools/list" -> envelope(id, handleToolsList());
            case "tools/call" -> {
                final var params = request.members().getOrDefault("params", JsonNull.INSTANCE);
                yield handleToolsCall(params, sessionId, id);
            }
            case "resources/list" -> envelope(id, handleResourcesList());
            case "resources/templates/list" -> envelope(id, handleResourcesTemplatesList());
            case "resources/read" -> {
                final var params = request.members().getOrDefault("params", JsonNull.INSTANCE);
                yield handleResourcesRead(params, id);
            }
            case "resources/subscribe" -> {
                final var params = request.members().getOrDefault("params", JsonNull.INSTANCE);
                yield handleResourcesSubscribe(params, sessionId, id);
            }
            case "resources/unsubscribe" -> {
                final var params = request.members().getOrDefault("params", JsonNull.INSTANCE);
                yield handleResourcesUnsubscribe(params, sessionId, id);
            }
            default -> errorEnvelope(id, -32601, "Method not found");
        };

        return Optional.of(response);
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
            .put("resources", JsonObject.builder()
                .put("subscribe", true)
                .put("listChanged", false)
                .build())
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

    private void handleSseConnection(final Exchange exchange) throws Exception {
        final var sessionId = exchange.request().header("Mcp-Session-Id").orElse(null);
        if (sessionId == null || !sessions.containsKey(sessionId)) {
            exchange.response().status(404).send("Session not found");
            return;
        }
        final var state = sessions.get(sessionId);
        SseUpgrade.sse(emitter -> {
            state.emitter = emitter;
            try {
                while (emitter.isOpen()) {
                    Thread.sleep(500);
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                state.emitter = null;
            }
        }).handle(exchange);
    }

    /**
     * Sends a {@code notifications/resources/updated} event to all sessions subscribed to
     * the given URI. Call this whenever the content of a resource changes.
     *
     * @param uri the URI of the changed resource
     */
    public void notifyResourceChanged(final String uri) {
        final var notification = JsonObject.builder()
            .put("jsonrpc", "2.0")
            .put("method", "notifications/resources/updated")
            .put("params", JsonObject.builder().put("uri", uri).build())
            .build()
            .toJsonString();
        final var event = SseEvent.of("message", notification);

        for (final var state : sessions.values()) {
            if (state.subscriptions.contains(uri)) {
                final var emitter = state.emitter;
                if (emitter != null && emitter.isOpen()) {
                    try {
                        emitter.send(event);
                    } catch (final IOException ignored) {
                    }
                }
            }
        }
    }

    private JsonObject handleResourcesList() {
        final var resourcesArray = JsonArray.builder();
        for (final var resource : resources.values()) {
            final var builder = JsonObject.builder()
                .put("uri", resource.uri())
                .put("name", resource.name());
            resource.description().ifPresent(d -> builder.put("description", d));
            resource.mimeType().ifPresent(m -> builder.put("mimeType", m));
            resourcesArray.add(builder.build());
        }
        return JsonObject.builder()
            .put("resources", resourcesArray.build())
            .build();
    }

    private JsonObject handleResourcesTemplatesList() {
        final var templatesArray = JsonArray.builder();
        for (final var entry : templates) {
            final var t = entry.template();
            final var builder = JsonObject.builder()
                .put("uriTemplate", t.uriTemplate())
                .put("name", t.name());
            t.description().ifPresent(d -> builder.put("description", d));
            t.mimeType().ifPresent(m -> builder.put("mimeType", m));
            templatesArray.add(builder.build());
        }
        return JsonObject.builder()
            .put("resourceTemplates", templatesArray.build())
            .build();
    }

    private JsonObject handleResourcesRead(final JsonValue params, final JsonValue id) {
        final var paramsObj = params instanceof JsonObject p ? p : JsonObject.builder().build();
        final var uri = getString(paramsObj, "uri");

        final McpResourceContent content;
        try {
            final var exact = resources.get(uri);
            if (exact != null) {
                content = exact.read();
            } else {
                final var matched = findTemplate(uri);
                if (matched == null) {
                    return errorEnvelope(id, -32002, "Resource not found: " + uri);
                }
                content = matched.read(uri);
            }
        } catch (final Exception e) {
            return errorEnvelope(id, -32002, "Failed to read resource: " + e.getMessage());
        }

        final var contentNode = switch (content) {
            case McpResourceContent.Text text -> JsonObject.builder()
                .put("uri", text.uri())
                .put("mimeType", text.mimeType())
                .put("text", text.text())
                .build();
            case McpResourceContent.Blob blob -> JsonObject.builder()
                .put("uri", blob.uri())
                .put("mimeType", blob.mimeType())
                .put("blob", blob.blob())
                .build();
        };
        return envelope(id, JsonObject.builder()
            .put("contents", JsonArray.builder().add(contentNode).build())
            .build());
    }

    private McpResourceTemplate findTemplate(final String uri) {
        for (final var entry : templates) {
            if (entry.pattern().matcher(uri).matches()) {
                return entry.template();
            }
        }
        return null;
    }

    private JsonObject handleResourcesSubscribe(final JsonValue params, final String sessionId, final JsonValue id) {
        final var paramsObj = params instanceof JsonObject p ? p : JsonObject.builder().build();
        final var uri = getString(paramsObj, "uri");
        final var state = sessions.get(sessionId);
        if (state != null) {
            state.subscriptions.add(uri);
        }
        return envelope(id, JsonObject.builder().build());
    }

    private JsonObject handleResourcesUnsubscribe(final JsonValue params, final String sessionId, final JsonValue id) {
        final var paramsObj = params instanceof JsonObject p ? p : JsonObject.builder().build();
        final var uri = getString(paramsObj, "uri");
        final var state = sessions.get(sessionId);
        if (state != null) {
            state.subscriptions.remove(uri);
        }
        return envelope(id, JsonObject.builder().build());
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
        private final List<McpResource> resources = new ArrayList<>();
        private final List<McpResourceTemplate> templates = new ArrayList<>();

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
         * Adds a resource to this server.
         *
         * @param resource the resource
         * @return this builder
         */
        public Builder resource(final McpResource resource) {
            resources.add(resource);
            return this;
        }

        /**
         * Adds a URI-template-based resource to this server.
         *
         * @param template the resource template
         * @return this builder
         */
        public Builder template(final McpResourceTemplate template) {
            templates.add(template);
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
