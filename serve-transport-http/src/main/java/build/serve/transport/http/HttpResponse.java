/*-
 * #%L
 * Serve Transport (HTTP)
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
package build.serve.transport.http;

import build.serve.foundation.Exchange;
import build.serve.foundation.Response;
import build.serve.foundation.http.Cookie;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * An implementation of {@link Response} that bridges the JDK {@link HttpExchange}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class HttpResponse
    implements Response {

    /**
     * The underlying JDK {@link HttpExchange}.
     */
    private final HttpExchange httpExchange;

    /**
     * The HTTP status code.
     */
    private int statusCode;

    /**
     * The owning {@link Exchange}, used to resolve the body writer for JSON serialization.
     */
    private Exchange exchange;

    /**
     * Constructs an {@link HttpResponse}.
     *
     * @param httpExchange the JDK {@link HttpExchange}
     */
    public HttpResponse(final HttpExchange httpExchange) {
        this.httpExchange = Objects.requireNonNull(httpExchange, "httpExchange must not be null");
        this.statusCode = 200;
    }

    /**
     * Sets the owning {@link Exchange} for this response, enabling JSON serialization via body writer.
     *
     * @param exchange the owning {@link Exchange}
     */
    void setExchange(final Exchange exchange) {
        this.exchange = Objects.requireNonNull(exchange, "exchange must not be null");
    }

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
    public Response header(final String name,
                           final String value) {
        httpExchange.getResponseHeaders().set(name, value);

        return this;
    }

    @Override
    public void removeHeader(final String name) {
        httpExchange.getResponseHeaders().remove(name);
    }

    @Override
    public void cookie(final Cookie cookie) {
        httpExchange.getResponseHeaders().add("Set-Cookie", cookie.toSetCookieHeader());
    }

    @Override
    public void send(final String body) {
        final var bytes = body.getBytes(StandardCharsets.UTF_8);

        send(bytes);
    }

    @Override
    public void send(final byte[] body) {
        try {
            if (!httpExchange.getResponseHeaders().containsKey("Content-Type")) {
                httpExchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            }

            httpExchange.sendResponseHeaders(statusCode, body.length);

            try (var os = httpExchange.getResponseBody()) {
                os.write(body);
            }
        } catch (final IOException e) {
            throw new RuntimeException("Failed to send response", e);
        }
    }

    @Override
    public void json(final Object body) {
        if (exchange != null) {
            exchange.sendBody(body);

            return;
        }

        throw new UnsupportedOperationException(
            "JSON serialization requires a transport module (e.g., serve-transport-json)");
    }

    @Override
    public OutputStream bodyAsStream() {
        try {
            httpExchange.sendResponseHeaders(statusCode, 0);

            return httpExchange.getResponseBody();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to open response stream", e);
        }
    }
}
