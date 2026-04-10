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
package build.serve.foundation.context;

import build.base.telemetry.TelemetryRecorder;
import build.serve.foundation.Exchange;

import java.time.Instant;
import java.util.UUID;

/**
 * Defines standard {@link ScopedValue}s for request context propagation.
 * <p>
 * The transport layer binds these values for each incoming request so that any code in the handler
 * chain can access them without explicit parameter passing.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class RequestContext {

    /**
     * The current {@link Exchange} for this request.
     */
    public static final ScopedValue<Exchange> EXCHANGE = ScopedValue.newInstance();

    /**
     * A unique request ID (UUID string).
     */
    public static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    /**
     * The authenticated principal (if any). {@link Object} to avoid coupling to a security framework.
     */
    public static final ScopedValue<Object> PRINCIPAL = ScopedValue.newInstance();

    /**
     * The request start time (for timing).
     */
    public static final ScopedValue<Instant> START_TIME = ScopedValue.newInstance();

    /**
     * The {@link TelemetryRecorder} for the current request scope.
     */
    public static final ScopedValue<TelemetryRecorder> TELEMETRY = ScopedValue.newInstance();

    private RequestContext() {
        // utility class
    }

    /**
     * Runs the specified action with standard request context bound.
     *
     * @param exchange the {@link Exchange} for this request
     * @param action   the action to run
     */
    public static void run(final Exchange exchange, final Runnable action) {
        ScopedValue.where(EXCHANGE, exchange)
            .where(REQUEST_ID, UUID.randomUUID().toString())
            .where(START_TIME, Instant.now())
            .run(action);
    }

    /**
     * Calls the specified action with standard request context bound.
     *
     * @param exchange the {@link Exchange} for this request
     * @param action   the action to call
     * @param <T>      the result type
     * @param <X>      the exception type
     * @return the result of the action
     * @throws X if the action throws
     */
    public static <T, X extends Throwable> T call(final Exchange exchange,
                                                  final ScopedValue.CallableOp<T, X> action) throws X {
        return ScopedValue.where(EXCHANGE, exchange)
            .where(REQUEST_ID, UUID.randomUUID().toString())
            .where(START_TIME, Instant.now())
            .call(action);
    }
}
