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

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A prompt template exposed by an MCP server that clients can list and render.
 *
 * <p>Prompts are user-controlled slash-command-style templates. When a client calls
 * {@code prompts/get}, the server fills in the prompt's arguments and returns a list
 * of ready-to-use messages.
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public interface McpPrompt {

    /**
     * Returns the unique name of this prompt.
     *
     * @return the prompt name
     */
    String name();

    /**
     * Returns an optional human-readable description of this prompt.
     *
     * @return the description
     */
    Optional<String> description();

    /**
     * Returns the arguments this prompt accepts. Defaults to an empty list.
     *
     * @return the argument descriptors
     */
    default List<McpPromptArgument> arguments() {
        return List.of();
    }

    /**
     * Renders this prompt with the given argument values and returns the resulting messages.
     *
     * @param arguments the caller-supplied argument values (keys match {@link McpPromptArgument#name()})
     * @return the rendered prompt result
     * @throws Exception if rendering fails
     */
    McpPromptResult get(Map<String, String> arguments) throws Exception;
}
