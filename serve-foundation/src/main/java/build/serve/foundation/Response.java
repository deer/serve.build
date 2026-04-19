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

import java.io.OutputStream;

/**
 * Represents an outbound HTTP response within an {@link Exchange}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface Response {

    /**
     * Sets the HTTP status code for this {@link Response}.
     *
     * @param statusCode the HTTP status code
     * @return this {@link Response} for fluent chaining
     */
    Response status(int statusCode);

    /**
     * Obtains the HTTP status code of this {@link Response}.
     *
     * @return the status code
     */
    int status();

    /**
     * Sets a response header.
     *
     * @param name  the header name
     * @param value the header value
     * @return this {@link Response} for fluent chaining
     */
    Response header(String name, String value);

    /**
     * Sends a plain text body and closes the response.
     *
     * @param body the response body as a {@link String}
     */
    void send(String body);

    /**
     * Sends a byte array body and closes the response.
     *
     * @param body the response body as a byte array
     */
    void send(byte[] body);

    /**
     * Sends an object as JSON and closes the response.
     * <p>
     * This method requires a JSON serializer to be registered (e.g., via the JSON transport module).
     *
     * @param body the object to serialize as JSON
     */
    void json(Object body);

    /**
     * Sends a redirect response to the given location with status 302.
     *
     * @param location the redirect target URL or path
     */
    default void redirect(final String location) {
        redirect(location, 302);
    }

    /**
     * Sends a redirect response to the given location with the specified status code.
     *
     * @param location   the redirect target URL or path
     * @param statusCode the HTTP redirect status code (e.g., 301, 302, 303)
     */
    default void redirect(final String location, final int statusCode) {
        header("Location", location)
            .status(statusCode)
            .send("");
    }

    /**
     * Adds a {@code Set-Cookie} header to this {@link Response} for the given {@link Cookie}.
     *
     * @param cookie the {@link Cookie} to set
     */
    default void cookie(final Cookie cookie) {
        // no-op by default; implementations should override to add Set-Cookie headers
    }

    /**
     * Adds a {@code Set-Cookie} header that expires the named cookie immediately (Max-Age=0).
     *
     * @param name the name of the cookie to delete
     */
    default void deleteCookie(final String name) {
        cookie(Cookie.of(name, "").toBuilder().maxAge(java.time.Duration.ZERO).build());
    }

    /**
     * Obtains the {@link OutputStream} for writing the response body directly.
     * <p>
     * The caller is responsible for flushing and closing the stream.
     *
     * @return the response body {@link OutputStream}
     */
    OutputStream bodyAsStream();
}
