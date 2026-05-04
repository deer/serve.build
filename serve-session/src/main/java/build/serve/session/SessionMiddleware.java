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
import build.serve.foundation.context.RequestContext;
import build.serve.foundation.http.Cookie;
import build.serve.foundation.middleware.Middleware;

import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link Middleware} that associates each request with a server-side {@link Session} via a
 * cookie.
 * <p>
 * On each request the middleware:
 * <ol>
 *   <li>Reads the session cookie and loads the matching session from the {@link SessionStore}.</li>
 *   <li>Creates a new session (and sets a cookie) if none is found.</li>
 *   <li>Binds the session to {@link SessionContext} for the duration of the handler chain.</li>
 *   <li>If a value is stored under {@link #PRINCIPAL_KEY}, binds it to
 *       {@code RequestContext.PRINCIPAL} so that {@code AuthContext.current()} returns it.</li>
 *   <li>After the handler returns, saves the session or deletes it if it was invalidated.</li>
 * </ol>
 * <pre>{@code
 * SessionMiddleware.builder()
 *     .store(InMemorySessionStore.create())
 *     .cookieName("session")
 *     .build()
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class SessionMiddleware implements Middleware {

    /**
     * Session attribute key under which a principal should be stored to enable principal
     * propagation across requests.
     */
    public static final String PRINCIPAL_KEY = "principal";

    private final SessionStore store;
    private final String cookieName;
    private final boolean secure;
    private final Long maxAgeSeconds;

    private SessionMiddleware(final Builder builder) {
        this.store = builder.store;
        this.cookieName = builder.cookieName;
        this.secure = builder.secure;
        this.maxAgeSeconds = builder.maxAgeSeconds;
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link SessionMiddleware}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var session = exchange.request().cookie(cookieName)
                .flatMap(c -> store.load(c.value()))
                .orElseGet(this::newSession);

            runWithSession(session, () -> {
                try {
                    next.handle(exchange);
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });

            if (session.isInvalidated()) {
                store.delete(session.id());
                exchange.response().deleteCookie(cookieName);
            } else {
                store.save(session);
                if (session.isNew()) {
                    exchange.response().cookie(buildCookie(session.id()));
                }
            }
        };
    }

    private void runWithSession(final Session session, final Runnable action) {
        final var withSession = ScopedValue.where(SessionContext.SESSION, session);
        final var principal = session.get(PRINCIPAL_KEY, Object.class);
        if (principal.isPresent()) {
            withSession.where(RequestContext.PRINCIPAL, principal.get()).run(action);
        } else {
            withSession.run(action);
        }
    }

    private Session newSession() {
        return new MapSession(UUID.randomUUID().toString(), true);
    }

    private Cookie buildCookie(final String sessionId) {
        final var builder = Cookie.of(cookieName, sessionId).toBuilder()
            .path("/")
            .httpOnly(true)
            .sameSite("Lax")
            .secure(secure);
        if (maxAgeSeconds != null) {
            builder.maxAge(Duration.ofSeconds(maxAgeSeconds));
        }
        return builder.build();
    }

    /**
     * A builder for configuring a {@link SessionMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Apr-2026
     */
    public static final class Builder {

        private SessionStore store;
        private String cookieName = "session";
        private boolean secure = true;
        private Long maxAgeSeconds = null;

        private Builder() {
        }

        /**
         * Sets the {@link SessionStore} used to persist sessions. Required.
         *
         * @param store the session store
         * @return this {@link Builder}
         */
        public Builder store(final SessionStore store) {
            this.store = Objects.requireNonNull(store, "store");
            return this;
        }

        /**
         * Sets the name of the session cookie. Defaults to {@code "session"}.
         *
         * @param cookieName the cookie name
         * @return this {@link Builder}
         */
        public Builder cookieName(final String cookieName) {
            this.cookieName = Objects.requireNonNull(cookieName, "cookieName");
            return this;
        }

        /**
         * Sets the {@code Secure} flag on the session cookie. Defaults to {@code true}.
         * Set to {@code false} only in plain-HTTP development environments.
         *
         * @param secure {@code true} to require HTTPS for the session cookie
         * @return this {@link Builder}
         */
        public Builder secure(final boolean secure) {
            this.secure = secure;
            return this;
        }

        /**
         * Sets a {@code Max-Age} on the session cookie, turning it into a persistent cookie.
         * By default no {@code Max-Age} is set and the cookie expires when the browser session ends.
         *
         * @param maxAge the maximum age for the session cookie
         * @return this {@link Builder}
         */
        public Builder maxAge(final Duration maxAge) {
            this.maxAgeSeconds = Objects.requireNonNull(maxAge, "maxAge").toSeconds();
            return this;
        }

        /**
         * Builds the {@link SessionMiddleware}.
         *
         * @return a new {@link SessionMiddleware}
         */
        public SessionMiddleware build() {
            Objects.requireNonNull(store, "store");
            return new SessionMiddleware(this);
        }
    }
}
