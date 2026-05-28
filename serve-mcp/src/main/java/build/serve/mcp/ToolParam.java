/*-
 * #%L
 * Serve MCP
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
package build.serve.mcp;

import build.base.json.JsonArray;
import build.base.json.JsonObject;
import build.base.json.JsonValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A typed tool parameter declaration that drives both JSON Schema generation and type-safe argument extraction.
 *
 * <p>Use the static factories to declare parameters; call {@link #optional()} or {@link #optional(Object)}
 * to make them optional. Use {@code var} or the concrete type to retain access to constraint methods:
 *
 * <pre>{@code
 * var name   = ToolParam.string("name",  "Voice name");
 * var octave = ToolParam.integer("octave", "Bass octave").optional(3);
 * var dir    = ToolParam.string("direction", "...").values(List.of("up","down")).optional("up");
 * var tempo  = ToolParam.number("tempo", "BPM").min(20.0).max(300.0);
 * var title  = ToolParam.string("title", "Track title").minLength(1).maxLength(200);
 * }</pre>
 *
 * <p>Object and array schemas compose from other params:
 *
 * <pre>{@code
 * var type     = ToolParam.string("type", "Step type").values(List.of("seed", "filter", "terminal"));
 * var id       = ToolParam.string("id", "Step ID");
 * var step     = ToolParam.object("step", "A pipeline step", List.of(type, id));
 * var pipeline = ToolParam.array("pipeline", "Ordered steps", step);
 * }</pre>
 *
 * @param <T> the extracted Java type
 * @author reed.vonredwitz
 * @since May-2026
 */
public interface ToolParam<T> {

    String name();

    String description();

    boolean isRequired();

    JsonObject propertySchema();

    T extract(JsonValue args);

    ToolParam<T> optional();

    ToolParam<T> optional(T defaultValue);

    ToolParam<T> refine(Predicate<T> predicate, String message);

    /**
     * Extracts the parameter value, returning {@link Optional#empty()} instead of throwing if the
     * value is absent or fails validation. Most useful for required parameters where callers prefer
     * an {@code Optional} over a try/catch. For optional parameters with no default, prefer
     * {@link #extract} directly — both paths return the same empty result and the distinction is lost.
     */
    default Optional<T> safeParse(final JsonValue args) {
        try {
            return Optional.ofNullable(extract(args));
        } catch (final IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    // --- Factories ---

    static StringParam string(final String name,
                              final String description) {
        return new StringParam(name, description, true, null,
            val -> val.asString().value(),
            JsonObject.builder().put("type", "string").put("description", description).build());
    }

    static NumberParam<Integer> integer(final String name,
                                        final String description) {
        return new NumberParam<>(name, description, true, null,
            val -> val.asNumber().toNumber().intValue(),
            JsonObject.builder().put("type", "integer").put("description", description).build());
    }

    static BoolParam bool(final String name,
                          final String description) {
        return new BoolParam(name, description, true, null,
            val -> val.asBoolean().value(),
            JsonObject.builder().put("type", "boolean").put("description", description).build());
    }

    static NumberParam<Double> number(final String name,
                                      final String description) {
        return new NumberParam<>(name, description, true, null,
            val -> val.asNumber().toNumber().doubleValue(),
            JsonObject.builder().put("type", "number").put("description", description).build());
    }

    static ArrayParam<JsonValue> array(final String name,
                                       final String description) {
        return new ArrayParam<>(name, description, true, null,
            val -> val.asArray().values(),
            JsonObject.builder().put("type", "array").put("description", description).build());
    }

    static <T> ArrayParam<T> array(final String name,
                                   final String description,
                                   final ToolParam<T> itemSchema) {
        final var impl = (ToolParamBase<T, ?>) itemSchema;
        return new ArrayParam<>(name, description, true, null,
            val -> val.asArray().values().stream().map(impl::extractValue).toList(),
            JsonObject.builder()
                .put("type", "array")
                .put("description", description)
                .put("items", itemSchema.propertySchema())
                .build());
    }

    static ObjectParam object(final String name,
                              final String description,
                              final List<ToolParam<?>> fields) {
        final var props = JsonObject.builder();
        final var requiredArr = JsonArray.builder();
        for (final ToolParam<?> field : fields) {
            props.put(field.name(), field.propertySchema());
            if (field.isRequired()) {
                requiredArr.add(field.name());
            }
        }
        final var schema = JsonObject.builder()
            .put("type", "object")
            .put("description", description)
            .put("properties", props.build())
            .put("required", requiredArr.build())
            .build();
        return new ObjectParam(name, description, true, null,
            val -> {
                final var obj = val.asObject();
                for (final ToolParam<?> field : fields) {
                    field.extract(obj);
                }
                return obj;
            },
            schema);
    }

    static <T> ToolParam<T> of(final String name,
                               final String description,
                               final JsonObject schema,
                               final Extractor<T> extractor) {
        return new GenericParam<>(name, description, true, null, extractor, schema);
    }

    // --- Extractor ---

    @FunctionalInterface
    interface Extractor<T> {
        T extract(JsonValue value);
    }
}
