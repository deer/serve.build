/*-
 * #%L
 * Serve Logging
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
package build.serve.logging;

import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.foundation.PrintStreamTelemetryRecorder;
import build.serve.foundation.Handler;
import build.serve.foundation.context.RequestContext;
import build.serve.foundation.middleware.Middleware;
import build.serve.foundation.util.JsonStrings;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Middleware} that logs HTTP requests with method, path, status code, and duration.
 * <p>
 * Slow requests (above a configurable threshold) are logged at WARN level. Paths can be excluded from logging.
 * Use {@link #defaults()} for sensible defaults or {@link #builder()} for fine-grained configuration.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class RequestLoggingMiddleware implements Middleware {

    private static final TelemetryRecorder DEFAULT_RECORDER =
        PrintStreamTelemetryRecorder.of(URI.create("serve://logging"), System.out, System.err);

    private final boolean includeHeaders;
    private final boolean includeQueryString;
    private final Set<String> excludedPaths;
    private final Duration slowRequestThreshold;
    private final LogFormat format;
    private final TelemetryRecorder recorder;

    private RequestLoggingMiddleware(final Builder builder) {
        this.includeHeaders = builder.includeHeaders;
        this.includeQueryString = builder.includeQueryString;
        this.excludedPaths = Set.copyOf(builder.excludedPaths);
        this.slowRequestThreshold = builder.slowRequestThreshold;
        this.format = builder.format;
        this.recorder = builder.recorder;
    }

    /**
     * Creates a {@link RequestLoggingMiddleware} with sensible defaults.
     *
     * @return a default {@link RequestLoggingMiddleware}
     */
    public static RequestLoggingMiddleware defaults() {
        return builder().build();
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link RequestLoggingMiddleware}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var request = exchange.request();
            final var path = request.path();

            if (excludedPaths.contains(path)) {
                next.handle(exchange);

                return;
            }

            final var method = request.method();
            final var displayPath = buildDisplayPath(request);
            final var startTime = System.nanoTime();

            try {
                next.handle(exchange);

                final var durationMs = (System.nanoTime() - startTime) / 1_000_000;
                final var status = exchange.response().status();
                final var message = formatMessage(format, method, displayPath, status, durationMs);
                final var slow = slowRequestThreshold != null && durationMs >= slowRequestThreshold.toMillis();

                if (slow) {
                    recorder.warn(format == LogFormat.JSON ? appendJsonField(message, "slow", true) : message + " [SLOW]");
                } else {
                    recorder.info(message);
                }

                if (includeHeaders && format != LogFormat.JSON) {
                    recorder.info("  Headers: " + request.headers());
                }
            } catch (final Exception exception) {
                final var durationMs = (System.nanoTime() - startTime) / 1_000_000;
                recorder.error(method + " " + displayPath + " -> ERROR (" + durationMs + "ms) "
                    + exception.getClass().getSimpleName());

                throw exception;
            }
        };
    }

    private String buildDisplayPath(final build.serve.foundation.Request request) {
        if (includeQueryString) {
            final var query = request.uri().getRawQuery();

            if (query != null && !query.isEmpty()) {
                return request.path() + "?" + query;
            }
        }

        return request.path();
    }

    private static String formatMessage(final LogFormat format, final String method, final String path,
                                        final int status, final long durationMs) {
        if (format == LogFormat.JSON) {
            final var requestId = RequestContext.REQUEST_ID.isBound() ? RequestContext.REQUEST_ID.get() : null;
            final var sb = new StringBuilder(128);

            sb.append("{\"method\":\"").append(JsonStrings.escape(method)).append("\"");
            sb.append(",\"path\":\"").append(JsonStrings.escape(path)).append("\"");
            sb.append(",\"status\":").append(status);
            sb.append(",\"duration_ms\":").append(durationMs);

            if (requestId != null) {
                sb.append(",\"request_id\":\"").append(JsonStrings.escape(requestId)).append("\"");
            }

            sb.append('}');

            return sb.toString();
        }

        return method + " " + path + " -> " + status + " (" + durationMs + "ms)";
    }

    private static String appendJsonField(final String jsonObject, final String key, final boolean value) {
        return jsonObject.substring(0, jsonObject.length() - 1) + ",\"" + key + "\":" + value + "}";
    }

    /**
     * A builder for configuring a {@link RequestLoggingMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private boolean includeHeaders;
        private boolean includeQueryString = false;
        private final Set<String> excludedPaths = new LinkedHashSet<>();
        private Duration slowRequestThreshold;
        private LogFormat format = LogFormat.TEXT;
        private TelemetryRecorder recorder = DEFAULT_RECORDER;

        private Builder() {
        }

        /**
         * Sets whether to log request headers.
         *
         * @param includeHeaders whether to include headers
         * @return this {@link Builder}
         */
        public Builder includeHeaders(final boolean includeHeaders) {
            this.includeHeaders = includeHeaders;

            return this;
        }

        /**
         * Sets whether to include the query string in log output.
         *
         * @param includeQueryString whether to include the query string
         * @return this {@link Builder}
         */
        public Builder includeQueryString(final boolean includeQueryString) {
            this.includeQueryString = includeQueryString;

            return this;
        }

        /**
         * Excludes a path from logging.
         *
         * @param path the path to exclude
         * @return this {@link Builder}
         */
        public Builder excludePath(final String path) {
            Objects.requireNonNull(path, "path");
            excludedPaths.add(path);

            return this;
        }

        /**
         * Sets the threshold above which requests are logged at WARN level as slow.
         *
         * @param threshold the slow request threshold
         * @return this {@link Builder}
         */
        public Builder slowRequestThreshold(final Duration threshold) {
            this.slowRequestThreshold = Objects.requireNonNull(threshold, "threshold");

            return this;
        }

        /**
         * Sets the log output format. Defaults to {@link LogFormat#TEXT}.
         *
         * @param format the {@link LogFormat} to use
         * @return this {@link Builder}
         */
        public Builder format(final LogFormat format) {
            this.format = Objects.requireNonNull(format, "format");

            return this;
        }

        /**
         * Sets the {@link TelemetryRecorder} to record access log entries with.
         *
         * @param recorder the {@link TelemetryRecorder}
         * @return this {@link Builder}
         */
        public Builder recorder(final TelemetryRecorder recorder) {
            this.recorder = Objects.requireNonNull(recorder, "recorder");

            return this;
        }

        /**
         * Builds the {@link RequestLoggingMiddleware}.
         *
         * @return a new {@link RequestLoggingMiddleware}
         */
        public RequestLoggingMiddleware build() {
            return new RequestLoggingMiddleware(this);
        }
    }
}
