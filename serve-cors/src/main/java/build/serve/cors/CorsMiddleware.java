/*-
 * #%L
 * Serve CORS
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
package build.serve.cors;

import build.base.logging.Logger;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.middleware.Middleware;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Middleware} that handles Cross-Origin Resource Sharing (CORS).
 * <p>
 * Supports preflight (OPTIONS) requests, configurable origins, methods, headers, exposed headers, credentials,
 * and max-age. Use {@link #builder()} for fine-grained configuration or {@link #allowAll()} for permissive
 * development defaults.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class CorsMiddleware implements Middleware {

    private static final Logger LOGGER = Logger.get(CorsMiddleware.class);

    private final Set<String> allowedOrigins;
    private final boolean anyOrigin;
    private final Set<String> allowedMethods;
    private final boolean anyMethod;
    private final Set<String> allowedHeaders;
    private final boolean anyHeader;
    private final Set<String> exposedHeaders;
    private final boolean allowCredentials;
    private final Duration maxAge;

    private CorsMiddleware(final Builder builder) {
        this.allowedOrigins = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedOrigins));
        this.anyOrigin = builder.anyOrigin;
        this.allowedMethods = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedMethods));
        this.anyMethod = builder.anyMethod;
        this.allowedHeaders = Collections.unmodifiableSet(new LinkedHashSet<>(builder.allowedHeaders));
        this.anyHeader = builder.anyHeader;
        this.exposedHeaders = Collections.unmodifiableSet(new LinkedHashSet<>(builder.exposedHeaders));
        this.allowCredentials = builder.allowCredentials;
        this.maxAge = builder.maxAge;

        if (allowCredentials && anyOrigin && allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot allow credentials with wildcard origin; configure specific origins instead");
        }
    }

    /**
     * Creates a permissive {@link CorsMiddleware} that allows all origins, methods, and headers.
     * <p>
     * <strong>Development use only.</strong> Do not use in production — this permits every origin,
     * method, and header with no restrictions.
     *
     * @return a permissive {@link CorsMiddleware}
     * @deprecated Use {@link #builder()} to configure specific allowed origins, methods, and headers.
     */
    @Deprecated
    public static CorsMiddleware allowAll() {
        LOGGER.warn("CorsMiddleware.allowAll() is for development only — permits all origins, methods,"
            + " and headers. Do not use in production.");

        return builder()
            .allowAnyOrigin()
            .allowAnyMethod()
            .allowAnyHeader()
            .build();
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link CorsMiddleware}.
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
            final var origin = request.header("Origin").orElse(null);

            if (origin == null) {
                next.handle(exchange);

                return;
            }

            if (!isOriginAllowed(origin)) {
                next.handle(exchange);

                return;
            }

            setOriginHeader(exchange, origin);
            setCorsHeaders(exchange);

            if ("OPTIONS".equalsIgnoreCase(request.method())) {
                handlePreflight(exchange);

                return;
            }

            next.handle(exchange);
        };
    }

    private boolean isOriginAllowed(final String origin) {
        if (anyOrigin) {
            return true;
        }

        return allowedOrigins.contains(origin);
    }

    private void setOriginHeader(final Exchange exchange, final String origin) {
        final var response = exchange.response();

        if (anyOrigin && !allowCredentials) {
            response.header("Access-Control-Allow-Origin", "*");
        } else {
            response.header("Access-Control-Allow-Origin", origin);
            response.header("Vary", "Origin");
        }
    }

    private void setCorsHeaders(final Exchange exchange) {
        final var response = exchange.response();

        if (allowCredentials) {
            response.header("Access-Control-Allow-Credentials", "true");
        }

        if (!exposedHeaders.isEmpty()) {
            response.header("Access-Control-Expose-Headers", String.join(", ", exposedHeaders));
        }
    }

    private void handlePreflight(final Exchange exchange) {
        final var response = exchange.response();

        if (anyMethod) {
            response.header("Access-Control-Allow-Methods", "*");
        } else if (!allowedMethods.isEmpty()) {
            response.header("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
        }

        if (anyHeader) {
            response.header("Access-Control-Allow-Headers", "*");
        } else if (!allowedHeaders.isEmpty()) {
            response.header("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
        }

        if (maxAge != null) {
            response.header("Access-Control-Max-Age", String.valueOf(maxAge.toSeconds()));
        }

        response.status(204).send("");
    }

    /**
     * A builder for configuring a {@link CorsMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private final Set<String> allowedOrigins = new LinkedHashSet<>();
        private boolean anyOrigin;
        private final Set<String> allowedMethods = new LinkedHashSet<>();
        private boolean anyMethod;
        private final Set<String> allowedHeaders = new LinkedHashSet<>();
        private boolean anyHeader;
        private final Set<String> exposedHeaders = new LinkedHashSet<>();
        private boolean allowCredentials;
        private Duration maxAge;

        private Builder() {
        }

        /**
         * Adds an allowed origin.
         *
         * @param origin the origin to allow
         * @return this {@link Builder}
         */
        public Builder allowOrigin(final String origin) {
            Objects.requireNonNull(origin, "origin");
            allowedOrigins.add(origin);

            return this;
        }

        /**
         * Allows any origin ({@code *}).
         *
         * @return this {@link Builder}
         */
        public Builder allowAnyOrigin() {
            anyOrigin = true;

            return this;
        }

        /**
         * Adds one or more allowed HTTP methods.
         *
         * @param methods the methods to allow
         * @return this {@link Builder}
         */
        public Builder allowMethod(final String... methods) {
            Objects.requireNonNull(methods, "methods");

            for (final var method : methods) {
                allowedMethods.add(method);
            }

            return this;
        }

        /**
         * Allows any HTTP method ({@code *}).
         *
         * @return this {@link Builder}
         */
        public Builder allowAnyMethod() {
            anyMethod = true;

            return this;
        }

        /**
         * Adds one or more allowed request headers.
         *
         * @param headers the headers to allow
         * @return this {@link Builder}
         */
        public Builder allowHeader(final String... headers) {
            Objects.requireNonNull(headers, "headers");

            for (final var header : headers) {
                allowedHeaders.add(header);
            }

            return this;
        }

        /**
         * Allows any request header ({@code *}).
         *
         * @return this {@link Builder}
         */
        public Builder allowAnyHeader() {
            anyHeader = true;

            return this;
        }

        /**
         * Adds a response header to expose to the client.
         *
         * @param header the header to expose
         * @return this {@link Builder}
         */
        public Builder exposeHeader(final String header) {
            Objects.requireNonNull(header, "header");
            exposedHeaders.add(header);

            return this;
        }

        /**
         * Enables or disables credentials support.
         *
         * @param allow whether to allow credentials
         * @return this {@link Builder}
         */
        public Builder allowCredentials(final boolean allow) {
            allowCredentials = allow;

            return this;
        }

        /**
         * Sets the max age for preflight cache.
         *
         * @param maxAge the max age {@link Duration}
         * @return this {@link Builder}
         */
        public Builder maxAge(final Duration maxAge) {
            this.maxAge = Objects.requireNonNull(maxAge, "maxAge");

            return this;
        }

        /**
         * Builds the {@link CorsMiddleware}.
         *
         * @return a new {@link CorsMiddleware}
         */
        public CorsMiddleware build() {
            return new CorsMiddleware(this);
        }
    }
}
