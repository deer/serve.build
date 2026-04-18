/*-
 * #%L
 * Serve Health
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
package build.serve.health;

import build.serve.foundation.Handler;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.foundation.util.JsonStrings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides health check handlers for liveness and readiness probes.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class HealthHandler {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    /**
     * Prevents instantiation.
     */
    private HealthHandler() {
    }

    /**
     * Creates a liveness handler that always returns 200 OK with {@code {"status":"UP"}}.
     *
     * @return a liveness {@link Handler}
     */
    public static Handler liveness() {
        return exchange -> {
            exchange.response()
                .status(200)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .send("{\"status\":\"UP\"}");
        };
    }

    /**
     * Creates a readiness handler that executes the specified health checks.
     * <p>
     * Returns 200 OK if all checks pass, or 503 Service Unavailable if any check fails.
     *
     * @param checks the health checks to execute
     * @return a readiness {@link Handler}
     */
    public static Handler readiness(final HealthCheck... checks) {
        final var checkList = List.of(checks);

        return exchange -> {
            final Map<String, Boolean> results = new LinkedHashMap<>();
            var allUp = true;

            for (final var check : checkList) {
                boolean healthy;

                try {
                    healthy = check.check();
                } catch (final Exception e) {
                    healthy = false;
                }

                results.put(check.name(), healthy);

                if (!healthy) {
                    allUp = false;
                }
            }

            final var status = allUp ? "UP" : "DOWN";
            final var statusCode = allUp ? 200 : 503;
            final var json = buildReadinessJson(status, results);

            exchange.response()
                .status(statusCode)
                .header(CONTENT_TYPE, APPLICATION_JSON)
                .send(json);
        };
    }

    /**
     * Creates a new {@link Builder} for constructing a combined health check {@link Router}.
     *
     * @return a new {@link Builder}
     */
    public static Builder create() {
        return new Builder();
    }

    /**
     * Builds the readiness JSON response.
     *
     * @param status  the overall status
     * @param results the individual check results
     * @return the JSON string
     */
    private static String buildReadinessJson(final String status,
                                             final Map<String, Boolean> results) {
        final var sb = new StringBuilder();

        sb.append("{\"status\":\"").append(status).append("\"");

        if (!results.isEmpty()) {
            sb.append(",\"checks\":{");

            var first = true;

            for (final var entry : results.entrySet()) {
                if (!first) {
                    sb.append(',');
                }

                sb.append('"').append(JsonStrings.escape(entry.getKey())).append("\":\"");
                sb.append(entry.getValue() ? "UP" : "DOWN");
                sb.append('"');
                first = false;
            }

            sb.append('}');
        }

        sb.append('}');

        return sb.toString();
    }


    /**
     * A builder for constructing a combined health check {@link Router} with liveness and readiness endpoints.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        /**
         * The router builder.
         */
        private final RouterBuilder routerBuilder;

        /**
         * Constructs a {@link Builder}.
         */
        private Builder() {
            this.routerBuilder = RouterBuilder.create();
        }

        /**
         * Registers a liveness endpoint at the specified path.
         *
         * @param path the liveness endpoint path
         * @return this {@link Builder} for fluent chaining
         */
        public Builder liveness(final String path) {
            routerBuilder.get(path, HealthHandler.liveness());

            return this;
        }

        /**
         * Registers a readiness endpoint at the specified path with the specified health checks.
         *
         * @param path   the readiness endpoint path
         * @param checks the health checks to execute
         * @return this {@link Builder} for fluent chaining
         */
        public Builder readiness(final String path,
                                 final HealthCheck... checks) {
            routerBuilder.get(path, HealthHandler.readiness(checks));

            return this;
        }

        /**
         * Builds the {@link Router}.
         *
         * @return the built {@link Router}
         */
        public Router build() {
            return routerBuilder.build();
        }
    }
}
