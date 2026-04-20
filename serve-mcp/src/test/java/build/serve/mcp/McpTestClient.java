package build.serve.mcp;

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

import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestRequest;
import build.serve.testing.TestServer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

/**
 * A fluent MCP test client that wraps a {@link TestServer} and handles session management
 * and JSON-RPC plumbing, so tests can focus on tool behavior rather than protocol details.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * try (var client = McpTestClient.start(mcpServer)) {
 *     client.initialize();
 *     var tools = client.listTools();
 *     var result = client.call("my_tool", Map.of("arg", "value"));
 * }
 * }</pre>
 */
final class McpTestClient implements AutoCloseable {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String MCP_PATH = "/mcp";

    private final TestServer server;
    private String sessionId;
    private int nextId = 1;

    private McpTestClient(final TestServer server) {
        this.server = server;
    }

    static McpTestClient start(final McpServer mcpServer) {
        final var server = TestServer.of(
            RouterBuilder.create().route(MCP_PATH, mcpServer.handler()).build());
        return new McpTestClient(server);
    }

    /**
     * Sends {@code initialize}, captures the session ID from the response, and returns the
     * full JSON-RPC response for assertion.
     */
    JsonNode initialize() throws Exception {
        final var response = postRaw(rpc("initialize", nextId++, Map.of()));
        sessionId = response.header("Mcp-Session-Id");
        return MAPPER.readTree(response.body());
    }

    /**
     * Returns the {@code tools} array from a {@code tools/list} call.
     */
    JsonNode listTools() throws Exception {
        return postJson(rpc("tools/list", nextId++, Map.of())).path("result").path("tools");
    }

    /**
     * Calls the named tool and returns the {@code result} object.
     */
    JsonNode call(final String toolName, final Map<String, Object> arguments) throws Exception {
        final var params = Map.of("name", toolName, "arguments", arguments);
        return postJson(rpc("tools/call", nextId++, params)).path("result");
    }

    /**
     * Sends a raw JSON-RPC request and returns the full response node.
     */
    JsonNode send(final String method, final Map<String, Object> params) throws Exception {
        return postJson(rpc(method, nextId++, params));
    }

    /**
     * Delegates to the underlying {@link TestServer} for direct HTTP access.
     */
    TestRequest get(final String path) {
        return server.get(path);
    }

    /**
     * Delegates to the underlying {@link TestServer} for direct HTTP access.
     */
    TestRequest post(final String path) {
        return server.post(path);
    }

    @Override
    public void close() {
        server.close();
    }

    private String rpc(final String method, final int id, final Map<String, Object> params) throws Exception {
        final var node = MAPPER.createObjectNode()
            .put("jsonrpc", "2.0")
            .put("id", id)
            .put("method", method);
        node.set("params", MAPPER.valueToTree(params));
        return MAPPER.writeValueAsString(node);
    }

    private build.serve.testing.TestResponse postRaw(final String body) {
        var req = server.post(MCP_PATH).header("Content-Type", "application/json");
        if (sessionId != null) {
            req = req.header("Mcp-Session-Id", sessionId);
        }
        return req.body(body).send();
    }

    private JsonNode postJson(final String body) throws Exception {
        return MAPPER.readTree(postRaw(body).body());
    }
}
