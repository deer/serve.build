/*-
 * #%L
 * Serve Foundation
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
package build.serve.foundation.util;

/**
 * Minimal JSON string utilities for hand-rolled JSON builders.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class JsonStrings {

    private JsonStrings() {
    }

    /**
     * Escapes a string value for safe embedding inside a JSON string literal.
     * Handles {@code "}, {@code \}, and control characters (U+0000–U+001F).
     *
     * @param value the raw string
     * @return the escaped string, safe to wrap in {@code "}
     */
    public static String escape(final String value) {
        final var sb = new StringBuilder(value.length());

        for (int i = 0; i < value.length(); i++) {
            final char c = value.charAt(i);

            if (c == '"') {
                sb.append("\\\"");
            } else if (c == '\\') {
                sb.append("\\\\");
            } else if (c < 0x20) {
                sb.append(String.format("\\u%04x", (int) c));
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }
}
