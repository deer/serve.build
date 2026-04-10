/*-
 * #%L
 * Serve Foundation
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
package build.serve.foundation;

import build.serve.foundation.http.Cookie;

import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents an inbound HTTP request within an {@link Exchange}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface Request {

    /**
     * Obtains the HTTP method of this {@link Request} (e.g., GET, POST, PUT, DELETE).
     *
     * @return the HTTP method
     */
    String method();

    /**
     * Obtains the {@link URI} of this {@link Request}.
     *
     * @return the request {@link URI}
     */
    URI uri();

    /**
     * Obtains the path component of the request {@link URI}.
     *
     * @return the request path
     */
    String path();

    /**
     * Obtains the value of a path parameter extracted by the {@link build.serve.foundation.routing.Router}.
     *
     * @param name the name of the path parameter
     * @return an {@link Optional} containing the parameter value, or empty if not present
     */
    Optional<String> pathParam(String name);

    /**
     * Obtains the values of a query parameter.
     *
     * @param name the name of the query parameter
     * @return the list of values, or an empty list if not present
     */
    List<String> queryParams(String name);

    /**
     * Obtains the first value of a query parameter.
     *
     * @param name the name of the query parameter
     * @return an {@link Optional} containing the first value, or empty if not present
     */
    Optional<String> queryParam(String name);

    /**
     * Obtains all request headers as a {@link Map}.
     *
     * @return the request headers
     */
    Map<String, List<String>> headers();

    /**
     * Obtains the first value of a request header.
     *
     * @param name the header name
     * @return an {@link Optional} containing the header value, or empty if not present
     */
    Optional<String> header(String name);

    /**
     * Obtains all cookies sent with this {@link Request}, parsed from the {@code Cookie} header.
     *
     * @return an immutable list of {@link Cookie} instances, or an empty list if none are present
     */
    default List<Cookie> cookies() {
        return List.of();
    }

    /**
     * Obtains a single cookie by name from this {@link Request}.
     *
     * @param name the cookie name
     * @return an {@link Optional} containing the {@link Cookie}, or empty if not present
     */
    default Optional<Cookie> cookie(final String name) {
        return cookies().stream()
            .filter(c -> c.name().equals(name))
            .findFirst();
    }

    /**
     * Obtains the raw body of this {@link Request} as an {@link InputStream}.
     *
     * @return the request body {@link InputStream}
     */
    InputStream bodyAsStream();

    /**
     * Obtains the raw body of this {@link Request} as a {@link String}.
     *
     * @return the request body as a {@link String}
     */
    String bodyAsString();

    /**
     * Obtains the request body deserialized as the specified type.
     * <p>
     * This method requires a body deserializer to be registered (e.g., via the JSON transport module).
     *
     * @param <T>  the type to deserialize to
     * @param type the {@link Class} of the desired type
     * @return the deserialized body
     */
    <T> T body(Class<T> type);
}
