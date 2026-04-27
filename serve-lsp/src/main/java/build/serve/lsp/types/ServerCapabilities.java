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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server capabilities declared in the initialize response.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServerCapabilities(Object textDocumentSync,
                                 @JsonIgnore Set<ServerCapability> providers) implements LspType {

    public static ServerCapabilities of(final ServerCapability... providers) {
        return new ServerCapabilities(null, toSet(providers));
    }

    public static ServerCapabilities of(final Object textDocumentSync, final ServerCapability... providers) {
        return new ServerCapabilities(textDocumentSync, toSet(providers));
    }

    private static Set<ServerCapability> toSet(final ServerCapability[] providers) {
        return providers.length > 0
            ? Collections.unmodifiableSet(EnumSet.copyOf(List.of(providers)))
            : Collections.unmodifiableSet(EnumSet.noneOf(ServerCapability.class));
    }

    @JsonAnyGetter
    public Map<String, Boolean> capabilityFields() {
        final Map<String, Boolean> fields = new LinkedHashMap<>();
        for (final ServerCapability cap : providers) {
            fields.put(cap.fieldName, Boolean.TRUE);
        }
        return fields;
    }
}
