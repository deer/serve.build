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

import build.base.flow.Subscriber;
import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.foundation.PrintStreamTelemetryRecorder;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * A {@link Subscriber} that logs {@link ToolCallEvent}s via a {@link TelemetryRecorder}.
 *
 * <ul>
 *   <li>STARTED events are logged at diagnostic level.</li>
 *   <li>SUCCEEDED events are logged at info level; events exceeding the slow-call threshold
 *       are logged at warn level instead.</li>
 *   <li>FAILED events are logged at warn level with the thrown exception.</li>
 * </ul>
 *
 * <p>Attach via {@link McpServer#toolCallEvents()}:
 * <pre>{@code
 * mcpServer.toolCallEvents().subscribe(McpToolCallLogger.of(myRecorder));
 * }</pre>
 *
 * <p>Or enable automatically at build time:
 * <pre>{@code
 * McpServer.builder("name", "1.0.0")
 *     .logToolCalls()
 *     .build();
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public final class McpToolCallLogger implements Subscriber<ToolCallEvent> {

    private static final Duration DEFAULT_SLOW_CALL_THRESHOLD = Duration.ofSeconds(5);

    private final TelemetryRecorder recorder;
    private final Duration slowCallThreshold;

    private McpToolCallLogger(final TelemetryRecorder recorder, final Duration slowCallThreshold) {
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
        this.slowCallThreshold = Objects.requireNonNull(slowCallThreshold, "slowCallThreshold must not be null");
    }

    /**
     * Creates a logger using the given recorder and a 5-second slow-call threshold.
     *
     * @param recorder the recorder to log to
     * @return the logger
     */
    public static McpToolCallLogger of(final TelemetryRecorder recorder) {
        return new McpToolCallLogger(recorder, DEFAULT_SLOW_CALL_THRESHOLD);
    }

    /**
     * Creates a logger using the given recorder and slow-call threshold.
     *
     * @param recorder          the recorder to log to
     * @param slowCallThreshold calls exceeding this duration are logged at warn instead of info
     * @return the logger
     */
    public static McpToolCallLogger of(final TelemetryRecorder recorder, final Duration slowCallThreshold) {
        return new McpToolCallLogger(recorder, slowCallThreshold);
    }

    /**
     * Creates a logger using a stderr-only recorder and a 5-second slow-call threshold.
     *
     * @return the logger
     */
    public static McpToolCallLogger defaults() {
        return new McpToolCallLogger(
            PrintStreamTelemetryRecorder.of(URI.create("serve://mcp/tool-calls"), System.err, System.err),
            DEFAULT_SLOW_CALL_THRESHOLD);
    }

    @Override
    public void onNext(final ToolCallEvent event) {
        final var toolName = event.toolName();
        final var id = event.invocationId();

        switch (event.phase()) {
            case STARTED -> recorder.diagnostic("tool-call %s started [#%d]", toolName, id);
            case SUCCEEDED -> {
                final var ms = event.duration().map(Duration::toMillis).orElse(0L);
                if (event.duration().map(d -> d.compareTo(slowCallThreshold) >= 0).orElse(false)) {
                    recorder.warn("tool-call %s slow: %dms (threshold: %dms) [#%d]",
                        toolName, ms, slowCallThreshold.toMillis(), id);
                } else {
                    recorder.info("tool-call %s succeeded in %dms [#%d]", toolName, ms, id);
                }
            }
            case FAILED -> {
                final var ms = event.duration().map(Duration::toMillis).orElse(0L);
                final var err = event.error().orElse(null);
                recorder.warn(err, "tool-call %s failed in %dms [#%d]", toolName, ms, id);
            }
        }
    }
}
