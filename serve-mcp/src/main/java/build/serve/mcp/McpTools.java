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

import build.base.json.JsonArray;
import build.base.json.JsonObject;

import java.util.List;
import java.util.Map;

/**
 * Factory helpers for building JSON Schema {@link JsonObject} instances used as MCP tool input schemas.
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
     * @param description the property description
     * @return the schema node
     */
    public static JsonObject stringProperty(final String description) {
        return JsonObject.builder()
            .put("type", "string")
            .put("description", description)
            .build();
    }

    /**
     * Builds an object schema with required string properties.
     *
     * @param properties a map of property names to descriptions
     * @param required   the list of required property names
     * @return the schema node
     */
    public static JsonObject schema(final Map<String, String> properties,
                                    final List<String> required) {
        final var props = JsonObject.builder();
        for (final var entry : properties.entrySet()) {
            props.put(entry.getKey(), stringProperty(entry.getValue()));
        }

        final var requiredArray = JsonArray.builder();
        for (final var name : required) {
            requiredArray.add(name);
        }

        return JsonObject.builder()
            .put("type", "object")
            .put("properties", props.build())
            .put("required", requiredArray.build())
            .build();
    }
}
