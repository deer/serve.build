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

import build.serve.lsp.params.CodeActionParams;
import build.serve.lsp.params.DocumentSymbolParams;
import build.serve.lsp.params.ExecuteCommandParams;
import build.serve.lsp.params.FoldingRangeParams;
import build.serve.lsp.params.FormattingParams;
import build.serve.lsp.params.InitializeParams;
import build.serve.lsp.params.InlayHintParams;
import build.serve.lsp.params.RangeFormattingParams;
import build.serve.lsp.params.ReferenceParams;
import build.serve.lsp.params.RenameParams;
import build.serve.lsp.params.SelectionRangeParams;
import build.serve.lsp.params.TextDocumentPositionParams;
import build.serve.lsp.params.WorkspaceSymbolParams;

/**
 * Sealed hierarchy of all typed LSP requests (messages with an id that expect a response).
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public sealed interface LspRequest {

    record CodeAction(CodeActionParams params) implements LspRequest {}

    record Completion(TextDocumentPositionParams params) implements LspRequest {}

    record Declaration(TextDocumentPositionParams params) implements LspRequest {}

    record Definition(TextDocumentPositionParams params) implements LspRequest {}

    record DocumentHighlight(TextDocumentPositionParams params) implements LspRequest {}

    record DocumentSymbol(DocumentSymbolParams params) implements LspRequest {}

    record ExecuteCommand(ExecuteCommandParams params) implements LspRequest {}

    record FoldingRange(FoldingRangeParams params) implements LspRequest {}

    record Formatting(FormattingParams params) implements LspRequest {}

    record Hover(TextDocumentPositionParams params) implements LspRequest {}

    record Implementation(TextDocumentPositionParams params) implements LspRequest {}

    record Initialize(InitializeParams params) implements LspRequest {}

    record InlayHint(InlayHintParams params) implements LspRequest {}

    record RangeFormatting(RangeFormattingParams params) implements LspRequest {}

    record References(ReferenceParams params) implements LspRequest {}

    record Rename(RenameParams params) implements LspRequest {}

    record SelectionRange(SelectionRangeParams params) implements LspRequest {}

    record SignatureHelp(TextDocumentPositionParams params) implements LspRequest {}

    record TypeDefinition(TextDocumentPositionParams params) implements LspRequest {}

    record WorkspaceSymbol(WorkspaceSymbolParams params) implements LspRequest {}
}
