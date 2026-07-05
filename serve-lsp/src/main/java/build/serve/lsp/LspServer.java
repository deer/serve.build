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

import build.serve.lsp.params.CallHierarchyIncomingCallsParams;
import build.serve.lsp.params.CallHierarchyOutgoingCallsParams;
import build.serve.lsp.params.CodeActionParams;
import build.serve.lsp.params.DidChangeParams;
import build.serve.lsp.params.DidCloseParams;
import build.serve.lsp.params.DidOpenParams;
import build.serve.lsp.params.DidSaveParams;
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
import build.serve.lsp.params.TypeHierarchySubtypesParams;
import build.serve.lsp.params.TypeHierarchySupertypesParams;
import build.serve.lsp.params.WorkspaceSymbolParams;
import build.serve.lsp.types.CallHierarchyIncomingCall;
import build.serve.lsp.types.CallHierarchyItem;
import build.serve.lsp.types.CallHierarchyOutgoingCall;
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
import build.serve.lsp.types.TypeHierarchyItem;
import build.serve.lsp.types.WorkspaceEdit;

import java.util.EnumMap;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An LSP server. Implement directly or use {@link #builder()} to register handlers via lambdas.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface LspServer {

    LspResponse handle(LspRequest request, LspContext ctx);

    void handle(LspNotification notification, LspContext ctx);

    static Builder builder() {
        return new Builder();
    }

    /**
     * Builds an {@link LspServer} from registered lambda handlers.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    final class Builder {

        private final EnumMap<LspRequestMethod, BiFunction<LspRequest, LspContext, Object>> requestHandlers =
            new EnumMap<>(LspRequestMethod.class);
        private final EnumMap<LspNotificationMethod, BiConsumer<LspNotification, LspContext>> notificationHandlers =
            new EnumMap<>(LspNotificationMethod.class);

        private Builder() {
        }

        public Builder onInitialize(final Function<InitializeParams, ServerCapabilities> h) {
            requestHandlers.put(LspRequestMethod.INITIALIZE,
                (r, ctx) -> h.apply(((LspRequest.Initialize) r).params()));
            return this;
        }

        public Builder onDidOpen(final BiConsumer<DidOpenParams, LspContext> h) {
            notificationHandlers.put(LspNotificationMethod.DID_OPEN,
                (n, ctx) -> h.accept(((LspNotification.DidOpen) n).params(), ctx));
            return this;
        }

        public Builder onDidChange(final BiConsumer<DidChangeParams, LspContext> h) {
            notificationHandlers.put(LspNotificationMethod.DID_CHANGE,
                (n, ctx) -> h.accept(((LspNotification.DidChange) n).params(), ctx));
            return this;
        }

        public Builder onDidClose(final BiConsumer<DidCloseParams, LspContext> h) {
            notificationHandlers.put(LspNotificationMethod.DID_CLOSE,
                (n, ctx) -> h.accept(((LspNotification.DidClose) n).params(), ctx));
            return this;
        }

        public Builder onDidSave(final BiConsumer<DidSaveParams, LspContext> h) {
            notificationHandlers.put(LspNotificationMethod.DID_SAVE,
                (n, ctx) -> h.accept(((LspNotification.DidSave) n).params(), ctx));
            return this;
        }

        public Builder onHover(final BiFunction<TextDocumentPositionParams, LspContext, Hover> h) {
            requestHandlers.put(LspRequestMethod.HOVER,
                (r, ctx) -> h.apply(((LspRequest.Hover) r).params(), ctx));
            return this;
        }

        public Builder onCompletion(final BiFunction<TextDocumentPositionParams, LspContext, List<CompletionItem>> h) {
            requestHandlers.put(LspRequestMethod.COMPLETION,
                (r, ctx) -> h.apply(((LspRequest.Completion) r).params(), ctx));
            return this;
        }

        public Builder onDefinition(final BiFunction<TextDocumentPositionParams, LspContext, List<Location>> h) {
            requestHandlers.put(LspRequestMethod.DEFINITION,
                (r, ctx) -> h.apply(((LspRequest.Definition) r).params(), ctx));
            return this;
        }

        public Builder onDeclaration(final BiFunction<TextDocumentPositionParams, LspContext, List<Location>> h) {
            requestHandlers.put(LspRequestMethod.DECLARATION,
                (r, ctx) -> h.apply(((LspRequest.Declaration) r).params(), ctx));
            return this;
        }

        public Builder onTypeDefinition(final BiFunction<TextDocumentPositionParams, LspContext, List<Location>> h) {
            requestHandlers.put(LspRequestMethod.TYPE_DEFINITION,
                (r, ctx) -> h.apply(((LspRequest.TypeDefinition) r).params(), ctx));
            return this;
        }

        public Builder onImplementation(final BiFunction<TextDocumentPositionParams, LspContext, List<Location>> h) {
            requestHandlers.put(LspRequestMethod.IMPLEMENTATION,
                (r, ctx) -> h.apply(((LspRequest.Implementation) r).params(), ctx));
            return this;
        }

        public Builder onPrepareCallHierarchy(final BiFunction<TextDocumentPositionParams, LspContext, List<CallHierarchyItem>> h) {
            requestHandlers.put(LspRequestMethod.PREPARE_CALL_HIERARCHY,
                (r, ctx) -> h.apply(((LspRequest.PrepareCallHierarchy) r).params(), ctx));
            return this;
        }

        public Builder onCallHierarchyIncomingCalls(final BiFunction<CallHierarchyIncomingCallsParams, LspContext, List<CallHierarchyIncomingCall>> h) {
            requestHandlers.put(LspRequestMethod.CALL_HIERARCHY_INCOMING_CALLS,
                (r, ctx) -> h.apply(((LspRequest.CallHierarchyIncomingCalls) r).params(), ctx));
            return this;
        }

        public Builder onCallHierarchyOutgoingCalls(final BiFunction<CallHierarchyOutgoingCallsParams, LspContext, List<CallHierarchyOutgoingCall>> h) {
            requestHandlers.put(LspRequestMethod.CALL_HIERARCHY_OUTGOING_CALLS,
                (r, ctx) -> h.apply(((LspRequest.CallHierarchyOutgoingCalls) r).params(), ctx));
            return this;
        }

        public Builder onPrepareTypeHierarchy(final BiFunction<TextDocumentPositionParams, LspContext, List<TypeHierarchyItem>> h) {
            requestHandlers.put(LspRequestMethod.PREPARE_TYPE_HIERARCHY,
                (r, ctx) -> h.apply(((LspRequest.PrepareTypeHierarchy) r).params(), ctx));
            return this;
        }

        public Builder onTypeHierarchySupertypes(final BiFunction<TypeHierarchySupertypesParams, LspContext, List<TypeHierarchyItem>> h) {
            requestHandlers.put(LspRequestMethod.TYPE_HIERARCHY_SUPERTYPES,
                (r, ctx) -> h.apply(((LspRequest.TypeHierarchySupertypes) r).params(), ctx));
            return this;
        }

        public Builder onTypeHierarchySubtypes(final BiFunction<TypeHierarchySubtypesParams, LspContext, List<TypeHierarchyItem>> h) {
            requestHandlers.put(LspRequestMethod.TYPE_HIERARCHY_SUBTYPES,
                (r, ctx) -> h.apply(((LspRequest.TypeHierarchySubtypes) r).params(), ctx));
            return this;
        }

        public Builder onReferences(final BiFunction<ReferenceParams, LspContext, List<Location>> h) {
            requestHandlers.put(LspRequestMethod.REFERENCES,
                (r, ctx) -> h.apply(((LspRequest.References) r).params(), ctx));
            return this;
        }

        public Builder onDocumentHighlight(final BiFunction<TextDocumentPositionParams, LspContext, List<DocumentHighlight>> h) {
            requestHandlers.put(LspRequestMethod.DOCUMENT_HIGHLIGHT,
                (r, ctx) -> h.apply(((LspRequest.DocumentHighlight) r).params(), ctx));
            return this;
        }

        public Builder onDocumentSymbol(final BiFunction<DocumentSymbolParams, LspContext, List<DocumentSymbol>> h) {
            requestHandlers.put(LspRequestMethod.DOCUMENT_SYMBOL,
                (r, ctx) -> h.apply(((LspRequest.DocumentSymbol) r).params(), ctx));
            return this;
        }

        public Builder onWorkspaceSymbol(final BiFunction<WorkspaceSymbolParams, LspContext, List<SymbolInformation>> h) {
            requestHandlers.put(LspRequestMethod.WORKSPACE_SYMBOL,
                (r, ctx) -> h.apply(((LspRequest.WorkspaceSymbol) r).params(), ctx));
            return this;
        }

        public Builder onCodeAction(final BiFunction<CodeActionParams, LspContext, List<CodeAction>> h) {
            requestHandlers.put(LspRequestMethod.CODE_ACTION,
                (r, ctx) -> h.apply(((LspRequest.CodeAction) r).params(), ctx));
            return this;
        }

        public Builder onSignatureHelp(final BiFunction<TextDocumentPositionParams, LspContext, SignatureHelp> h) {
            requestHandlers.put(LspRequestMethod.SIGNATURE_HELP,
                (r, ctx) -> h.apply(((LspRequest.SignatureHelp) r).params(), ctx));
            return this;
        }

        public Builder onRename(final BiFunction<RenameParams, LspContext, WorkspaceEdit> h) {
            requestHandlers.put(LspRequestMethod.RENAME,
                (r, ctx) -> h.apply(((LspRequest.Rename) r).params(), ctx));
            return this;
        }

        public Builder onFormatting(final BiFunction<FormattingParams, LspContext, List<TextEdit>> h) {
            requestHandlers.put(LspRequestMethod.FORMATTING,
                (r, ctx) -> h.apply(((LspRequest.Formatting) r).params(), ctx));
            return this;
        }

        public Builder onRangeFormatting(final BiFunction<RangeFormattingParams, LspContext, List<TextEdit>> h) {
            requestHandlers.put(LspRequestMethod.RANGE_FORMATTING,
                (r, ctx) -> h.apply(((LspRequest.RangeFormatting) r).params(), ctx));
            return this;
        }

        public Builder onFoldingRange(final BiFunction<FoldingRangeParams, LspContext, List<FoldingRange>> h) {
            requestHandlers.put(LspRequestMethod.FOLDING_RANGE,
                (r, ctx) -> h.apply(((LspRequest.FoldingRange) r).params(), ctx));
            return this;
        }

        public Builder onSelectionRange(final BiFunction<SelectionRangeParams, LspContext, List<SelectionRange>> h) {
            requestHandlers.put(LspRequestMethod.SELECTION_RANGE,
                (r, ctx) -> h.apply(((LspRequest.SelectionRange) r).params(), ctx));
            return this;
        }

        public Builder onInlayHint(final BiFunction<InlayHintParams, LspContext, List<InlayHint>> h) {
            requestHandlers.put(LspRequestMethod.INLAY_HINT,
                (r, ctx) -> h.apply(((LspRequest.InlayHint) r).params(), ctx));
            return this;
        }

        public Builder onExecuteCommand(final BiFunction<ExecuteCommandParams, LspContext, Object> h) {
            requestHandlers.put(LspRequestMethod.EXECUTE_COMMAND,
                (r, ctx) -> h.apply(((LspRequest.ExecuteCommand) r).params(), ctx));
            return this;
        }

        public LspServer build() {
            return new Dispatcher(
                new EnumMap<>(requestHandlers),
                new EnumMap<>(notificationHandlers));
        }

        // LspResponse.MethodNotFound is intentionally never returned by Dispatcher — unregistered methods
        // return Ok(null), which the LSP spec treats as a valid "no result" response.
        private record Dispatcher(EnumMap<LspRequestMethod, BiFunction<LspRequest, LspContext, Object>> requestHandlers,
                                  EnumMap<LspNotificationMethod, BiConsumer<LspNotification, LspContext>> notificationHandlers) implements LspServer {

            @Override
            public LspResponse handle(final LspRequest request, final LspContext ctx) {
                final var handler = requestHandlers.get(request.method());
                return new LspResponse.Ok(handler != null ? handler.apply(request, ctx) : null);
            }

            @Override
            public void handle(final LspNotification notification, final LspContext ctx) {
                try {
                    final var handler = notificationHandlers.get(notification.method());
                    if (handler != null) {
                        handler.accept(notification, ctx);
                    }
                } catch (final Exception e) {
                    // silently ignore notification errors
                }
            }
        }
    }
}
