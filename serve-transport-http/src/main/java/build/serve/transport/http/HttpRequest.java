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

import build.base.json.JsonValue;
import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.http.Cookie;
import build.serve.foundation.option.MaxRequestSize;
import com.sun.net.httpserver.HttpExchange;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * An implementation of {@link Request} that bridges the JDK {@link HttpExchange}.
 * <p>
 * Path parameters are resolved from the owning {@link Exchange} attribute, where they are set by the router
 * before the handler is invoked.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class HttpRequest
    implements Request {

    /**
     * The underlying JDK {@link HttpExchange}.
     */
    private final HttpExchange httpExchange;

    /**
     * The maximum allowed request body size.
     */
    private final long maxBodyBytes;

    /**
     * The owning {@link Exchange}, used to resolve path parameters from exchange attributes.
     */
    private Exchange exchange;

    /**
     * Constructs an {@link HttpRequest}.
     *
     * @param httpExchange   the JDK {@link HttpExchange}
     * @param maxRequestSize the maximum allowed request body size
     */
    HttpRequest(final HttpExchange httpExchange, final MaxRequestSize maxRequestSize) {
        this.httpExchange = Objects.requireNonNull(httpExchange, "httpExchange must not be null");
        this.maxBodyBytes = Objects.requireNonNull(maxRequestSize, "maxRequestSize must not be null").bytes();
    }

    /**
     * Sets the owning {@link Exchange} for this request, enabling path parameter resolution.
     *
     * @param exchange the owning {@link Exchange}
     */
    void setExchange(final Exchange exchange) {
        this.exchange = Objects.requireNonNull(exchange, "exchange must not be null");
    }

    @Override
    public String method() {
        return httpExchange.getRequestMethod();
    }

    @Override
    public URI uri() {
        return httpExchange.getRequestURI();
    }

    @Override
    public String path() {
        return httpExchange.getRequestURI().getPath();
    }

    @Override
    public Optional<String> pathParam(final String name) {
        if (exchange != null) {
            return exchange.pathParam(name);
        }

        return Optional.empty();
    }

    private static final int MAX_QUERY_PARAMS = 100;

    /**
     * Maximum length (characters, before URL-decoding) of a single query-param or cookie value.
     * Bounds per-value memory/CPU independent of the count caps above, which only bound the
     * number of entries — a single absurdly long value could otherwise still exhaust memory.
     */
    private static final int MAX_VALUE_LENGTH = 8192;

    @Override
    public List<String> queryParams(final String name) {
        final var query = httpExchange.getRequestURI().getQuery();

        if (query == null) {
            return List.of();
        }

        final var result = new java.util.ArrayList<String>();
        int paramCount = 0;

        for (final var param : query.split("&")) {
            if (++paramCount > MAX_QUERY_PARAMS) {
                break;
            }

            if (param.length() > MAX_VALUE_LENGTH) {
                continue;
            }

            final var parts = param.split("=", 2);

            if (parts[0].equals(name)) {
                result.add(parts.length > 1 ? java.net.URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "");
            }
        }

        return List.copyOf(result);
    }

    @Override
    public Optional<String> queryParam(final String name) {
        final var params = queryParams(name);

        return params.isEmpty() ? Optional.empty() : Optional.of(params.getFirst());
    }

    @Override
    public Map<String, List<String>> headers() {
        return httpExchange.getRequestHeaders();
    }

    @Override
    public Optional<String> header(final String name) {
        return Optional.ofNullable(httpExchange.getRequestHeaders().getFirst(name));
    }

    @Override
    public Optional<InetSocketAddress> remoteAddress() {
        return Optional.ofNullable(httpExchange.getRemoteAddress());
    }

    private static final int MAX_COOKIES = 50;

    @Override
    public List<Cookie> cookies() {
        final var cookieHeader = header("Cookie");

        if (cookieHeader.isEmpty()) {
            return List.of();
        }

        final var result = new ArrayList<Cookie>();
        int cookieCount = 0;

        for (final var pair : cookieHeader.get().split(";")) {
            if (++cookieCount > MAX_COOKIES) {
                break;
            }

            if (pair.length() > MAX_VALUE_LENGTH) {
                continue;
            }

            final var trimmed = pair.trim();
            final var eq = trimmed.indexOf('=');

            if (eq > 0) {
                final var name = trimmed.substring(0, eq).trim();
                final var value = trimmed.substring(eq + 1).trim();

                result.add(Cookie.of(name, value));
            }
        }

        return List.copyOf(result);
    }

    @Override
    public InputStream bodyAsStream() {
        return new LimitedInputStream(httpExchange.getRequestBody(), maxBodyBytes);
    }

    @Override
    public String bodyAsString() {
        try (var stream = bodyAsStream()) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final java.io.IOException e) {
            throw new RuntimeException("Failed to read request body", e);
        }
    }

    @Override
    public JsonValue bodyAsJson() {
        if (exchange != null) {
            return exchange.bodyAsJson();
        }

        throw new UnsupportedOperationException(
            "Body parsing requires a transport module (e.g., serve-transport-json)");
    }
}
