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

import build.base.json.JsonValue;

/**
 * A type hierarchy item, returned by {@code textDocument/prepareTypeHierarchy} and passed back by
 * the client to {@code typeHierarchy/supertypes}/{@code subtypes}.
 *
 * @param name           the name of the type
 * @param kind           the kind of the type
 * @param detail         more detail for this item, e.g. the signature of a type, or {@code null}
 * @param uri            the resource identifier of this item
 * @param range          the range enclosing this type, not including leading/trailing whitespace
 *                       but everything else, e.g. comments and code
 * @param selectionRange the range that should be selected/highlighted when the type is picked,
 *                       e.g. the name of a class; must be contained by {@code range}
 * @param data           opaque data round-tripped back to the server on subsequent
 *                       {@code supertypes}/{@code subtypes} requests, or {@code null}
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public record TypeHierarchyItem(String name,
                                SymbolKind kind,
                                String detail,
                                String uri,
                                Range range,
                                Range selectionRange,
                                JsonValue data) implements LspType {
}
