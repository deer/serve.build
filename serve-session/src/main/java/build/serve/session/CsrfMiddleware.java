/*-
 * #%L
 * Serve Session
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
package build.serve.session;

import build.serve.foundation.Handler;
import build.serve.foundation.middleware.Middleware;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A {@link Middleware} implementing the synchronizer-token pattern for CSRF protection.
 * <p>
 * A random token is stored on the session (under {@link #TOKEN_KEY}), generated on first access.
 * Requests using a "safe" method ({@code GET}, {@code HEAD}, {@code OPTIONS}, {@code TRACE}) pass
 * through unconditionally. Requests using any other method must echo the token back in a request
 * header (by default {@value #DEFAULT_HEADER_NAME}); a missing or mismatched token is rejected
 * with {@code 403}.
 * <p>
 * Use {@link #currentToken()} in a handler or template to embed the token in a form (as a hidden
 * field submitted back as the header) or to expose it to client-side JS.
 * <p>
 * <strong>Ordering:</strong> this middleware requires {@link SessionContext} to be bound, so
 * {@link SessionMiddleware} must run first. Per the framework's middleware convention
 * (first-registered = outermost), register {@link SessionMiddleware} before
 * {@link CsrfMiddleware}:
 * <pre>{@code
 * RouterBuilder.create()
 *     .middleware(SessionMiddleware.builder().store(store).build())
 *     .middleware(CsrfMiddleware.builder().build())
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public final class CsrfMiddleware implements Middleware {

    /**
     * Session attribute key under which the CSRF token is stored.
     */
    public static final String TOKEN_KEY = "csrfToken";

    /**
     * The default request header expected to carry the CSRF token on state-changing requests.
     */
    public static final String DEFAULT_HEADER_NAME = "X-CSRF-Token";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS", "TRACE");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final String headerName;

    private CsrfMiddleware(final Builder builder) {
        this.headerName = builder.headerName;
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link CsrfMiddleware}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var session = SessionContext.current()
                .orElseThrow(() -> new IllegalStateException(
                    "CsrfMiddleware requires SessionMiddleware to run first — no session is bound"));

            // Ensure a token exists so it's available to render into the response even on safe methods.
            final var token = tokenFor(session);

            if (!SAFE_METHODS.contains(exchange.request().method().toUpperCase())) {
                final var supplied = exchange.request().header(headerName).orElse(null);
                if (supplied == null || !constantTimeEquals(supplied, token)) {
                    exchange.response().status(403).send("CSRF token missing or invalid");
                    return;
                }
            }

            next.handle(exchange);
        };
    }

    /**
     * Returns the CSRF token for the current request's session, generating and storing one if
     * absent. Requires {@link SessionMiddleware} to be active.
     *
     * @return the CSRF token, or empty if no session is bound
     */
    public static Optional<String> currentToken() {
        return SessionContext.current().map(CsrfMiddleware::tokenFor);
    }

    private static String tokenFor(final Session session) {
        return session.get(TOKEN_KEY, String.class).orElseGet(() -> {
            final var generated = generateToken();
            session.set(TOKEN_KEY, generated);
            return generated;
        });
    }

    private static String generateToken() {
        final var bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean constantTimeEquals(final String a, final String b) {
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A builder for configuring a {@link CsrfMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Jul-2026
     */
    public static final class Builder {

        private String headerName = DEFAULT_HEADER_NAME;

        private Builder() {
        }

        /**
         * Sets the request header expected to carry the CSRF token on state-changing requests.
         * Defaults to {@value #DEFAULT_HEADER_NAME}.
         *
         * @param headerName the header name
         * @return this {@link Builder}
         */
        public Builder headerName(final String headerName) {
            this.headerName = Objects.requireNonNull(headerName, "headerName");
            return this;
        }

        /**
         * Builds the {@link CsrfMiddleware}.
         *
         * @return a new {@link CsrfMiddleware}
         */
        public CsrfMiddleware build() {
            return new CsrfMiddleware(this);
        }
    }
}
