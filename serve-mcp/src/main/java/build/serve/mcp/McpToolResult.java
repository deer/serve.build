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

import build.base.json.JsonBoolean;
import build.base.json.JsonObject;
import build.base.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * The result of an MCP tool invocation.
 *
 * @param content the content items
 * @param isError whether the result represents an error
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record McpToolResult(List<McpContent> content, boolean isError) {

    /**
     * Creates a successful text result.
     *
     * @param text the text
     * @return the result
     */
    public static McpToolResult text(final String text) {
        return new McpToolResult(List.of(new McpContent.Text(text)), false);
    }

    /**
     * Creates a successful result whose single content item is the compact JSON serialization
     * of the given value. Use {@link McpContent.Text#json()} to deserialize it on the other side.
     *
     * @param value the JSON value to serialize as text content
     * @return the result
     */
    public static McpToolResult json(final JsonValue value) {
        return text(value.toJsonString());
    }

    /**
     * Creates a successful result with text and embedded resource content items.
     *
     * @param text      the text description
     * @param resources resource content items to append
     * @return the result
     */
    public static McpToolResult withResources(final String text, final List<McpContent.Resource> resources) {
        final var content = new ArrayList<McpContent>();
        content.add(new McpContent.Text(text));
        content.addAll(resources);
        return new McpToolResult(List.copyOf(content), false);
    }

    /**
     * Creates an error result.
     *
     * @param message the error message
     * @return the result
     */
    public static McpToolResult error(final String message) {
        return new McpToolResult(List.of(new McpContent.Text(message)), true);
    }

    /**
     * Returns the first text content item parsed as a {@link JsonValue}.
     * Pairs with {@link #json(JsonValue)} for round-trip use.
     *
     * @return the parsed value
     * @throws IllegalStateException              if this result contains no text content
     * @throws build.base.json.JsonParseException if the text is not valid JSON
     */
    public JsonValue json() {
        for (final var item : content) {
            if (item instanceof McpContent.Text text) {
                return text.json();
            }
        }
        throw new IllegalStateException("No text content in result");
    }

    /**
     * Reconstructs an {@code McpToolResult} from the {@code result} object in a JSON-RPC
     * {@code tools/call} response — i.e. the value of the {@code "result"} key, not the full
     * response envelope.
     *
     * @param result the {@code result} object containing {@code content} and {@code isError}
     * @return the reconstructed result
     * @throws IllegalArgumentException if a content item carries an unrecognised {@code type}
     */
    public static McpToolResult fromJson(final JsonObject result) {
        final var isErrorVal = result.members().get("isError");
        final var isError = isErrorVal instanceof JsonBoolean b && b.value();

        final var items = new ArrayList<McpContent>();
        for (final var item : result.get("content").asArray().values()) {
            final var obj = item.asObject();
            final McpContent content = switch (obj.getString("type")) {
                case "text" -> new McpContent.Text(obj.getString("text"));
                case "image" -> new McpContent.Image(obj.getString("data"), obj.getString("mimeType"));
                case "audio" -> new McpContent.Audio(obj.getString("data"), obj.getString("mimeType"));
                case "resource" -> {
                    final var r = obj.get("resource").asObject();
                    if (r.members().containsKey("text")) {
                        yield McpContent.Resource.text(r.getString("uri"), r.getString("mimeType"), r.getString("text"));
                    } else {
                        yield McpContent.Resource.blob(r.getString("uri"), r.getString("mimeType"), r.getString("blob"));
                    }
                }
                default -> throw new IllegalArgumentException("Unknown content type: " + obj.getString("type"));
            };
            items.add(content);
        }
        return new McpToolResult(List.copyOf(items), isError);
    }
}
