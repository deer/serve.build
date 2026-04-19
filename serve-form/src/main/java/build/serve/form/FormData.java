/*-
 * #%L
 * Serve Form
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
package build.serve.form;

import build.serve.foundation.Request;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Parsed {@code application/x-www-form-urlencoded} form data.
 * <p>
 * Obtain an instance from an inbound {@link Request}:
 * <pre>{@code
 * var form = FormData.from(exchange.request());
 * var name = form.get("name").orElse("");
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class FormData {

    private final Map<String, List<String>> fields;

    private FormData(final Map<String, List<String>> fields) {
        this.fields = fields;
    }

    /**
     * Parses {@code application/x-www-form-urlencoded} form data from the given {@link Request}.
     *
     * @param request the inbound request
     * @return the parsed {@link FormData}
     */
    public static FormData from(final Request request) {
        return parse(request.bodyAsString());
    }

    /**
     * Parses {@code application/x-www-form-urlencoded} form data from a raw string.
     *
     * @param body the raw form body (e.g., {@code "name=Alice&age=30"})
     * @return the parsed {@link FormData}
     */
    public static FormData parse(final String body) {
        final Map<String, List<String>> fields = new LinkedHashMap<>();

        if (body == null || body.isBlank()) {
            return new FormData(fields);
        }

        for (final var pair : body.split("&")) {
            if (pair.isBlank()) {
                continue;
            }
            final var eq = pair.indexOf('=');
            final String key;
            final String value;

            if (eq < 0) {
                key = decode(pair);
                value = "";
            } else {
                key = decode(pair.substring(0, eq));
                value = decode(pair.substring(eq + 1));
            }

            fields.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return new FormData(Collections.unmodifiableMap(fields));
    }

    /**
     * Returns the first value for the given field name, or empty if absent.
     *
     * @param name the field name
     * @return the first value, or empty
     */
    public Optional<String> get(final String name) {
        final var values = fields.get(name);
        return values != null && !values.isEmpty() ? Optional.of(values.get(0)) : Optional.empty();
    }

    /**
     * Returns all values for the given field name.
     *
     * @param name the field name
     * @return all values, or an empty list if absent
     */
    public List<String> getAll(final String name) {
        return fields.getOrDefault(name, List.of());
    }

    /**
     * Returns all fields as an unmodifiable map.
     *
     * @return the field map
     */
    public Map<String, List<String>> fields() {
        return fields;
    }

    private static String decode(final String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
