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

import build.base.json.JsonObject;
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

    LspRequestMethod method();

    record CodeAction(CodeActionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.CODE_ACTION;
        }
    }

    record Completion(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.COMPLETION;
        }
    }

    record Declaration(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.DECLARATION;
        }
    }

    record Definition(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.DEFINITION;
        }
    }

    record DocumentHighlight(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.DOCUMENT_HIGHLIGHT;
        }
    }

    record DocumentSymbol(DocumentSymbolParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.DOCUMENT_SYMBOL;
        }
    }

    record ExecuteCommand(ExecuteCommandParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.EXECUTE_COMMAND;
        }
    }

    record FoldingRange(FoldingRangeParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.FOLDING_RANGE;
        }
    }

    record Formatting(FormattingParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.FORMATTING;
        }
    }

    record Hover(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.HOVER;
        }
    }

    record Implementation(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.IMPLEMENTATION;
        }
    }

    record Initialize(InitializeParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.INITIALIZE;
        }
    }

    record InlayHint(InlayHintParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.INLAY_HINT;
        }
    }

    record RangeFormatting(RangeFormattingParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.RANGE_FORMATTING;
        }
    }

    record References(ReferenceParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.REFERENCES;
        }
    }

    record Rename(RenameParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.RENAME;
        }
    }

    record SelectionRange(SelectionRangeParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.SELECTION_RANGE;
        }
    }

    record SignatureHelp(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.SIGNATURE_HELP;
        }
    }

    record TypeDefinition(TextDocumentPositionParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.TYPE_DEFINITION;
        }
    }

    record WorkspaceSymbol(WorkspaceSymbolParams params) implements LspRequest {
        @Override
        public LspRequestMethod method() {
            return LspRequestMethod.WORKSPACE_SYMBOL;
        }
    }

    static LspRequest parse(final LspRequestMethod method, final JsonObject params) {
        return switch (method) {
            case CODE_ACTION -> new CodeAction(LspJson.parseCodeAction(params));
            case COMPLETION -> new Completion(LspJson.parseTDPosition(params));
            case DECLARATION -> new Declaration(LspJson.parseTDPosition(params));
            case DEFINITION -> new Definition(LspJson.parseTDPosition(params));
            case DOCUMENT_HIGHLIGHT -> new DocumentHighlight(LspJson.parseTDPosition(params));
            case DOCUMENT_SYMBOL -> new DocumentSymbol(LspJson.parseDocumentSymbol(params));
            case EXECUTE_COMMAND -> new ExecuteCommand(LspJson.parseExecuteCommand(params));
            case FOLDING_RANGE -> new FoldingRange(LspJson.parseFoldingRange(params));
            case FORMATTING -> new Formatting(LspJson.parseFormatting(params));
            case HOVER -> new Hover(LspJson.parseTDPosition(params));
            case IMPLEMENTATION -> new Implementation(LspJson.parseTDPosition(params));
            case INITIALIZE -> new Initialize(LspJson.parseInitialize(params));
            case INLAY_HINT -> new InlayHint(LspJson.parseInlayHint(params));
            case RANGE_FORMATTING -> new RangeFormatting(LspJson.parseRangeFormatting(params));
            case REFERENCES -> new References(LspJson.parseReferences(params));
            case RENAME -> new Rename(LspJson.parseRename(params));
            case SELECTION_RANGE -> new SelectionRange(LspJson.parseSelectionRange(params));
            case SIGNATURE_HELP -> new SignatureHelp(LspJson.parseTDPosition(params));
            case TYPE_DEFINITION -> new TypeDefinition(LspJson.parseTDPosition(params));
            case WORKSPACE_SYMBOL -> new WorkspaceSymbol(LspJson.parseWorkspaceSymbol(params));
        };
    }
}
