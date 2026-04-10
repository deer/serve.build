/*-
 * #%L
 * Serve Security
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
package build.serve.security;

import build.serve.foundation.Handler;
import build.serve.foundation.Response;
import build.serve.foundation.middleware.Middleware;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * A {@link Middleware} that sets standard security headers on every response.
 * <p>
 * Use {@link #defaults()} for sensible defaults or {@link #builder()} for fine-grained configuration.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class SecurityHeadersMiddleware implements Middleware {

    private final String xFrameOptions;
    private final String xContentTypeOptions;
    private final String xXssProtection;
    private final String strictTransportSecurity;
    private final String contentSecurityPolicy;
    private final String referrerPolicy;
    private final String permissionsPolicy;
    private final String crossOriginOpenerPolicy;
    private final String crossOriginResourcePolicy;
    private final Set<String> removeHeaders;

    private SecurityHeadersMiddleware(final Builder builder) {
        this.xFrameOptions = builder.xFrameOptions;
        this.xContentTypeOptions = builder.xContentTypeOptions;
        this.xXssProtection = builder.xXssProtection;
        this.strictTransportSecurity = builder.strictTransportSecurity;
        this.contentSecurityPolicy = builder.contentSecurityPolicy;
        this.referrerPolicy = builder.referrerPolicy;
        this.permissionsPolicy = builder.permissionsPolicy;
        this.crossOriginOpenerPolicy = builder.crossOriginOpenerPolicy;
        this.crossOriginResourcePolicy = builder.crossOriginResourcePolicy;
        this.removeHeaders = Set.copyOf(builder.removeHeaders);
    }

    /**
     * Creates a {@link SecurityHeadersMiddleware} with sensible defaults.
     * <p>
     * Sets X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Strict-Transport-Security,
     * Referrer-Policy, Cross-Origin-Opener-Policy, and Cross-Origin-Resource-Policy.
     * Does not set Content-Security-Policy or Permissions-Policy (too application-specific).
     *
     * @return a {@link SecurityHeadersMiddleware} with default headers
     */
    public static SecurityHeadersMiddleware defaults() {
        return builder().build();
    }

    /**
     * Creates a new {@link Builder} for fine-grained configuration.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            next.handle(exchange);

            final var response = exchange.response();

            setIfNonNull(response, "X-Frame-Options", xFrameOptions);
            setIfNonNull(response, "X-Content-Type-Options", xContentTypeOptions);
            setIfNonNull(response, "X-XSS-Protection", xXssProtection);
            setIfNonNull(response, "Strict-Transport-Security", strictTransportSecurity);
            setIfNonNull(response, "Content-Security-Policy", contentSecurityPolicy);
            setIfNonNull(response, "Referrer-Policy", referrerPolicy);
            setIfNonNull(response, "Permissions-Policy", permissionsPolicy);
            setIfNonNull(response, "Cross-Origin-Opener-Policy", crossOriginOpenerPolicy);
            setIfNonNull(response, "Cross-Origin-Resource-Policy", crossOriginResourcePolicy);

            for (final var header : removeHeaders) {
                response.header(header, "");
            }
        };
    }

    private static void setIfNonNull(final Response response, final String name, final String value) {
        if (value != null) {
            response.header(name, value);
        }
    }

    /**
     * Builder for {@link SecurityHeadersMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private String xFrameOptions = "DENY";
        private String xContentTypeOptions = "nosniff";
        private String xXssProtection = "0";
        private String strictTransportSecurity = "max-age=63072000";
        private String contentSecurityPolicy;
        private String referrerPolicy = "strict-origin-when-cross-origin";
        private String permissionsPolicy;
        private String crossOriginOpenerPolicy = "same-origin";
        private String crossOriginResourcePolicy = "same-origin";
        private final Set<String> removeHeaders = new LinkedHashSet<>();

        private Builder() {
        }

        /**
         * Sets the X-Frame-Options header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder xFrameOptions(final String value) {
            this.xFrameOptions = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the X-Content-Type-Options header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder xContentTypeOptions(final String value) {
            this.xContentTypeOptions = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the X-XSS-Protection header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder xXssProtection(final String value) {
            this.xXssProtection = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Strict-Transport-Security header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder strictTransportSecurity(final String value) {
            this.strictTransportSecurity = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Content-Security-Policy header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder contentSecurityPolicy(final String value) {
            this.contentSecurityPolicy = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Referrer-Policy header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder referrerPolicy(final String value) {
            this.referrerPolicy = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Permissions-Policy header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder permissionsPolicy(final String value) {
            this.permissionsPolicy = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Cross-Origin-Opener-Policy header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder crossOriginOpenerPolicy(final String value) {
            this.crossOriginOpenerPolicy = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Sets the Cross-Origin-Resource-Policy header value.
         *
         * @param value the header value
         * @return this {@link Builder}
         */
        public Builder crossOriginResourcePolicy(final String value) {
            this.crossOriginResourcePolicy = Objects.requireNonNull(value, "value must not be null");
            return this;
        }

        /**
         * Adds a header name to be removed (stripped) from the response.
         *
         * @param headerName the header name to remove
         * @return this {@link Builder}
         */
        public Builder removeHeader(final String headerName) {
            this.removeHeaders.add(Objects.requireNonNull(headerName, "headerName must not be null"));
            return this;
        }

        /**
         * Builds the {@link SecurityHeadersMiddleware}.
         *
         * @return a new {@link SecurityHeadersMiddleware}
         */
        public SecurityHeadersMiddleware build() {
            return new SecurityHeadersMiddleware(this);
        }
    }
}
