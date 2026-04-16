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

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorsMiddlewareTests {

    @Test
    void preflightReturns204WithCorrectHeaders() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://example.com")
            .allowMethod("GET", "POST")
            .allowHeader("Content-Type")
            .maxAge(Duration.ofHours(1))
            .build();

        var response = new StubResponse();
        var exchange = createExchange("OPTIONS", "https://example.com", response);
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isFalse();
        assertThat(response.statusCode).isEqualTo(204);
        assertThat(response.headers).containsEntry("Access-Control-Allow-Origin", "https://example.com");
        assertThat(response.headers).containsEntry("Access-Control-Allow-Methods", "GET, POST");
        assertThat(response.headers).containsEntry("Access-Control-Allow-Headers", "Content-Type");
        assertThat(response.headers).containsEntry("Access-Control-Max-Age", "3600");
        assertThat(response.headers).containsEntry("Vary", "Origin");
    }

    @Test
    void actualRequestGetsCorsHeaders() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://example.com")
            .exposeHeader("X-Custom-Header")
            .build();

        var response = new StubResponse();
        var exchange = createExchange("GET", "https://example.com", response);
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isTrue();
        assertThat(response.headers).containsEntry("Access-Control-Allow-Origin", "https://example.com");
        assertThat(response.headers).containsEntry("Access-Control-Expose-Headers", "X-Custom-Header");
    }

    @Test
    void disallowedOriginGetsNoHeaders() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://example.com")
            .build();

        var response = new StubResponse();
        var exchange = createExchange("GET", "https://evil.com", response);
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isTrue();
        assertThat(response.headers).doesNotContainKey("Access-Control-Allow-Origin");
    }

    @Test
    void credentialsWithWildcardOriginThrows() {
        assertThatThrownBy(() -> CorsMiddleware.builder()
            .allowAnyOrigin()
            .allowCredentials(true)
            .build())
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void multipleOriginsMatchesDynamically() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://a.com")
            .allowOrigin("https://b.com")
            .build();

        var responseA = new StubResponse();
        middleware.apply(ex -> {
        }).handle(createExchange("GET", "https://a.com", responseA));
        assertThat(responseA.headers).containsEntry("Access-Control-Allow-Origin", "https://a.com");

        var responseB = new StubResponse();
        middleware.apply(ex -> {
        }).handle(createExchange("GET", "https://b.com", responseB));
        assertThat(responseB.headers).containsEntry("Access-Control-Allow-Origin", "https://b.com");

        var responseC = new StubResponse();
        middleware.apply(ex -> {
        }).handle(createExchange("GET", "https://c.com", responseC));
        assertThat(responseC.headers).doesNotContainKey("Access-Control-Allow-Origin");
    }

    @Test
    void allowAllIsPermissive() throws Exception {
        var middleware = CorsMiddleware.allowAll();

        var response = new StubResponse();
        var exchange = createExchange("OPTIONS", "https://anything.com", response);

        middleware.apply(ex -> {
        }).handle(exchange);

        assertThat(response.statusCode).isEqualTo(204);
        assertThat(response.headers).containsEntry("Access-Control-Allow-Origin", "*");
        assertThat(response.headers).containsEntry("Access-Control-Allow-Methods", "*");
        assertThat(response.headers).containsEntry("Access-Control-Allow-Headers", "*");
    }

    @Test
    void credentialsWithSpecificOriginSetsVaryHeader() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://example.com")
            .allowCredentials(true)
            .build();

        var response = new StubResponse();
        middleware.apply(ex -> {
        }).handle(createExchange("GET", "https://example.com", response));

        assertThat(response.headers).containsEntry("Access-Control-Allow-Credentials", "true");
        assertThat(response.headers).containsEntry("Vary", "Origin");
    }

    @Test
    void requestWithoutOriginPassesThrough() throws Exception {
        var middleware = CorsMiddleware.builder()
            .allowOrigin("https://example.com")
            .build();

        var response = new StubResponse();
        var exchange = createExchange("GET", null, response);
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isTrue();
        assertThat(response.headers).doesNotContainKey("Access-Control-Allow-Origin");
    }

    private static Exchange createExchange(final String method, final String origin, final StubResponse response) {
        return new SimpleExchange(new StubRequest(method, origin), response);
    }

    private static final class StubRequest implements Request {

        private final String method;
        private final String origin;

        StubRequest(final String method, final String origin) {
            this.method = method;
            this.origin = origin;
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
            if ("Origin".equalsIgnoreCase(name) && origin != null) {
                return Optional.of(origin);
            }

            return Optional.empty();
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

        final Map<String, String> headers = new LinkedHashMap<>();
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
            headers.put(name, value);

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
    }
}
