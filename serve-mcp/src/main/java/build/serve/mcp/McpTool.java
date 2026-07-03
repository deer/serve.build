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

import build.base.json.JsonObject;
import build.base.json.JsonValue;

import java.util.Optional;

/**
 * A tool that can be invoked via the MCP protocol.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface McpTool {

    /**
     * Returns the name of this tool.
     *
     * @return the tool name
     */
    String name();

    /**
     * Returns a human-readable description of this tool.
     *
     * @return the description
     */
    String description();

    /**
     * Returns the JSON Schema describing the input parameters for this tool.
     *
     * @return the input schema as a {@link JsonObject}
     */
    JsonObject inputSchema();

    /**
     * Returns the JSON Schema describing the {@code structuredContent} this tool's result may carry.
     *
     * <p>Empty by default — most tools return unstructured content only. Present it as an object schema
     * (per the MCP spec's {@code outputSchema} field) when the tool's result includes
     * {@link McpToolResult#structuredContent()}, so clients can validate it.
     *
     * @return the output schema, or empty if this tool has no structured output
     */
    default Optional<JsonObject> outputSchema() {
        return Optional.empty();
    }

    /**
     * Returns optional behavioral hints for this tool.
     *
     * <p>These hints are informational only — clients may use them for UI decisions
     * but must not rely on them for security enforcement.
     *
     * @return the annotations, or empty if none
     */
    default Optional<McpToolAnnotations> annotations() {
        return Optional.empty();
    }

    /**
     * Invokes this tool with the specified arguments.
     *
     * @param arguments the input arguments as a {@link JsonValue}
     * @return the tool result
     * @throws Exception if an error occurs during invocation
     */
    McpToolResult call(JsonValue arguments) throws Exception;
}
