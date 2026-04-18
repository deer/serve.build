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
package build.serve.foundation.http;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents an HTTP cookie, with support for all standard Set-Cookie attributes.
 * <p>
 * Use {@link #of(String, String)} to create a simple name/value cookie, or
 * {@link #toBuilder()} to configure Set-Cookie attributes (path, domain, maxAge, etc.).
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record Cookie(
    String name,
    String value,
    String path,
    String domain,
    Long maxAgeSeconds,
    boolean secure,
    boolean httpOnly,
    String sameSite
) {

    /**
     * Compact constructor to validate required fields.
     */
    public Cookie {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(value, "value must not be null");
        requireNoCrlf(name, "name");
        requireNoCrlf(value, "value");

        if (path != null) {
            requireNoCrlf(path, "path");
        }

        if (domain != null) {
            requireNoCrlf(domain, "domain");
        }

        if (sameSite != null) {
            requireNoCrlf(sameSite, "sameSite");
        }
    }

    /**
     * Throws {@link IllegalArgumentException} if the value contains CR or LF characters.
     */
    private static void requireNoCrlf(final String value, final String field) {
        if (value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
            throw new IllegalArgumentException("Cookie " + field + " must not contain CR or LF characters");
        }
    }

    /**
     * Creates a simple {@link Cookie} with only a name and value.
     *
     * @param name  the cookie name
     * @param value the cookie value
     * @return a new {@link Cookie}
     */
    public static Cookie of(final String name,
                            final String value) {
        return new Cookie(name, value, null, null, null, false, false, null);
    }

    /**
     * Returns a {@link Builder} pre-populated with this cookie's name, value, and attributes.
     *
     * @return a new {@link Builder}
     */
    public Builder toBuilder() {
        return new Builder(this);
    }

    /**
     * Renders this cookie as a {@code Set-Cookie} header value string.
     *
     * @return the {@code Set-Cookie} header value
     */
    public String toSetCookieHeader() {
        final var sb = new StringBuilder();

        sb.append(name).append('=').append(value);

        if (path != null) {
            sb.append("; Path=").append(path);
        }

        if (domain != null) {
            sb.append("; Domain=").append(domain);
        }

        if (maxAgeSeconds != null) {
            sb.append("; Max-Age=").append(maxAgeSeconds);
        }

        if (secure) {
            sb.append("; Secure");
        }

        if (httpOnly) {
            sb.append("; HttpOnly");
        }

        if (sameSite != null) {
            sb.append("; SameSite=").append(sameSite);
        }

        return sb.toString();
    }

    /**
     * Builder for configuring {@code Set-Cookie} attributes on a {@link Cookie}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        /**
         * The cookie name.
         */
        private final String name;

        /**
         * The cookie value.
         */
        private final String value;

        /**
         * The Path attribute.
         */
        private String path;

        /**
         * The Domain attribute.
         */
        private String domain;

        /**
         * The Max-Age attribute in seconds.
         */
        private Long maxAgeSeconds;

        /**
         * Whether the Secure flag is set.
         */
        private boolean secure;

        /**
         * Whether the HttpOnly flag is set.
         */
        private boolean httpOnly;

        /**
         * The SameSite attribute value ("Strict", "Lax", or "None").
         */
        private String sameSite;

        /**
         * Constructs a {@link Builder} from an existing {@link Cookie}.
         *
         * @param cookie the source {@link Cookie}
         */
        private Builder(final Cookie cookie) {
            this.name = cookie.name();
            this.value = cookie.value();
            this.path = cookie.path();
            this.domain = cookie.domain();
            this.maxAgeSeconds = cookie.maxAgeSeconds();
            this.secure = cookie.secure();
            this.httpOnly = cookie.httpOnly();
            this.sameSite = cookie.sameSite();
        }

        /**
         * Sets the Path attribute.
         *
         * @param path the cookie path
         * @return this builder
         */
        public Builder path(final String path) {
            this.path = path;

            return this;
        }

        /**
         * Sets the Domain attribute.
         *
         * @param domain the cookie domain
         * @return this builder
         */
        public Builder domain(final String domain) {
            this.domain = domain;

            return this;
        }

        /**
         * Sets the Max-Age attribute from a {@link Duration}.
         *
         * @param maxAge the max age duration
         * @return this builder
         */
        public Builder maxAge(final Duration maxAge) {
            this.maxAgeSeconds = Objects.requireNonNull(maxAge, "maxAge must not be null").toSeconds();

            return this;
        }

        /**
         * Sets the Secure flag.
         *
         * @param secure {@code true} to set the Secure flag
         * @return this builder
         */
        public Builder secure(final boolean secure) {
            this.secure = secure;

            return this;
        }

        /**
         * Sets the HttpOnly flag.
         *
         * @param httpOnly {@code true} to set the HttpOnly flag
         * @return this builder
         */
        public Builder httpOnly(final boolean httpOnly) {
            this.httpOnly = httpOnly;

            return this;
        }

        /**
         * Sets the SameSite attribute.
         *
         * @param sameSite the SameSite value: "Strict", "Lax", or "None"
         * @return this builder
         */
        public Builder sameSite(final String sameSite) {
            this.sameSite = sameSite;

            return this;
        }

        /**
         * Builds and returns the configured {@link Cookie}.
         *
         * @return a new {@link Cookie} with all configured attributes
         */
        public Cookie build() {
            return new Cookie(name, value, path, domain, maxAgeSeconds, secure, httpOnly, sameSite);
        }
    }
}
