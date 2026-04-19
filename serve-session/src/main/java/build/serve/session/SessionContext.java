/*-
 * #%L
 * Serve Session
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
package build.serve.session;

import java.util.Optional;

/**
 * Provides access to the {@link Session} for the current request.
 * <p>
 * The session is bound by {@link SessionMiddleware} for the duration of each request.
 * <pre>{@code
 * SessionContext.current().ifPresent(session -> {
 *     session.set("userId", userId);
 * });
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class SessionContext {

    static final ScopedValue<Session> SESSION = ScopedValue.newInstance();

    private SessionContext() {
    }

    /**
     * Returns the {@link Session} for the current request, if present.
     *
     * @return the current session, or empty if no session middleware is active
     */
    public static Optional<Session> current() {
        return SESSION.isBound() ? Optional.of(SESSION.get()) : Optional.empty();
    }
}
