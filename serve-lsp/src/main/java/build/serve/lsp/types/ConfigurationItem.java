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
 * A single item requested via {@code workspace/configuration}.
 *
 * @param scopeUri the scope to resolve the setting for, or {@code null} for the global scope
 * @param section  the dotted configuration section name, or {@code null} for the whole scope
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record ConfigurationItem(String scopeUri, String section) implements LspType {
}
