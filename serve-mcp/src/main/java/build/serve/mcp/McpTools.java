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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Map;

/**
 * Factory helpers for building JSON Schema {@link ObjectNode} instances used as MCP tool input schemas.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class McpTools {

    private McpTools() {
    }

    /**
     * Builds a simple string property schema.
     *
     * @param mapper      the {@link ObjectMapper}
     * @param description the property description
     * @return the schema node
     */
    public static ObjectNode stringProperty(final ObjectMapper mapper, final String description) {
        final var node = mapper.createObjectNode();
        node.put("type", "string");
        node.put("description", description);
        return node;
    }

    /**
     * Builds an object schema with required string properties.
     *
     * @param mapper     the {@link ObjectMapper}
     * @param properties a map of property names to descriptions
     * @param required   the list of required property names
     * @return the schema node
     */
    public static ObjectNode schema(final ObjectMapper mapper,
                                    final Map<String, String> properties,
                                    final List<String> required) {
        final var schema = mapper.createObjectNode();
        schema.put("type", "object");

        final var props = mapper.createObjectNode();
        for (final var entry : properties.entrySet()) {
            props.set(entry.getKey(), stringProperty(mapper, entry.getValue()));
        }
        schema.set("properties", props);

        final var requiredArray = mapper.createArrayNode();
        for (final var name : required) {
            requiredArray.add(name);
        }
        schema.set("required", requiredArray);

        return schema;
    }
}
