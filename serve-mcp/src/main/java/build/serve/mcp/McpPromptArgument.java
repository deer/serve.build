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

import java.util.Optional;

/**
 * Describes a named argument accepted by an {@link McpPrompt}.
 *
 * @param name        the argument name
 * @param description an optional human-readable description
 * @param required    whether the argument must be supplied by the caller
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public record McpPromptArgument(String name, Optional<String> description, boolean required) {

    /**
     * Creates a required argument with a description.
     *
     * @param name        the argument name
     * @param description the description
     * @return the argument
     */
    public static McpPromptArgument required(final String name, final String description) {
        return new McpPromptArgument(name, Optional.of(description), true);
    }

    /**
     * Creates a required argument with no description.
     *
     * @param name the argument name
     * @return the argument
     */
    public static McpPromptArgument required(final String name) {
        return new McpPromptArgument(name, Optional.empty(), true);
    }

    /**
     * Creates an optional argument with a description.
     *
     * @param name        the argument name
     * @param description the description
     * @return the argument
     */
    public static McpPromptArgument optional(final String name, final String description) {
        return new McpPromptArgument(name, Optional.of(description), false);
    }

    /**
     * Creates an optional argument with no description.
     *
     * @param name the argument name
     * @return the argument
     */
    public static McpPromptArgument optional(final String name) {
        return new McpPromptArgument(name, Optional.empty(), false);
    }
}
