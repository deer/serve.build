/*-
 * #%L
 * Serve Auth
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
package build.serve.auth;

import java.util.Objects;

/**
 * A simple {@link Principal} backed by an ID and display name.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record SimplePrincipal(String id, String name) implements Principal {

    /**
     * Constructs a {@link SimplePrincipal}.
     *
     * @param id   the principal ID
     * @param name the principal name
     */
    public SimplePrincipal {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }

    /**
     * Creates a {@link SimplePrincipal} with the given name used as both ID and display name.
     *
     * @param name the name
     * @return a new {@link SimplePrincipal}
     */
    public static SimplePrincipal of(final String name) {
        return new SimplePrincipal(name, name);
    }

    /**
     * Creates a {@link SimplePrincipal} with distinct ID and display name.
     *
     * @param id   the principal ID
     * @param name the display name
     * @return a new {@link SimplePrincipal}
     */
    public static SimplePrincipal of(final String id, final String name) {
        return new SimplePrincipal(id, name);
    }
}
