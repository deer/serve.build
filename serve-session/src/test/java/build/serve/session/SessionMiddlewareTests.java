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

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.context.RequestContext;
import build.serve.foundation.http.Cookie;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class SessionMiddlewareTests {

    // --- session creation ---

    @Test
    void shouldSetSessionCookieForNewRequest() throws Exception {
        var store = InMemorySessionStore.create();
        var middleware = SessionMiddleware.builder().store(store).build();
        var response = new StubResponse();

        middleware.apply(ex -> {
        }).handle(exchange(List.of(), response));

        assertThat(response.cookies).hasSize(1);
        assertThat(response.cookies.getFirst().name()).isEqualTo("session");
        assertThat(response.cookies.getFirst().httpOnly()).isTrue();
        assertThat(response.cookies.getFirst().sameSite()).isEqualTo("Lax");
    }

    @Test
    void shouldNotSetNewCookieForExistingSession() throws Exception {
        var store = InMemorySessionStore.create();
        var existing = new MapSession("session-id-1", false);
        store.save(existing);
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> {
            }).handle(exchange(List.of(Cookie.of("session", "session-id-1")), response));

        assertThat(response.cookies).isEmpty();
    }

    @Test
    void shouldCreateNewSessionWhenCookieIdIsUnknown() throws Exception {
        var store = InMemorySessionStore.create();
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> {
            }).handle(exchange(List.of(Cookie.of("session", "unknown-id")), response));

        assertThat(response.cookies).hasSize(1);
        assertThat(response.cookies.getFirst().value()).isNotEqualTo("unknown-id");
    }

    // --- SessionContext ---

    @Test
    void shouldExposeSessionViaSessionContext() throws Exception {
        var store = InMemorySessionStore.create();
        var captured = new AtomicReference<Optional<Session>>();

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> captured.set(SessionContext.current()))
            .handle(exchange(List.of(), new StubResponse()));

        assertThat(captured.get()).isPresent();
    }

    @Test
    void shouldReturnEmptySessionContextOutsideMiddleware() {
        assertThat(SessionContext.current()).isEmpty();
    }

    // --- attribute persistence ---

    @Test
    void shouldPersistAttributeAcrossRequests() throws Exception {
        var store = InMemorySessionStore.create();
        var middleware = SessionMiddleware.builder().store(store).build();

        // First request: set attribute
        var response1 = new StubResponse();
        middleware.apply(ex -> SessionContext.current().ifPresent(s -> s.set("userId", "reed")))
            .handle(exchange(List.of(), response1));
        var sessionId = response1.cookies.getFirst().value();

        // Second request: read attribute
        var captured = new AtomicReference<Optional<String>>();
        middleware.apply(ex -> captured.set(
                SessionContext.current().flatMap(s -> s.get("userId", String.class))))
            .handle(exchange(List.of(Cookie.of("session", sessionId)), new StubResponse()));

        assertThat(captured.get()).contains("reed");
    }

    // --- invalidation ---

    @Test
    void shouldDeleteCookieOnInvalidation() throws Exception {
        var store = InMemorySessionStore.create();
        var existing = new MapSession("inv-session-id", false);
        store.save(existing);
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> SessionContext.current().ifPresent(Session::invalidate))
            .handle(exchange(List.of(Cookie.of("session", "inv-session-id")), response));

        assertThat(response.deletedCookies).contains("session");
        assertThat(store.load("inv-session-id")).isEmpty();
    }

    @Test
    void shouldNotSaveInvalidatedSession() throws Exception {
        var store = InMemorySessionStore.create();
        var existing = new MapSession("del-session-id", false);
        store.save(existing);

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> SessionContext.current().ifPresent(Session::invalidate))
            .handle(exchange(List.of(Cookie.of("session", "del-session-id")), new StubResponse()));

        assertThat(store.load("del-session-id")).isEmpty();
    }

    // --- principal propagation ---

    @Test
    void shouldPropagatePrincipalFromSession() throws Exception {
        var store = InMemorySessionStore.create();
        var session = new MapSession("princ-session-id", false);
        session.set(SessionMiddleware.PRINCIPAL_KEY, "reed");
        store.save(session);

        var capturedPrincipal = new AtomicReference<Object>();
        SessionMiddleware.builder().store(store).build()
            .apply(ex -> {
                if (RequestContext.PRINCIPAL.isBound()) {
                    capturedPrincipal.set(RequestContext.PRINCIPAL.get());
                }
            }).handle(exchange(List.of(Cookie.of("session", "princ-session-id")), new StubResponse()));

        assertThat(capturedPrincipal.get()).isEqualTo("reed");
    }

    @Test
    void shouldNotBindPrincipalWhenSessionHasNone() throws Exception {
        var store = InMemorySessionStore.create();
        var capturedBound = new AtomicReference<Boolean>();

        SessionMiddleware.builder().store(store).build()
            .apply(ex -> capturedBound.set(RequestContext.PRINCIPAL.isBound()))
            .handle(exchange(List.of(), new StubResponse()));

        assertThat(capturedBound.get()).isFalse();
    }

    // --- cookie options ---

    @Test
    void shouldRespectCustomCookieName() throws Exception {
        var store = InMemorySessionStore.create();
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).cookieName("MYSESSION").build()
            .apply(ex -> {
            }).handle(exchange(List.of(), response));

        assertThat(response.cookies.getFirst().name()).isEqualTo("MYSESSION");
    }

    @Test
    void shouldSetSecureFlagWhenConfigured() throws Exception {
        var store = InMemorySessionStore.create();
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).secure(true).build()
            .apply(ex -> {
            }).handle(exchange(List.of(), response));

        assertThat(response.cookies.getFirst().secure()).isTrue();
    }

    @Test
    void shouldSetMaxAgeWhenConfigured() throws Exception {
        var store = InMemorySessionStore.create();
        var response = new StubResponse();

        SessionMiddleware.builder().store(store).maxAge(java.time.Duration.ofHours(1)).build()
            .apply(ex -> {
            }).handle(exchange(List.of(), response));

        assertThat(response.cookies.getFirst().maxAgeSeconds()).isEqualTo(3600L);
    }

    // --- helpers ---

    private static Exchange exchange(final List<Cookie> cookies, final StubResponse response) {
        return new SimpleExchange(new StubRequest(cookies), response);
    }

    private static final class StubRequest implements Request {

        private final List<Cookie> cookies;

        StubRequest(final List<Cookie> cookies) {
            this.cookies = cookies;
        }

        @Override
        public String method() {
            return "GET";
        }

        @Override
        public URI uri() {
            return URI.create("/");
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public Optional<String> pathParam(final String name) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(final String name) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(final String name) {
            return Optional.empty();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Map.of();
        }

        @Override
        public Optional<String> header(final String name) {
            return Optional.empty();
        }

        @Override
        public List<Cookie> cookies() {
            return cookies;
        }

        @Override
        public InputStream bodyAsStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public <T> T body(final Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubResponse implements Response {

        final List<Cookie> cookies = new ArrayList<>();
        final List<String> deletedCookies = new ArrayList<>();

        @Override
        public Response status(final int statusCode) {
            return this;
        }

        @Override
        public int status() {
            return 200;
        }

        @Override
        public Response header(final String name, final String value) {
            return this;
        }

        @Override
        public void send(final String body) {
        }

        @Override
        public void send(final byte[] body) {
        }

        @Override
        public void json(final Object body) {
        }

        @Override
        public OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void cookie(final Cookie cookie) {
            if (cookie.maxAgeSeconds() != null && cookie.maxAgeSeconds() == 0L) {
                deletedCookies.add(cookie.name());
            } else {
                cookies.add(cookie);
            }
        }
    }
}
