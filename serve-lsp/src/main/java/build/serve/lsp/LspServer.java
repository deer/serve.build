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

import build.serve.lsp.types.CodeAction;
import build.serve.lsp.types.CompletionItem;
import build.serve.lsp.types.DocumentHighlight;
import build.serve.lsp.types.DocumentSymbol;
import build.serve.lsp.types.FoldingRange;
import build.serve.lsp.types.Hover;
import build.serve.lsp.types.InlayHint;
import build.serve.lsp.types.Location;
import build.serve.lsp.types.SelectionRange;
import build.serve.lsp.types.ServerCapabilities;
import build.serve.lsp.types.SignatureHelp;
import build.serve.lsp.types.SymbolInformation;
import build.serve.lsp.types.TextEdit;
import build.serve.lsp.types.WorkspaceEdit;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An LSP (Language Server Protocol) server with typed handler registration.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class LspServer {

    final Function<InitializeParams, ServerCapabilities> onInitialize;
    final BiConsumer<DidOpenParams, LspContext> onDidOpen;
    final BiConsumer<DidChangeParams, LspContext> onDidChange;
    final BiConsumer<DidCloseParams, LspContext> onDidClose;
    final BiConsumer<DidSaveParams, LspContext> onDidSave;
    final BiFunction<TextDocumentPositionParams, LspContext, Hover> onHover;
    final BiFunction<TextDocumentPositionParams, LspContext, List<CompletionItem>> onCompletion;
    final BiFunction<TextDocumentPositionParams, LspContext, Location> onDefinition;
    final BiFunction<TextDocumentPositionParams, LspContext, Location> onDeclaration;
    final BiFunction<TextDocumentPositionParams, LspContext, Location> onTypeDefinition;
    final BiFunction<TextDocumentPositionParams, LspContext, Location> onImplementation;
    final BiFunction<ReferenceParams, LspContext, List<Location>> onReferences;
    final BiFunction<TextDocumentPositionParams, LspContext, List<DocumentHighlight>> onDocumentHighlight;
    final BiFunction<DocumentSymbolParams, LspContext, List<DocumentSymbol>> onDocumentSymbol;
    final BiFunction<WorkspaceSymbolParams, LspContext, List<SymbolInformation>> onWorkspaceSymbol;
    final BiFunction<CodeActionParams, LspContext, List<CodeAction>> onCodeAction;
    final BiFunction<TextDocumentPositionParams, LspContext, SignatureHelp> onSignatureHelp;
    final BiFunction<RenameParams, LspContext, WorkspaceEdit> onRename;
    final BiFunction<FormattingParams, LspContext, List<TextEdit>> onFormatting;
    final BiFunction<RangeFormattingParams, LspContext, List<TextEdit>> onRangeFormatting;
    final BiFunction<FoldingRangeParams, LspContext, List<FoldingRange>> onFoldingRange;
    final BiFunction<SelectionRangeParams, LspContext, List<SelectionRange>> onSelectionRange;
    final BiFunction<InlayHintParams, LspContext, List<InlayHint>> onInlayHint;
    final BiFunction<ExecuteCommandParams, LspContext, Object> onExecuteCommand;

    private LspServer(final Builder builder) {
        this.onInitialize = builder.onInitialize;
        this.onDidOpen = builder.onDidOpen;
        this.onDidChange = builder.onDidChange;
        this.onDidClose = builder.onDidClose;
        this.onDidSave = builder.onDidSave;
        this.onHover = builder.onHover;
        this.onCompletion = builder.onCompletion;
        this.onDefinition = builder.onDefinition;
        this.onDeclaration = builder.onDeclaration;
        this.onTypeDefinition = builder.onTypeDefinition;
        this.onImplementation = builder.onImplementation;
        this.onReferences = builder.onReferences;
        this.onDocumentHighlight = builder.onDocumentHighlight;
        this.onDocumentSymbol = builder.onDocumentSymbol;
        this.onWorkspaceSymbol = builder.onWorkspaceSymbol;
        this.onCodeAction = builder.onCodeAction;
        this.onSignatureHelp = builder.onSignatureHelp;
        this.onRename = builder.onRename;
        this.onFormatting = builder.onFormatting;
        this.onRangeFormatting = builder.onRangeFormatting;
        this.onFoldingRange = builder.onFoldingRange;
        this.onSelectionRange = builder.onSelectionRange;
        this.onInlayHint = builder.onInlayHint;
        this.onExecuteCommand = builder.onExecuteCommand;
    }

    /**
     * Creates a new {@link Builder}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link LspServer}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private Function<InitializeParams, ServerCapabilities> onInitialize;
        private BiConsumer<DidOpenParams, LspContext> onDidOpen;
        private BiConsumer<DidChangeParams, LspContext> onDidChange;
        private BiConsumer<DidCloseParams, LspContext> onDidClose;
        private BiConsumer<DidSaveParams, LspContext> onDidSave;
        private BiFunction<TextDocumentPositionParams, LspContext, Hover> onHover;
        private BiFunction<TextDocumentPositionParams, LspContext, List<CompletionItem>> onCompletion;
        private BiFunction<TextDocumentPositionParams, LspContext, Location> onDefinition;
        private BiFunction<TextDocumentPositionParams, LspContext, Location> onDeclaration;
        private BiFunction<TextDocumentPositionParams, LspContext, Location> onTypeDefinition;
        private BiFunction<TextDocumentPositionParams, LspContext, Location> onImplementation;
        private BiFunction<ReferenceParams, LspContext, List<Location>> onReferences;
        private BiFunction<TextDocumentPositionParams, LspContext, List<DocumentHighlight>> onDocumentHighlight;
        private BiFunction<DocumentSymbolParams, LspContext, List<DocumentSymbol>> onDocumentSymbol;
        private BiFunction<WorkspaceSymbolParams, LspContext, List<SymbolInformation>> onWorkspaceSymbol;
        private BiFunction<CodeActionParams, LspContext, List<CodeAction>> onCodeAction;
        private BiFunction<TextDocumentPositionParams, LspContext, SignatureHelp> onSignatureHelp;
        private BiFunction<RenameParams, LspContext, WorkspaceEdit> onRename;
        private BiFunction<FormattingParams, LspContext, List<TextEdit>> onFormatting;
        private BiFunction<RangeFormattingParams, LspContext, List<TextEdit>> onRangeFormatting;
        private BiFunction<FoldingRangeParams, LspContext, List<FoldingRange>> onFoldingRange;
        private BiFunction<SelectionRangeParams, LspContext, List<SelectionRange>> onSelectionRange;
        private BiFunction<InlayHintParams, LspContext, List<InlayHint>> onInlayHint;
        private BiFunction<ExecuteCommandParams, LspContext, Object> onExecuteCommand;

        private Builder() {
        }

        public Builder onInitialize(final Function<InitializeParams, ServerCapabilities> handler) {
            this.onInitialize = handler;
            return this;
        }

        public Builder onDidOpen(final BiConsumer<DidOpenParams, LspContext> handler) {
            this.onDidOpen = handler;
            return this;
        }

        public Builder onDidChange(final BiConsumer<DidChangeParams, LspContext> handler) {
            this.onDidChange = handler;
            return this;
        }

        public Builder onDidClose(final BiConsumer<DidCloseParams, LspContext> handler) {
            this.onDidClose = handler;
            return this;
        }

        public Builder onDidSave(final BiConsumer<DidSaveParams, LspContext> handler) {
            this.onDidSave = handler;
            return this;
        }

        public Builder onHover(final BiFunction<TextDocumentPositionParams, LspContext, Hover> handler) {
            this.onHover = handler;
            return this;
        }

        public Builder onCompletion(final BiFunction<TextDocumentPositionParams, LspContext, List<CompletionItem>> handler) {
            this.onCompletion = handler;
            return this;
        }

        public Builder onDefinition(final BiFunction<TextDocumentPositionParams, LspContext, Location> handler) {
            this.onDefinition = handler;
            return this;
        }

        public Builder onDeclaration(final BiFunction<TextDocumentPositionParams, LspContext, Location> handler) {
            this.onDeclaration = handler;
            return this;
        }

        public Builder onTypeDefinition(final BiFunction<TextDocumentPositionParams, LspContext, Location> handler) {
            this.onTypeDefinition = handler;
            return this;
        }

        public Builder onImplementation(final BiFunction<TextDocumentPositionParams, LspContext, Location> handler) {
            this.onImplementation = handler;
            return this;
        }

        public Builder onReferences(final BiFunction<ReferenceParams, LspContext, List<Location>> handler) {
            this.onReferences = handler;
            return this;
        }

        public Builder onDocumentHighlight(final BiFunction<TextDocumentPositionParams, LspContext, List<DocumentHighlight>> handler) {
            this.onDocumentHighlight = handler;
            return this;
        }

        public Builder onDocumentSymbol(final BiFunction<DocumentSymbolParams, LspContext, List<DocumentSymbol>> handler) {
            this.onDocumentSymbol = handler;
            return this;
        }

        public Builder onWorkspaceSymbol(final BiFunction<WorkspaceSymbolParams, LspContext, List<SymbolInformation>> handler) {
            this.onWorkspaceSymbol = handler;
            return this;
        }

        public Builder onCodeAction(final BiFunction<CodeActionParams, LspContext, List<CodeAction>> handler) {
            this.onCodeAction = handler;
            return this;
        }

        public Builder onSignatureHelp(final BiFunction<TextDocumentPositionParams, LspContext, SignatureHelp> handler) {
            this.onSignatureHelp = handler;
            return this;
        }

        public Builder onRename(final BiFunction<RenameParams, LspContext, WorkspaceEdit> handler) {
            this.onRename = handler;
            return this;
        }

        public Builder onFormatting(final BiFunction<FormattingParams, LspContext, List<TextEdit>> handler) {
            this.onFormatting = handler;
            return this;
        }

        public Builder onRangeFormatting(final BiFunction<RangeFormattingParams, LspContext, List<TextEdit>> handler) {
            this.onRangeFormatting = handler;
            return this;
        }

        public Builder onFoldingRange(final BiFunction<FoldingRangeParams, LspContext, List<FoldingRange>> handler) {
            this.onFoldingRange = handler;
            return this;
        }

        public Builder onSelectionRange(final BiFunction<SelectionRangeParams, LspContext, List<SelectionRange>> handler) {
            this.onSelectionRange = handler;
            return this;
        }

        public Builder onInlayHint(final BiFunction<InlayHintParams, LspContext, List<InlayHint>> handler) {
            this.onInlayHint = handler;
            return this;
        }

        public Builder onExecuteCommand(final BiFunction<ExecuteCommandParams, LspContext, Object> handler) {
            this.onExecuteCommand = handler;
            return this;
        }

        /**
         * Builds the {@link LspServer}.
         *
         * @return the server
         */
        public LspServer build() {
            return new LspServer(this);
        }
    }
}
