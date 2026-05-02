/*-
 * #%L
 * Serve GraphQL
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
package build.serve.graphql;

import build.base.json.JsonArray;
import build.base.json.JsonBoolean;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;

import java.util.List;
import java.util.Map;

/**
 * The result of executing a GraphQL request.
 *
 * @param data   the data map, or {@code null} if only errors
 * @param errors the list of errors, empty if none
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record GraphQlResult(
    Map<String, Object> data,
    List<GraphQlError> errors
) {

    /**
     * Returns whether this result contains any errors.
     *
     * @return {@code true} if there are errors
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Creates a successful result with the given data.
     *
     * @param data the data map
     * @return a new {@link GraphQlResult}
     */
    public static GraphQlResult of(final Map<String, Object> data) {
        return new GraphQlResult(data, List.of());
    }

    /**
     * Creates an error result with the given message.
     *
     * @param message the error message
     * @return a new {@link GraphQlResult}
     */
    public static GraphQlResult error(final String message) {
        return new GraphQlResult(null, List.of(new GraphQlError(message, null)));
    }

    /**
     * Serializes this result to a {@link JsonValue}.
     *
     * @return the JSON representation
     */
    public JsonValue toJson() {
        final var builder = JsonObject.builder();
        if (data != null) {
            builder.put("data", objectToJson(data));
        } else {
            builder.putNull("data");
        }
        if (errors != null && !errors.isEmpty()) {
            final var errorsArray = JsonArray.builder();
            for (final var error : errors) {
                errorsArray.add(error.toJson());
            }
            builder.put("errors", errorsArray.build());
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    static JsonValue objectToJson(final Object obj) {
        if (obj == null) {
            return JsonNull.INSTANCE;
        }
        if (obj instanceof String s) {
            return JsonString.of(s);
        }
        if (obj instanceof Boolean b) {
            return JsonBoolean.of(b);
        }
        if (obj instanceof Number n) {
            return JsonNumber.of(n);
        }
        if (obj instanceof Map<?, ?> m) {
            final var b = JsonObject.builder();
            for (final var entry : m.entrySet()) {
                b.put(entry.getKey().toString(), objectToJson(entry.getValue()));
            }
            return b.build();
        }
        if (obj instanceof List<?> l) {
            final var b = JsonArray.builder();
            for (final var item : l) {
                b.add(objectToJson(item));
            }
            return b.build();
        }
        return JsonString.of(obj.toString());
    }
}
