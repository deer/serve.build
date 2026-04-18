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

class HealthHandlerTests {

    @Test
    void livenessReturns200WithUpStatus() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.liveness().handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo("{\"status\":\"UP\"}");
        assertThat(response.headers).containsEntry("Content-Type", "application/json");
    }

    @Test
    void readinessWithAllPassingChecksReturns200() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.readiness(
            HealthCheck.of("database", () -> true),
            HealthCheck.of("cache", () -> true)
        ).handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo(
            "{\"status\":\"UP\",\"checks\":{\"database\":\"UP\",\"cache\":\"UP\"}}");
    }

    @Test
    void readinessWithFailingCheckReturns503() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.readiness(
            HealthCheck.of("database", () -> true),
            HealthCheck.of("cache", () -> false)
        ).handle(exchange);

        assertThat(response.statusCode).isEqualTo(503);
        assertThat(response.body).isEqualTo(
            "{\"status\":\"DOWN\",\"checks\":{\"database\":\"UP\",\"cache\":\"DOWN\"}}");
    }

    @Test
    void readinessWithExceptionTreatsAsFailing() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.readiness(
            HealthCheck.of("database", () -> {
                throw new RuntimeException("connection refused");
            })
        ).handle(exchange);

        assertThat(response.statusCode).isEqualTo(503);
        assertThat(response.body).contains("\"database\":\"DOWN\"");
    }

    @Test
    void readinessEscapesSpecialCharactersInCheckNames() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.readiness(
            HealthCheck.of("db\"shard\\1", () -> true)
        ).handle(exchange);

        assertThat(response.body).isEqualTo(
            "{\"status\":\"UP\",\"checks\":{\"db\\\"shard\\\\1\":\"UP\"}}");
    }

    @Test
    void readinessWithNoChecksReturns200() throws Exception {
        var response = new StubResponse();
        var exchange = createExchange("GET", response);

        HealthHandler.readiness().handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.body).isEqualTo("{\"status\":\"UP\"}");
    }

    // -- Test helpers --

    private static Exchange createExchange(final String method,
                                           final Response response) {
        return new SimpleExchange(new StubRequest(method), response);
    }

    private static final class StubRequest implements Request {

        private final String method;

        StubRequest(final String method) {
            this.method = method;
        }

        @Override
        public String method() {
            return method;
        }

        @Override
        public URI uri() {
            return URI.create("/health");
        }

        @Override
        public String path() {
            return "/health";
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
            throw new UnsupportedOperationException();
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
        String body;

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
            this.body = body;
        }

        @Override
        public void send(final byte[] body) {
            this.body = new String(body);
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
