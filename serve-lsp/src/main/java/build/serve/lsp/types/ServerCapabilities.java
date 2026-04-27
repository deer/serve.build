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
 * Server capabilities.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record ServerCapabilities(Object textDocumentSync,
                                  boolean hoverProvider,
                                  boolean completionProvider,
                                  boolean definitionProvider,
                                  boolean declarationProvider,
                                  boolean typeDefinitionProvider,
                                  boolean implementationProvider,
                                  boolean referencesProvider,
                                  boolean documentHighlightProvider,
                                  boolean documentSymbolProvider,
                                  boolean codeActionProvider,
                                  boolean codeLensProvider,
                                  boolean documentFormattingProvider,
                                  boolean documentRangeFormattingProvider,
                                  boolean renameProvider,
                                  boolean foldingRangeProvider,
                                  boolean selectionRangeProvider,
                                  boolean signatureHelpProvider,
                                  boolean workspaceSymbolProvider,
                                  boolean inlayHintProvider) implements LspType {

    /**
     * Creates a new builder.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link ServerCapabilities}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private Object textDocumentSync;
        private boolean hoverProvider;
        private boolean completionProvider;
        private boolean definitionProvider;
        private boolean declarationProvider;
        private boolean typeDefinitionProvider;
        private boolean implementationProvider;
        private boolean referencesProvider;
        private boolean documentHighlightProvider;
        private boolean documentSymbolProvider;
        private boolean codeActionProvider;
        private boolean codeLensProvider;
        private boolean documentFormattingProvider;
        private boolean documentRangeFormattingProvider;
        private boolean renameProvider;
        private boolean foldingRangeProvider;
        private boolean selectionRangeProvider;
        private boolean signatureHelpProvider;
        private boolean workspaceSymbolProvider;
        private boolean inlayHintProvider;

        private Builder() {
        }

        public Builder textDocumentSync(final Object textDocumentSync) {
            this.textDocumentSync = textDocumentSync;
            return this;
        }

        public Builder hoverProvider(final boolean hoverProvider) {
            this.hoverProvider = hoverProvider;
            return this;
        }

        public Builder completionProvider(final boolean completionProvider) {
            this.completionProvider = completionProvider;
            return this;
        }

        public Builder definitionProvider(final boolean definitionProvider) {
            this.definitionProvider = definitionProvider;
            return this;
        }

        public Builder declarationProvider(final boolean declarationProvider) {
            this.declarationProvider = declarationProvider;
            return this;
        }

        public Builder typeDefinitionProvider(final boolean typeDefinitionProvider) {
            this.typeDefinitionProvider = typeDefinitionProvider;
            return this;
        }

        public Builder implementationProvider(final boolean implementationProvider) {
            this.implementationProvider = implementationProvider;
            return this;
        }

        public Builder referencesProvider(final boolean referencesProvider) {
            this.referencesProvider = referencesProvider;
            return this;
        }

        public Builder documentHighlightProvider(final boolean documentHighlightProvider) {
            this.documentHighlightProvider = documentHighlightProvider;
            return this;
        }

        public Builder documentSymbolProvider(final boolean documentSymbolProvider) {
            this.documentSymbolProvider = documentSymbolProvider;
            return this;
        }

        public Builder codeActionProvider(final boolean codeActionProvider) {
            this.codeActionProvider = codeActionProvider;
            return this;
        }

        public Builder codeLensProvider(final boolean codeLensProvider) {
            this.codeLensProvider = codeLensProvider;
            return this;
        }

        public Builder documentFormattingProvider(final boolean documentFormattingProvider) {
            this.documentFormattingProvider = documentFormattingProvider;
            return this;
        }

        public Builder documentRangeFormattingProvider(final boolean documentRangeFormattingProvider) {
            this.documentRangeFormattingProvider = documentRangeFormattingProvider;
            return this;
        }

        public Builder renameProvider(final boolean renameProvider) {
            this.renameProvider = renameProvider;
            return this;
        }

        public Builder foldingRangeProvider(final boolean foldingRangeProvider) {
            this.foldingRangeProvider = foldingRangeProvider;
            return this;
        }

        public Builder selectionRangeProvider(final boolean selectionRangeProvider) {
            this.selectionRangeProvider = selectionRangeProvider;
            return this;
        }

        public Builder signatureHelpProvider(final boolean signatureHelpProvider) {
            this.signatureHelpProvider = signatureHelpProvider;
            return this;
        }

        public Builder workspaceSymbolProvider(final boolean workspaceSymbolProvider) {
            this.workspaceSymbolProvider = workspaceSymbolProvider;
            return this;
        }

        public Builder inlayHintProvider(final boolean inlayHintProvider) {
            this.inlayHintProvider = inlayHintProvider;
            return this;
        }

        /**
         * Builds the {@link ServerCapabilities}.
         *
         * @return the capabilities
         */
        public ServerCapabilities build() {
            return new ServerCapabilities(
                textDocumentSync, hoverProvider, completionProvider, definitionProvider,
                declarationProvider, typeDefinitionProvider, implementationProvider, referencesProvider,
                documentHighlightProvider, documentSymbolProvider, codeActionProvider, codeLensProvider,
                documentFormattingProvider, documentRangeFormattingProvider, renameProvider,
                foldingRangeProvider, selectionRangeProvider, signatureHelpProvider,
                workspaceSymbolProvider, inlayHintProvider
            );
        }
    }
}
