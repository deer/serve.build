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

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * An event published after each MCP tool invocation, regardless of outcome.
 *
 * <p>Subscribers attached via {@link build.base.flow.Publisher#subscribe} on
 * {@link McpServer#toolCallEvents()} receive one event per tool call.
 * The event carries the tool name, arguments, elapsed time, and either the
 * successful result or the thrown error.
 *
 * @param timestamp  the instant the invocation completed
 * @param sessionId  the MCP session ID that originated this call; {@code "local"} for sessionless calls
 * @param toolName   the name of the invoked tool
 * @param arguments  the raw JSON arguments passed to the tool
 * @param result     the tool result on success; empty on failure
 * @param durationMs wall-clock milliseconds from call start to finish
 * @param error      the thrown exception on failure; empty on success
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record ToolCallEvent(Instant timestamp,
                            String sessionId,
                            String toolName,
                            JsonNode arguments,
                            Optional<McpToolResult> result,
                            long durationMs,
                            Optional<Throwable> error) {

    /**
     * Creates a successful tool call event.
     *
     * @param sessionId  the MCP session ID; {@code "local"} for sessionless calls
     * @param toolName   the tool name
     * @param arguments  the arguments passed to the tool
     * @param result     the successful result
     * @param durationMs elapsed milliseconds
     * @return the event
     */
    public static ToolCallEvent success(final String sessionId,
                                        final String toolName,
                                        final JsonNode arguments,
                                        final McpToolResult result,
                                        final long durationMs) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(result, "result must not be null");
        return new ToolCallEvent(
            Instant.now(),
            sessionId,
            toolName,
            arguments,
            Optional.of(result),
            durationMs,
            Optional.empty()
        );
    }

    /**
     * Creates a failed tool call event.
     *
     * @param sessionId  the MCP session ID; {@code "local"} for sessionless calls
     * @param toolName   the tool name
     * @param arguments  the arguments passed to the tool
     * @param error      the exception thrown by the tool
     * @param durationMs elapsed milliseconds
     * @return the event
     */
    public static ToolCallEvent failure(final String sessionId,
                                        final String toolName,
                                        final JsonNode arguments,
                                        final Throwable error,
                                        final long durationMs) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        Objects.requireNonNull(toolName, "toolName must not be null");
        Objects.requireNonNull(arguments, "arguments must not be null");
        Objects.requireNonNull(error, "error must not be null");
        return new ToolCallEvent(
            Instant.now(),
            sessionId,
            toolName,
            arguments,
            Optional.empty(),
            durationMs,
            Optional.of(error)
        );
    }
}
