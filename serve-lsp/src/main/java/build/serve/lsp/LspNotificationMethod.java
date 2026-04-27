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
 * All LSP notification methods — fire-and-forget messages with no id and no response.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum LspNotificationMethod {

    DID_CHANGE("textDocument/didChange"),
    DID_CLOSE("textDocument/didClose"),
    DID_OPEN("textDocument/didOpen"),
    DID_SAVE("textDocument/didSave"),
    INITIALIZED("initialized");

    public final String methodName;

    private static final Map<String, LspNotificationMethod> BY_NAME = Stream.of(values())
        .collect(Collectors.toMap(m -> m.methodName, m -> m));

    LspNotificationMethod(final String methodName) {
        this.methodName = methodName;
    }

    public static Optional<LspNotificationMethod> from(final String name) {
        return Optional.ofNullable(BY_NAME.get(name));
    }
}
