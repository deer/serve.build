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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * The result of a {@code prompts/get} call: an optional description and a list of messages
 * that together constitute the filled-in prompt.
 *
 * @param description an optional human-readable description of this specific invocation
 * @param messages    the prompt messages in order
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public record McpPromptResult(Optional<String> description, List<McpPromptMessage> messages) {

    /**
     * Creates a result with messages and no description.
     *
     * @param messages the prompt messages
     * @return the result
     */
    public static McpPromptResult of(final McpPromptMessage... messages) {
        return new McpPromptResult(Optional.empty(), List.copyOf(Arrays.asList(messages)));
    }

    /**
     * Creates a result with a description and messages.
     *
     * @param description the description
     * @param messages    the prompt messages
     * @return the result
     */
    public static McpPromptResult of(final String description, final McpPromptMessage... messages) {
        return new McpPromptResult(Optional.of(description), List.copyOf(Arrays.asList(messages)));
    }
}
