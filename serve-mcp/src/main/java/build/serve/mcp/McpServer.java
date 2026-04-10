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

import build.serve.foundation.Handler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * An MCP (Model Context Protocol) server that exposes tools via JSON-RPC 2.0 over HTTP.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class McpServer {

    private final McpServerInfo info;
    private final Map<String, McpTool> tools;
    private final ObjectMapper mapper;

    private McpServer(final Builder builder) {
        this.info = new McpServerInfo(builder.name, builder.version);
        this.mapper = new ObjectMapper();

        final var toolMap = new LinkedHashMap<String, McpTool>();
        for (final var tool : builder.tools) {
            toolMap.put(tool.name(), tool);
        }
        this.tools = Map.copyOf(toolMap);
    }

    /**
     * Returns a serve.build {@link Handler} that handles the MCP endpoint.
     * <p>
     * Handles POST for JSON-RPC requests and returns 405 for GET.
     *
     * @return the handler
     */
    public Handler handler() {
        return exchange -> {
            final var method = exchange.request().method();

            if ("GET".equalsIgnoreCase(method)) {
                exchange.response().status(405).send("Method Not Allowed");
                return;
            }

            if (!"POST".equalsIgnoreCase(method)) {
                exchange.response().status(405).send("Method Not Allowed");
                return;
            }

            final var body = exchange.request().bodyAsString();
            final var request = mapper.readTree(body);

            final var rpcMethod = request.path("method").asText("");
            final var id = request.get("id");

            // Notification (no id) — respond 202
            if (id == null || id.isNull()) {
                exchange.response().status(202).send("");
                return;
            }

            final var result = switch (rpcMethod) {
                case "initialize" -> handleInitialize();
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolsCall(request.path("params"));
                default -> errorResponse(-32601, "Method not found");
            };

            result.put("jsonrpc", "2.0");

            if (id.isNumber()) {
                result.put("id", id.asInt());
            } else {
                result.put("id", id.asText());
            }

            final var json = mapper.writeValueAsBytes(result);
            exchange.response()
                .header("Content-Type", "application/json")
                .send(json);
        };
    }

    private ObjectNode handleInitialize() {
        final var result = mapper.createObjectNode();

        final var resultObj = mapper.createObjectNode();
        resultObj.put("protocolVersion", "2025-03-26");

        final var capabilities = mapper.createObjectNode();
        final var toolsCap = mapper.createObjectNode();
        toolsCap.put("listChanged", false);
        capabilities.set("tools", toolsCap);
        resultObj.set("capabilities", capabilities);

        final var serverInfo = mapper.createObjectNode();
        serverInfo.put("name", info.name());
        serverInfo.put("version", info.version());
        resultObj.set("serverInfo", serverInfo);

        result.set("result", resultObj);
        return result;
    }

    private ObjectNode handleToolsList() {
        final var result = mapper.createObjectNode();
        final var resultObj = mapper.createObjectNode();

        final var toolsArray = mapper.createArrayNode();
        for (final var tool : tools.values()) {
            final var toolNode = mapper.createObjectNode();
            toolNode.put("name", tool.name());
            toolNode.put("description", tool.description());
            toolNode.set("inputSchema", tool.inputSchema());
            toolsArray.add(toolNode);
        }
        resultObj.set("tools", toolsArray);

        result.set("result", resultObj);
        return result;
    }

    private ObjectNode handleToolsCall(final JsonNode params) {
        final var toolName = params.path("name").asText("");
        final var arguments = params.path("arguments");

        final var tool = tools.get(toolName);
        if (tool == null) {
            return errorResponse(-32602, "Unknown tool: " + toolName);
        }

        try {
            final var toolResult = tool.call(arguments);
            return buildToolResult(toolResult);
        } catch (final Exception e) {
            return buildToolResult(McpToolResult.error(e.getMessage()));
        }
    }

    private ObjectNode buildToolResult(final McpToolResult toolResult) {
        final var result = mapper.createObjectNode();
        final var resultObj = mapper.createObjectNode();

        final var contentArray = mapper.createArrayNode();
        for (final var content : toolResult.content()) {
            final var contentNode = mapper.createObjectNode();
            switch (content) {
                case McpContent.Text text -> {
                    contentNode.put("type", "text");
                    contentNode.put("text", text.text());
                }
                case McpContent.Image image -> {
                    contentNode.put("type", "image");
                    contentNode.put("data", image.data());
                    contentNode.put("mimeType", image.mimeType());
                }
            }
            contentArray.add(contentNode);
        }
        resultObj.set("content", contentArray);
        resultObj.put("isError", toolResult.isError());

        result.set("result", resultObj);
        return result;
    }

    private ObjectNode errorResponse(final int code, final String message) {
        final var result = mapper.createObjectNode();
        final var error = mapper.createObjectNode();
        error.put("code", code);
        error.put("message", message);
        result.set("error", error);
        return result;
    }

    /**
     * Creates a new {@link Builder}.
     *
     * @param name    the server name
     * @param version the server version
     * @return the builder
     */
    public static Builder builder(final String name, final String version) {
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
