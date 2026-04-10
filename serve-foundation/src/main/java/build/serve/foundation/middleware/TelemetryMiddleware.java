/*-
 * #%L
 * Serve Foundation
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
package build.serve.foundation.middleware;

import build.base.telemetry.TelemetryRecorder;
import build.serve.foundation.Handler;

import java.util.Objects;

/**
 * A {@link Middleware} that wraps each request in a telemetry {@link build.base.telemetry.Activity}.
 * <p>
 * On success the activity is completed; on failure the error is recorded and the activity
 * is completed exceptionally before re-throwing.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class TelemetryMiddleware implements Middleware {

    /**
     * The {@link TelemetryRecorder} to use.
     */
    private final TelemetryRecorder recorder;

    /**
     * Constructs a {@link TelemetryMiddleware}.
     *
     * @param recorder the {@link TelemetryRecorder} to record activities with
     */
    public TelemetryMiddleware(final TelemetryRecorder recorder) {
        this.recorder = Objects.requireNonNull(recorder, "recorder must not be null");
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var method = exchange.request().method();
            final var path = exchange.request().path();
            final var activity = recorder.commence("%s %s", method, path);

            try {
                next.handle(exchange);
                activity.complete();
            } catch (final Exception e) {
                recorder.error(e, "%s %s failed", method, path);
                activity.completeExceptionally(e);
                throw e;
            }
        };
    }
}
