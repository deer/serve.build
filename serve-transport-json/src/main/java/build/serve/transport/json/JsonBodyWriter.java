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

import build.base.json.JsonValue;
import build.serve.foundation.Response;

import java.nio.charset.StandardCharsets;

/**
 * Writes {@link JsonValue} objects as JSON response bodies.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonBodyWriter {

    /**
     * Writes the specified {@link JsonValue} as JSON to the response.
     *
     * @param response the {@link Response}
     * @param body     the {@link JsonValue} to serialize; must be a {@link JsonValue}
     */
    public void write(final Response response,
                      final Object body) {
        if (!(body instanceof JsonValue jv)) {
            throw new IllegalArgumentException("JsonBodyWriter requires a JsonValue, got: " +
                (body == null ? "null" : body.getClass().getName()));
        }
        response.header("Content-Type", "application/json; charset=utf-8")
            .send(jv.toJsonString().getBytes(StandardCharsets.UTF_8));
    }
}
