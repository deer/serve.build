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
import java.util.OptionalDouble;

/**
 * Sends {@code notifications/progress} messages back to the client during a tool call.
 *
 * <p>A reporter is available to tool implementations via {@link McpServer#PROGRESS_REPORTER}
 * when the client included a {@code progressToken} in its {@code tools/call} request. When
 * no token is present the scoped value is unbound and tools should not report progress.
 *
 * <pre>{@code
 * if (McpServer.PROGRESS_REPORTER.isBound()) {
 *     var reporter = McpServer.PROGRESS_REPORTER.get();
 *     reporter.report(1, 10, "loading...");
 *     reporter.report(5, 10, "halfway");
 *     reporter.report(10, 10, "done");
 * }
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
@FunctionalInterface
public interface ProgressReporter {

    /**
     * Reports progress to the client.
     *
     * @param progress current progress value
     * @param total    optional total — when present, clients can compute a percentage
     * @param message  optional human-readable status message
     */
    void report(double progress, OptionalDouble total, Optional<String> message);

    /**
     * Reports progress without a total or message.
     *
     * @param progress current progress value
     */
    default void report(final double progress) {
        report(progress, OptionalDouble.empty(), Optional.empty());
    }

    /**
     * Reports progress with a total but no message.
     *
     * @param progress current progress value
     * @param total    the total against which progress is measured
     */
    default void report(final double progress, final double total) {
        report(progress, OptionalDouble.of(total), Optional.empty());
    }

    /**
     * Reports progress with both a total and a human-readable status message.
     *
     * @param progress current progress value
     * @param total    the total against which progress is measured
     * @param message  human-readable status message
     */
    default void report(final double progress, final double total, final String message) {
        report(progress, OptionalDouble.of(total), Optional.of(message));
    }
}
