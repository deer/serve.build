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

import build.base.network.option.Port;
import build.serve.foundation.option.ListenAddress;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.security.SecurityHeadersMiddleware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that {@link ServerApplication.Implementation} auto-registers
 * {@link SecurityHeadersMiddleware} by default, and that subclasses can opt out.
 */
class SecurityHeadersDefaultTests {

    private ServerApplication.Implementation server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void shouldSetSecurityHeadersByDefault() throws Exception {
        server = new ServerApplication.Implementation() {
            @Override
            protected Router configure() {
                return pingRouter();
            }
        };
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));
        server.onStart().toCompletableFuture().get(5, TimeUnit.SECONDS);

        final var headers = fetchHeaders(server.boundAddress().getPort());

        assertThat(headers).containsIgnoringCase("X-Frame-Options: DENY");
        assertThat(headers).containsIgnoringCase("X-Content-Type-Options: nosniff");
        assertThat(headers).containsIgnoringCase("Strict-Transport-Security:");
    }

    @Test
    void shouldAllowOptingOutOfSecurityHeaders() throws Exception {
        server = new ServerApplication.Implementation() {
            @Override
            protected Router configure() {
                return pingRouter();
            }

            @Override
            protected SecurityHeadersMiddleware securityHeaders() {
                return null;
            }
        };
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));
        server.onStart().toCompletableFuture().get(5, TimeUnit.SECONDS);

        final var headers = fetchHeaders(server.boundAddress().getPort());

        assertThat(headers.toLowerCase(Locale.ROOT)).doesNotContain("x-frame-options:");
    }

    private static Router pingRouter() {
        return RouterBuilder.create()
            .get("/ping", exchange -> exchange.response().send("pong"))
            .build();
    }

    private static String fetchHeaders(final int port) throws Exception {
        try (var socket = new Socket("127.0.0.1", port)) {
            socket.getOutputStream().write(
                "GET /ping HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n"
                    .getBytes(StandardCharsets.US_ASCII));
            return new String(socket.getInputStream().readAllBytes(), StandardCharsets.US_ASCII);
        }
    }
}
