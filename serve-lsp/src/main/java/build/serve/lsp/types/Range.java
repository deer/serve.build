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

/**
 * A range in a text document.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record Range(Position start,
                    Position end) implements LspType {

    /**
     * Creates a range from line/character coordinates.
     *
     * @param startLine the start line
     * @param startChar the start character
     * @param endLine   the end line
     * @param endChar   the end character
     * @return the range
     */
    public static Range of(final int startLine, final int startChar, final int endLine, final int endChar) {
        return new Range(new Position(startLine, startChar), new Position(endLine, endChar));
    }
}
