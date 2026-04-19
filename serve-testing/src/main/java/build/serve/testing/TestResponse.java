/*-
 * #%L
 * Serve Testing
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
package build.serve.testing;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A fluent HTTP response wrapper with assertion methods for integration tests.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TestResponse {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int status;
    private final byte[] body;
    private final Map<String, String> headers;
    private final String contentType;

    TestResponse(final int status,
                 final byte[] body,
                 final Map<String, String> headers,
                 final String contentType) {
        this.status = status;
        this.body = body;
        this.headers = headers;
        this.contentType = contentType;
    }

    /**
     * Returns the HTTP status code.
     *
     * @return the status code
     */
    public int status() {
        return status;
    }

    /**
     * Returns the response body as a string.
     *
     * @return the body
     */
    public String body() {
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * Returns the response body as bytes.
     *
     * @return the body bytes
     */
    public byte[] bodyBytes() {
        return body.clone();
    }

    /**
     * Deserializes the response body as the given type using Jackson.
     *
     * @param type the target type
     * @param <T>  the target type
     * @return the deserialized object
     */
    public <T> T bodyAs(final Class<T> type) {
        try {
            return MAPPER.readValue(body, type);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns a response header value.
     *
     * @param name the header name
     * @return the header value, or {@code null}
     */
    public String header(final String name) {
        return headers.entrySet().stream()
            .filter(e -> e.getKey().equalsIgnoreCase(name))
            .map(Map.Entry::getValue)
            .findFirst()
            .orElse(null);
    }

    /**
     * Returns the Content-Type header.
     *
     * @return the content type
     */
    public String contentType() {
        return contentType;
    }

    /**
     * Asserts that the status code equals the expected value.
     *
     * @param expected the expected status
     * @return this response for chaining
     */
    public TestResponse assertStatus(final int expected) {
        assertThat(status).isEqualTo(expected);
        return this;
    }

    /**
     * Asserts that the body equals the expected string.
     *
     * @param expected the expected body
     * @return this response for chaining
     */
    public TestResponse assertBody(final String expected) {
        assertThat(body()).isEqualTo(expected);
        return this;
    }

    /**
     * Asserts that the body contains the given substring.
     *
     * @param substring the expected substring
     * @return this response for chaining
     */
    public TestResponse assertBodyContains(final String substring) {
        assertThat(body()).contains(substring);
        return this;
    }

    /**
     * Asserts that a header has the expected value.
     *
     * @param name     the header name
     * @param expected the expected value
     * @return this response for chaining
     */
    public TestResponse assertHeader(final String name, final String expected) {
        assertThat(header(name)).isEqualTo(expected);
        return this;
    }

    /**
     * Asserts that the Content-Type contains the expected string.
     *
     * @param expected the expected content type
     * @return this response for chaining
     */
    public TestResponse assertContentType(final String expected) {
        assertThat(contentType).contains(expected);
        return this;
    }
}
