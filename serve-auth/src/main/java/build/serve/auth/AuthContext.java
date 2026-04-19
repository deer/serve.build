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

import build.serve.foundation.context.RequestContext;

import java.util.Optional;

/**
 * Provides typed access to the authenticated {@link Principal} for the current request.
 * <p>
 * The principal is bound by {@link AuthMiddleware} via {@link RequestContext#PRINCIPAL}.
 * Route handlers call {@link #current()} to retrieve it:
 * <pre>{@code
 * AuthContext.current().ifPresent(p -> log.info("Request from {}", p.name()));
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class AuthContext {

    private AuthContext() {
    }

    /**
     * Returns the authenticated {@link Principal} for the current request, if present.
     *
     * @return the current {@link Principal}, or empty if the request is unauthenticated
     */
    public static Optional<Principal> current() {
        if (RequestContext.PRINCIPAL.isBound()) {
            return Optional.of((Principal) RequestContext.PRINCIPAL.get());
        }
        return Optional.empty();
    }
}
