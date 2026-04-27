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

import build.serve.lsp.params.DidChangeParams;
import build.serve.lsp.params.DidCloseParams;
import build.serve.lsp.params.DidOpenParams;
import build.serve.lsp.params.DidSaveParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Sealed hierarchy of all typed LSP notifications (fire-and-forget messages with no id).
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public sealed interface LspNotification {

    LspNotificationMethod method();

    record DidChange(DidChangeParams params) implements LspNotification {
        @Override
        public LspNotificationMethod method() {
            return LspNotificationMethod.DID_CHANGE;
        }
    }

    record DidClose(DidCloseParams params) implements LspNotification {
        @Override
        public LspNotificationMethod method() {
            return LspNotificationMethod.DID_CLOSE;
        }
    }

    record DidOpen(DidOpenParams params) implements LspNotification {
        @Override
        public LspNotificationMethod method() {
            return LspNotificationMethod.DID_OPEN;
        }
    }

    record DidSave(DidSaveParams params) implements LspNotification {
        @Override
        public LspNotificationMethod method() {
            return LspNotificationMethod.DID_SAVE;
        }
    }

    record Initialized() implements LspNotification {
        @Override
        public LspNotificationMethod method() {
            return LspNotificationMethod.INITIALIZED;
        }
    }

    static LspNotification parse(final LspNotificationMethod method,
                                 final ObjectMapper mapper,
                                 final JsonNode params) throws Exception {
        return switch (method) {
            case DID_CHANGE -> new DidChange(mapper.treeToValue(params, DidChangeParams.class));
            case DID_CLOSE -> new DidClose(mapper.treeToValue(params, DidCloseParams.class));
            case DID_OPEN -> new DidOpen(mapper.treeToValue(params, DidOpenParams.class));
            case DID_SAVE -> new DidSave(mapper.treeToValue(params, DidSaveParams.class));
            case INITIALIZED -> new Initialized();
        };
    }
}
