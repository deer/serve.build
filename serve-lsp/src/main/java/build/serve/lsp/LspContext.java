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

import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.ShowMessageParams;

import java.util.List;

/**
 * Context passed to every LSP handler for server-initiated messages.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface LspContext {

    /**
     * Publishes diagnostics for the given URI.
     *
     * @param uri         the document URI
     * @param diagnostics the diagnostics
     */
    void publishDiagnostics(String uri, List<Diagnostic> diagnostics);

    /**
     * Shows a message to the client.
     *
     * @param params the message parameters
     */
    void showMessage(ShowMessageParams params);

    /**
     * Logs a message to the client.
     *
     * @param params the message parameters
     */
    void logMessage(ShowMessageParams params);

    /**
     * Shows a message request to the client.
     *
     * @param params the message parameters
     */
    void showMessageRequest(ShowMessageParams params);
}
