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

import build.base.json.JsonObject;

import java.util.List;

public final class ArrayParam<E> extends ToolParamBase<List<E>, ArrayParam<E>> {

    ArrayParam(final String name,
               final String description,
               final boolean required,
               final List<E> defaultValue,
               final ToolParam.Extractor<List<E>> extractor,
               final JsonObject propertySchema) {
        super(name, description, required, defaultValue, extractor, propertySchema);
    }

    @Override
    protected ArrayParam<E> copy(final boolean required, final List<E> defaultValue,
                                 final ToolParam.Extractor<List<E>> extractor, final JsonObject propertySchema) {
        return new ArrayParam<>(name, description, required, defaultValue, extractor, propertySchema);
    }

    @Override
    public ArrayParam<E> optional() {
        return copy(false, List.of(), extractor, propertySchema);
    }

    public ArrayParam<E> minItems(final int min) {
        final var schema = schemaBuilder().put("minItems", min).build();
        final var outer = extractor;
        return copy(required, defaultValue,
            val -> {
                final var list = outer.extract(val);
                if (list.size() < min) {
                    throw new IllegalArgumentException("'" + name + "' must have at least " + min + " item(s).");
                }
                return list;
            },
            schema);
    }

    public ArrayParam<E> maxItems(final int max) {
        final var schema = schemaBuilder().put("maxItems", max).build();
        final var outer = extractor;
        return copy(required, defaultValue,
            val -> {
                final var list = outer.extract(val);
                if (list.size() > max) {
                    throw new IllegalArgumentException("'" + name + "' must have at most " + max + " item(s).");
                }
                return list;
            },
            schema);
    }
}
