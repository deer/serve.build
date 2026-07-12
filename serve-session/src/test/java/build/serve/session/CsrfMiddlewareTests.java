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

import build.base.json.JsonValue;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.http.Cookie;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsrfMiddlewareTests {

    @Test
    void shouldAllowSafeMethodWithoutToken() throws Exception {
        var store = InMemorySessionStore.create();
        var nextCalled = new AtomicBoolean(false);
        var response = new StubResponse();

        chain(store, ex -> nextCalled.set(true))
            .handle(exchange("GET", Map.of(), List.of(), response));

        assertThat(nextCalled.get()).isTrue();
    }

    @Test
    void shouldExposeTokenViaCurrentTokenOnSafeMethod() throws Exception {
        var store = InMemorySessionStore.create();
        var captured = new AtomicReference<Optional<String>>();

        chain(store, ex -> captured.set(CsrfMiddleware.currentToken()))
            .handle(exchange("GET", Map.of(), List.of(), new StubResponse()));

        assertThat(captured.get()).isPresent();
    }

    @Test
    void shouldRejectStateChangingRequestWithoutToken() throws Exception {
        var store = InMemorySessionStore.create();
        var nextCalled = new AtomicBoolean(false);
        var response = new StubResponse();

        chain(store, ex -> nextCalled.set(true))
            .handle(exchange("POST", Map.of(), List.of(), response));

        assertThat(nextCalled.get()).isFalse();
        assertThat(response.statusCode).isEqualTo(403);
    }

    @Test
    void shouldRejectStateChangingRequestWithWrongToken() throws Exception {
        var store = InMemorySessionStore.create();
        var session = new MapSession("csrf-session", false);
        session.set(CsrfMiddleware.TOKEN_KEY, "the-real-token");
        store.save(session);
        var nextCalled = new AtomicBoolean(false);
        var response = new StubResponse();

        chain(store, ex -> nextCalled.set(true))
            .handle(exchange("POST", Map.of("X-CSRF-Token", "wrong-token"),
                List.of(Cookie.of("session", "csrf-session")), response));

        assertThat(nextCalled.get()).isFalse();
        assertThat(response.statusCode).isEqualTo(403);
    }

    @Test
    void shouldAllowStateChangingRequestWithMatchingToken() throws Exception {
        var store = InMemorySessionStore.create();
        var session = new MapSession("csrf-session-2", false);
        session.set(CsrfMiddleware.TOKEN_KEY, "the-real-token");
        store.save(session);
        var nextCalled = new AtomicBoolean(false);
        var response = new StubResponse();

        chain(store, ex -> nextCalled.set(true))
            .handle(exchange("POST", Map.of("X-CSRF-Token", "the-real-token"),
                List.of(Cookie.of("session", "csrf-session-2")), response));

        assertThat(nextCalled.get()).isTrue();
    }

    @Test
    void shouldUseCustomHeaderName() throws Exception {
        var store = InMemorySessionStore.create();
        var session = new MapSession("csrf-session-3", false);
        session.set(CsrfMiddleware.TOKEN_KEY, "the-real-token");
        store.save(session);
        var nextCalled = new AtomicBoolean(false);

        var sessionMw = SessionMiddleware.builder().store(store).build();
        var csrfMw = CsrfMiddleware.builder().headerName("X-Custom-Csrf").build();

        sessionMw.apply(csrfMw.apply(ex -> nextCalled.set(true)))
            .handle(exchange("POST", Map.of("X-Custom-Csrf", "the-real-token"),
                List.of(Cookie.of("session", "csrf-session-3")), new StubResponse()));

        assertThat(nextCalled.get()).isTrue();
    }

    @Test
    void shouldThrowWhenSessionMiddlewareNotRegistered() {
        var csrfMw = CsrfMiddleware.builder().build();

        assertThatThrownBy(() ->
            csrfMw.apply(ex -> {
            }).handle(exchange("GET", Map.of(), List.of(), new StubResponse())))
            .isInstanceOf(IllegalStateException.class);
    }

    // --- helpers ---

    private static Handler chain(final InMemorySessionStore store, final Handler next) {
        var sessionMw = SessionMiddleware.builder().store(store).build();
        var csrfMw = CsrfMiddleware.builder().build();
        return sessionMw.apply(csrfMw.apply(next));
    }

    private static Exchange exchange(final String method, final Map<String, String> headers,
                                     final List<Cookie> cookies, final StubResponse response) {
        return new SimpleExchange(new StubRequest(method, headers, cookies), response);
    }

    private static final class StubRequest implements Request {

        private final String method;
        private final Map<String, String> headers;
        private final List<Cookie> cookies;

        StubRequest(final String method, final Map<String, String> headers, final List<Cookie> cookies) {
            this.method = method;
            this.headers = headers;
            this.cookies = cookies;
        }

        @Override
        public String method() {
            return method;
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
            return Optional.ofNullable(headers.get(name));
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
        public JsonValue bodyAsJson() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubResponse implements Response {

        final List<Cookie> cookies = new ArrayList<>();
        int statusCode = 200;

        @Override
        public Response status(final int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public int status() {
            return statusCode;
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
            cookies.add(cookie);
        }
    }
}
