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
 * The diagnostic severity.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public enum DiagnosticSeverity {

    ERROR(1), WARNING(2), INFORMATION(3), HINT(4);

    private final int value;

    DiagnosticSeverity(final int value) {
        this.value = value;
    }

    /**
     * Returns the numeric LSP value of this severity.
     *
     * @return the value
     */
    @JsonValue
    public int value() {
        return value;
    }

    /**
     * Returns the {@link DiagnosticSeverity} for the given LSP integer value.
     *
     * @param value the LSP integer value
     * @return the matching severity
     * @throws IllegalArgumentException if the value is not a valid severity
     */
    @JsonCreator
    public static DiagnosticSeverity fromValue(final int value) {
        for (final var v : values()) {
            if (v.value == value) {
                return v;
            }
        }
        throw new IllegalArgumentException("Unknown DiagnosticSeverity value: " + value);
    }
}
