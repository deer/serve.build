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

/**
 * Thrown when a client responds to a server-initiated request (via {@link LspContext#sendRequest})
 * with a JSON-RPC error instead of a result.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public final class LspClientErrorException extends RuntimeException {

    private final int code;

    /**
     * Creates an exception wrapping a JSON-RPC error response.
     *
     * @param code    the JSON-RPC error code
     * @param message the error message
     */
    public LspClientErrorException(final int code, final String message) {
        super(message);
        this.code = code;
    }

    /**
     * Returns the JSON-RPC error code.
     *
     * @return the error code
     */
    public int code() {
        return code;
    }
}
