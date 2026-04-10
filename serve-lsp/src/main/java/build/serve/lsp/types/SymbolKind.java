/*-
 * #%L
 * Serve LSP
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
package build.serve.lsp.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * A symbol kind.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public enum SymbolKind {

    FILE(1), MODULE(2), NAMESPACE(3), PACKAGE(4), CLASS(5),
    METHOD(6), PROPERTY(7), FIELD(8), CONSTRUCTOR(9), ENUM(10),
    INTERFACE(11), FUNCTION(12), VARIABLE(13), CONSTANT(14), STRING(15),
    NUMBER(16), BOOLEAN(17), ARRAY(18), OBJECT(19), KEY(20),
    NULL(21), ENUM_MEMBER(22), STRUCT(23), EVENT(24), OPERATOR(25),
    TYPE_PARAMETER(26);

    private final int value;

    SymbolKind(final int value) {
        this.value = value;
    }

    /**
     * Returns the numeric LSP value of this kind.
     *
     * @return the value
     */
    @JsonValue
    public int value() {
        return value;
    }

    /**
     * Returns the {@link SymbolKind} for the given LSP integer value.
     *
     * @param value the LSP integer value
     * @return the matching kind
     * @throws IllegalArgumentException if the value is not a valid kind
     */
    @JsonCreator
    public static SymbolKind fromValue(final int value) {
        for (final var v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown SymbolKind value: " + value);
    }
}
