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
 * A completion item.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record CompletionItem(String label,
                             CompletionItemKind kind,
                             String detail,
                             MarkupContent documentation,
                             String insertText) implements LspType {

    /**
     * Creates a completion item with just a label.
     *
     * @param label the label
     * @return the completion item
     */
    public static CompletionItem of(final String label) {
        return new CompletionItem(label, null, null, null, null);
    }

    /**
     * Creates a completion item with a label and kind.
     *
     * @param label the label
     * @param kind  the kind
     * @return the completion item
     */
    public static CompletionItem of(final String label, final CompletionItemKind kind) {
        return new CompletionItem(label, kind, null, null, null);
    }
}
