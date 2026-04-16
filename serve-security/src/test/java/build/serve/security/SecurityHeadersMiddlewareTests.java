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

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityHeadersMiddlewareTests {

    @Test
    void defaultsSetsAllExpectedHeaders() throws Exception {
        var middleware = SecurityHeadersMiddleware.defaults();
        var response = new StubResponse();
        var exchange = createExchange(response);

        middleware.apply(ex -> {
        }).handle(exchange);

        assertThat(response.headers).containsEntry("X-Frame-Options", "DENY");
        assertThat(response.headers).containsEntry("X-Content-Type-Options", "nosniff");
        assertThat(response.headers).containsEntry("X-XSS-Protection", "0");
        assertThat(response.headers).containsEntry("Strict-Transport-Security", "max-age=63072000");
        assertThat(response.headers).containsEntry("Referrer-Policy", "strict-origin-when-cross-origin");
        assertThat(response.headers).containsEntry("Cross-Origin-Opener-Policy", "same-origin");
        assertThat(response.headers).containsEntry("Cross-Origin-Resource-Policy", "same-origin");
    }

    @Test
    void customValuesOverrideDefaults() throws Exception {
        var middleware = SecurityHeadersMiddleware.builder()
            .xFrameOptions("SAMEORIGIN")
            .strictTransportSecurity("max-age=31536000")
            .build();
        var response = new StubResponse();
        var exchange = createExchange(response);

        middleware.apply(ex -> {
        }).handle(exchange);

        assertThat(response.headers).containsEntry("X-Frame-Options", "SAMEORIGIN");
        assertThat(response.headers).containsEntry("Strict-Transport-Security", "max-age=31536000");
        // Other defaults still present
        assertThat(response.headers).containsEntry("X-Content-Type-Options", "nosniff");
    }

    @Test
    void cspNotSetByDefaultSetWhenConfigured() throws Exception {
        var defaults = SecurityHeadersMiddleware.defaults();
        var defaultResponse = new StubResponse();

        defaults.apply(ex -> {
        }).handle(createExchange(defaultResponse));

        assertThat(defaultResponse.headers).doesNotContainKey("Content-Security-Policy");
        assertThat(defaultResponse.headers).doesNotContainKey("Permissions-Policy");

        var withCsp = SecurityHeadersMiddleware.builder()
            .contentSecurityPolicy("default-src 'self'")
            .permissionsPolicy("camera=(), microphone=()")
            .build();
        var cspResponse = new StubResponse();

        withCsp.apply(ex -> {
        }).handle(createExchange(cspResponse));

        assertThat(cspResponse.headers).containsEntry("Content-Security-Policy", "default-src 'self'");
        assertThat(cspResponse.headers).containsEntry("Permissions-Policy", "camera=(), microphone=()");
    }

    @Test
    void removeHeaderStripsHeaders() throws Exception {
        var middleware = SecurityHeadersMiddleware.builder()
            .removeHeader("X-Powered-By")
            .removeHeader("Server")
            .build();
        var response = new StubResponse();
        // Simulate headers set by the application
        response.headers.put("X-Powered-By", "Jetty");
        response.headers.put("Server", "MyServer");
        var exchange = createExchange(response);

        middleware.apply(ex -> {
        }).handle(exchange);

        assertThat(response.headers).containsEntry("X-Powered-By", "");
        assertThat(response.headers).containsEntry("Server", "");
    }

    @Test
    void builderWithNoCustomizationEqualsDefaults() throws Exception {
        var defaults = SecurityHeadersMiddleware.defaults();
        var builder = SecurityHeadersMiddleware.builder().build();

        var defaultResponse = new StubResponse();
        var builderResponse = new StubResponse();

        defaults.apply(ex -> {
        }).handle(createExchange(defaultResponse));
        builder.apply(ex -> {
        }).handle(createExchange(builderResponse));

        assertThat(builderResponse.headers).isEqualTo(defaultResponse.headers);
    }

    private static Exchange createExchange(final StubResponse response) {
        return new SimpleExchange(new StubRequest(), response);
    }

    private static final class StubRequest implements Request {

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
