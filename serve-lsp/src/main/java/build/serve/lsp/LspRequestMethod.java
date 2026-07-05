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
package build.serve.lsp;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * All LSP request methods — requests that carry an id and expect a response.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum LspRequestMethod {

    CALL_HIERARCHY_INCOMING_CALLS("callHierarchy/incomingCalls"),
    CALL_HIERARCHY_OUTGOING_CALLS("callHierarchy/outgoingCalls"),
    CODE_ACTION("textDocument/codeAction"),
    COMPLETION("textDocument/completion"),
    DECLARATION("textDocument/declaration"),
    DEFINITION("textDocument/definition"),
    DOCUMENT_HIGHLIGHT("textDocument/documentHighlight"),
    DOCUMENT_SYMBOL("textDocument/documentSymbol"),
    EXECUTE_COMMAND("workspace/executeCommand"),
    FOLDING_RANGE("textDocument/foldingRange"),
    FORMATTING("textDocument/formatting"),
    HOVER("textDocument/hover"),
    IMPLEMENTATION("textDocument/implementation"),
    INITIALIZE("initialize"),
    INLAY_HINT("textDocument/inlayHint"),
    PREPARE_CALL_HIERARCHY("textDocument/prepareCallHierarchy"),
    PREPARE_TYPE_HIERARCHY("textDocument/prepareTypeHierarchy"),
    RANGE_FORMATTING("textDocument/rangeFormatting"),
    REFERENCES("textDocument/references"),
    RENAME("textDocument/rename"),
    SELECTION_RANGE("textDocument/selectionRange"),
    SIGNATURE_HELP("textDocument/signatureHelp"),
    TYPE_DEFINITION("textDocument/typeDefinition"),
    TYPE_HIERARCHY_SUBTYPES("typeHierarchy/subtypes"),
    TYPE_HIERARCHY_SUPERTYPES("typeHierarchy/supertypes"),
    WORKSPACE_SYMBOL("workspace/symbol");

    public final String methodName;

    private static final Map<String, LspRequestMethod> BY_NAME = Stream.of(values())
        .collect(Collectors.toMap(m -> m.methodName, m -> m));

    LspRequestMethod(final String methodName) {
        this.methodName = methodName;
    }

    public static Optional<LspRequestMethod> from(final String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }
}
