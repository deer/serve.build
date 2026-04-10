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
 * The result of a hover request.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record Hover(MarkupContent contents, Range range) {

    /**
     * Creates a hover with no range.
     *
     * @param contents the contents
     * @return the hover
     */
    public static Hover of(final MarkupContent contents) {
        return new Hover(contents, null);
    }

    /**
     * Creates a hover with markdown content and no range.
     *
     * @param markdown the markdown text
     * @return the hover
     */
    public static Hover of(final String markdown) {
        return new Hover(MarkupContent.markdown(markdown), null);
    }
}
