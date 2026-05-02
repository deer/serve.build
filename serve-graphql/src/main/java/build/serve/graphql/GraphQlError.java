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
import build.base.json.JsonObject;
import build.base.json.JsonValue;

import java.util.List;

/**
 * Represents a GraphQL error.
 *
 * @param message the error message
 * @param path    the field path where the error occurred, or {@code null}
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record GraphQlError(
    String message,
    List<String> path
) {

    /**
     * Serializes this error to a {@link JsonValue}.
     *
     * @return the JSON representation
     */
    public JsonValue toJson() {
        final var builder = JsonObject.builder()
            .put("message", message);
        if (path != null && !path.isEmpty()) {
            builder.put("path", JsonArray.builder().addAll(path).build());
        }
        return builder.build();
    }
}
