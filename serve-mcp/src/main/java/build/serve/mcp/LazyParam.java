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

import build.base.json.JsonNull;
import build.base.json.JsonObject;
import build.base.json.JsonValue;

import java.util.Map;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * A lazy, self-referential parameter whose property schema is a {@code $ref} to a named {@code $defs} entry.
 *
 * <p>The {@code $ref} schema ({@code {"$ref":"#/$defs/<defName>"}}) is pre-computed at construction time
 * so that {@link #propertySchema()} never triggers the supplier. The supplier is resolved exactly once,
 * on the first call to {@link #extract}, {@link #extractSelf}, or {@link #defs}.
 *
 * <p><b>Transitivity limitation:</b> {@link ToolDef#inputSchema()} only hoists {@code defs()} from
 * top-level params. If the resolved schema itself contains further {@code $ref}s with their own
 * {@code defs()}, those nested entries are not collected automatically.
 *
 * @author reed.vonredwitz
 * @since May-2026
 */
final class LazyParam<T> implements ToolParam<T> {

    private final String name;
    private final String description;
    private final String defName;
    private final Supplier<ToolParam<T>> supplier;
    private final boolean required;
    private final T defaultValue;
    private final JsonObject refSchema;
    private volatile ToolParam<T> resolved;

    LazyParam(final String name,
              final String description,
              final String defName,
              final Supplier<ToolParam<T>> supplier) {
        this(name, description, defName, supplier, true, null);
    }

    private LazyParam(final String name,
                      final String description,
                      final String defName,
                      final Supplier<ToolParam<T>> supplier,
                      final boolean required,
                      final T defaultValue) {
        this.name = name;
        this.description = description;
        this.defName = defName;
        this.supplier = supplier;
        this.required = required;
        this.defaultValue = defaultValue;
        this.refSchema = JsonObject.builder().put("$ref", "#/$defs/" + defName).build();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean isRequired() {
        return required;
    }

    @Override
    public JsonObject propertySchema() {
        return refSchema;
    }

    @Override
    public Map<String, JsonObject> defs() {
        return Map.of(defName, resolve().propertySchema());
    }

    @Override
    public T extract(final JsonValue args) {
        final var obj = args.asObject();
        if (!obj.has(name) || obj.get(name) instanceof JsonNull) {
            if (required) {
                throw new IllegalArgumentException("Missing required argument '" + name + "'.");
            }
            return defaultValue;
        }
        return extractSelf(obj.get(name));
    }

    @Override
    public T extractSelf(final JsonValue value) {
        if (value instanceof JsonNull) {
            if (required) {
                throw new IllegalArgumentException("Value must not be null.");
            }
            return defaultValue;
        }
        return resolve().extractSelf(value);
    }

    @Override
    public ToolParam<T> optional() {
        return new LazyParam<>(name, description, defName, supplier, false, null);
    }

    @Override
    public ToolParam<T> optional(final T defaultValue) {
        return new LazyParam<>(name, description, defName, supplier, false, defaultValue);
    }

    @Override
    public ToolParam<T> refine(final Predicate<T> predicate, final String message) {
        throw new UnsupportedOperationException("LazyParam does not support refine().");
    }

    private ToolParam<T> resolve() {
        if (resolved == null) {
            synchronized (this) {
                if (resolved == null) {
                    resolved = supplier.get();
                }
            }
        }
        return resolved;
    }
}
