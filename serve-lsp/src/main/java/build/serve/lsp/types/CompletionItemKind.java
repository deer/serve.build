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
 * The kind of a completion entry.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public enum CompletionItemKind {

    TEXT(1), METHOD(2), FUNCTION(3), CONSTRUCTOR(4), FIELD(5),
    VARIABLE(6), CLASS(7), INTERFACE(8), MODULE(9), PROPERTY(10),
    UNIT(11), VALUE(12), ENUM(13), KEYWORD(14), SNIPPET(15),
    COLOR(16), FILE(17), REFERENCE(18), FOLDER(19), ENUM_MEMBER(20),
    CONSTANT(21), STRUCT(22), EVENT(23), OPERATOR(24), TYPE_PARAMETER(25);

    private final int value;

    CompletionItemKind(final int value) {
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
     * Returns the {@link CompletionItemKind} for the given LSP integer value.
     *
     * @param value the LSP integer value
     * @return the matching kind
     * @throws IllegalArgumentException if the value is not a valid kind
     */
    @JsonCreator
    public static CompletionItemKind fromValue(final int value) {
        for (final var v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown CompletionItemKind value: " + value);
    }
}
