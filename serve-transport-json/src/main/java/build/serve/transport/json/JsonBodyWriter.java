/*-
 * #%L
 * Serve Transport (JSON)
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
package build.serve.transport.json;

import build.serve.foundation.Response;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Writes objects as JSON response bodies using Jackson {@link ObjectMapper}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonBodyWriter {

    /**
     * The Jackson {@link ObjectMapper}.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@link JsonBodyWriter} with the specified {@link ObjectMapper}.
     *
     * @param objectMapper the {@link ObjectMapper} to use for serialization
     */
    public JsonBodyWriter(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Constructs a {@link JsonBodyWriter} with a default {@link ObjectMapper}.
     */
    public JsonBodyWriter() {
        this(new ObjectMapper());
    }

    /**
     * Writes the specified object as JSON to the response.
     *
     * @param response the {@link Response}
     * @param body     the object to serialize
     */
    public void write(final Response response,
                      final Object body) {
        try {
            final var json = objectMapper.writeValueAsBytes(body);

            response.header("Content-Type", "application/json; charset=utf-8")
                .send(json);
        } catch (final com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
