/*-
 * #%L
 * Serve Application
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
package build.serve.application;

import build.base.json.Json;
import build.serve.foundation.error.BadRequestException;
import build.serve.foundation.error.ErrorHandler;
import build.serve.foundation.error.NotFoundException;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import build.serve.transport.json.JsonErrorHandler;
import build.serve.transport.json.JsonMiddleware;
import org.junit.jupiter.api.Test;

class ErrorHandlingTests {

    @Test
    void shouldReturn404ForNonexistentRoute() {
        try (var server = createServer(null)) {
            server.get("/nonexistent")
                .send()
                .assertStatus(404);
        }
    }

    @Test
    void shouldReturn404WhenHandlerThrowsNotFoundException() {
        try (var server = createServer(null)) {
            server.get("/throw-not-found")
                .send()
                .assertStatus(404)
                .assertBody("User 42 not found");
        }
    }

    @Test
    void shouldReturn400WhenHandlerThrowsBadRequestException() {
        try (var server = createServer(null)) {
            server.get("/throw-bad-request")
                .send()
                .assertStatus(400)
                .assertBody("missing field");
        }
    }

    @Test
    void shouldReturn500WhenHandlerThrowsRuntimeException() {
        try (var server = createServer(null)) {
            var response = server.get("/throw-runtime").send();

            response.assertStatus(500)
                .assertBody("Internal Server Error");
        }
    }

    @Test
    void shouldReturnJsonErrorResponseFromJsonErrorHandler() throws Exception {
        try (var server = createServer(new JsonErrorHandler())) {
            var response = server.get("/throw-not-found").send();

            response.assertStatus(404)
                .assertContentType("application/json");

            final var json = Json.parse(response.body()).asObject();
            assert json.get("status").asNumber().toNumber().intValue() == 404;
            assert json.getString("message").equals("User 42 not found");
        }
    }

    private TestServer createServer(final ErrorHandler errorHandler) {
        final var builder = RouterBuilder.create();

        if (errorHandler instanceof JsonErrorHandler) {
            builder.middleware(new JsonMiddleware());
        }

        final var router = builder
            .get("/ok", exchange -> exchange.response().send("OK"))
            .get("/throw-not-found", exchange -> {
                throw new NotFoundException("User 42 not found");
            })
            .get("/throw-bad-request", exchange -> {
                throw new BadRequestException("missing field");
            })
            .get("/throw-runtime", exchange -> {
                throw new RuntimeException("secret internal details");
            })
            .build();

        if (errorHandler != null) {
            return TestServer.of(router, errorHandler);
        }
        return TestServer.of(router);
    }
}
