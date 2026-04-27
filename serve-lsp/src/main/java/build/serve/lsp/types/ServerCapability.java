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
 * All server capabilities that can be declared in the initialize response.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum ServerCapability {

    CODE_ACTION("codeActionProvider"),
    CODE_LENS("codeLensProvider"),
    COMPLETION("completionProvider"),
    DECLARATION("declarationProvider"),
    DEFINITION("definitionProvider"),
    DOCUMENT_FORMATTING("documentFormattingProvider"),
    DOCUMENT_HIGHLIGHT("documentHighlightProvider"),
    DOCUMENT_RANGE_FORMATTING("documentRangeFormattingProvider"),
    DOCUMENT_SYMBOL("documentSymbolProvider"),
    FOLDING_RANGE("foldingRangeProvider"),
    HOVER("hoverProvider"),
    IMPLEMENTATION("implementationProvider"),
    INLAY_HINT("inlayHintProvider"),
    REFERENCES("referencesProvider"),
    RENAME("renameProvider"),
    SELECTION_RANGE("selectionRangeProvider"),
    SIGNATURE_HELP("signatureHelpProvider"),
    TYPE_DEFINITION("typeDefinitionProvider"),
    WORKSPACE_SYMBOL("workspaceSymbolProvider");

    public final String fieldName;

    ServerCapability(final String fieldName) {
        this.fieldName = fieldName;
    }
}
