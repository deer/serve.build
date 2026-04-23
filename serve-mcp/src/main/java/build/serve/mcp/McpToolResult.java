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
}
