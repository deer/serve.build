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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A GraphQL request containing the query, optional operation name, and variables.
 *
 * @param query         the GraphQL query string
 * @param operationName the operation name, or {@code null}
 * @param variables     the variables map, or {@code null}
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record GraphQlRequest(
    String query,
    String operationName,
    Map<String, Object> variables
) {

    /**
     * Parses a {@link GraphQlRequest} from a {@link JsonObject}.
     *
     * @param obj the JSON object
     * @return the parsed request
     */
    public static GraphQlRequest fromJson(final JsonObject obj) {
        final var query = obj.getString("query");
        final String operationName = obj.has("operationName") ? obj.getString("operationName") : null;
        final Map<String, Object> variables = obj.has("variables")
            ? jsonObjectToMap(obj.get("variables").asObject())
            : null;
        return new GraphQlRequest(query, operationName, variables);
    }

    private static Map<String, Object> jsonObjectToMap(final JsonObject obj) {
        final var map = new LinkedHashMap<String, Object>();
        for (final var entry : obj.members().entrySet()) {
            map.put(entry.getKey(), jsonValueToObject(entry.getValue()));
        }
        return map;
    }

    private static Object jsonValueToObject(final JsonValue value) {
        return switch (value) {
            case JsonString s -> s.value();
            case JsonNumber n -> n.toNumber();
            case JsonBoolean b -> b.value();
            case JsonNull ignored -> null;
            case JsonObject o -> jsonObjectToMap(o);
            case JsonArray a -> jsonArrayToList(a);
        };
    }

    private static List<Object> jsonArrayToList(final JsonArray arr) {
        final var list = new ArrayList<Object>();
        for (final var item : arr.values()) {
            list.add(jsonValueToObject(item));
        }
        return list;
    }
}
