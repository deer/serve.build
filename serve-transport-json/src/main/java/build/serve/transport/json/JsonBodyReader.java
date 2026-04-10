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

import build.serve.foundation.Request;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Objects;

/**
 * Reads and deserializes JSON request bodies using Jackson {@link ObjectMapper}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonBodyReader {

    /**
     * The Jackson {@link ObjectMapper}.
     */
    private final ObjectMapper objectMapper;

    /**
     * Constructs a {@link JsonBodyReader} with the specified {@link ObjectMapper}.
     *
     * @param objectMapper the {@link ObjectMapper} to use for deserialization
     */
    public JsonBodyReader(final ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    /**
     * Constructs a {@link JsonBodyReader} with a default {@link ObjectMapper}.
     */
    public JsonBodyReader() {
        this(new ObjectMapper());
    }

    /**
     * Reads the request body as the specified type.
     *
     * @param <T>     the target type
     * @param request the {@link Request}
     * @param type    the {@link Class} of the target type
     * @return the deserialized object
     */
    public <T> T read(final Request request,
                      final Class<T> type) {
        try {
            return objectMapper.readValue(request.bodyAsStream(), type);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to deserialize JSON request body to " + type.getName(), e);
        }
    }
}
