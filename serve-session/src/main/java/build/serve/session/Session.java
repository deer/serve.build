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

import java.util.Map;
import java.util.Optional;

/**
 * A server-side session associated with a browser via a session cookie.
 * <p>
 * Sessions are retrieved per-request via {@link SessionContext#current()} and are backed by a
 * {@link SessionStore}. Mutations are persisted automatically at the end of each request.
 * <p>
 * To propagate an authenticated principal through subsequent requests, store it under
 * {@link SessionMiddleware#PRINCIPAL_KEY} and regenerate the session ID in the same step to
 * defend against session fixation:
 * <pre>{@code
 * SessionContext.current().ifPresent(s -> {
 *     s.set(SessionMiddleware.PRINCIPAL_KEY, principal);
 *     s.regenerateId();
 * });
 * }</pre>
 * {@link SessionMiddleware} will then bind it to {@code RequestContext.PRINCIPAL} for the duration
 * of each request, making it available via {@code AuthContext.current()}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public interface Session {

    /**
     * Returns the unique session identifier (used as the cookie value).
     *
     * @return the session ID
     */
    String id();

    /**
     * Returns the value for the given key, cast to the specified type.
     *
     * @param key  the attribute key
     * @param type the expected type
     * @param <T>  the attribute type
     * @return the value, or empty if absent or not assignable to {@code type}
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Sets an attribute on this session.
     *
     * @param key   the attribute key
     * @param value the attribute value (must not be null)
     */
    void set(String key, Object value);

    /**
     * Removes an attribute from this session.
     *
     * @param key the attribute key
     */
    void remove(String key);

    /**
     * Marks this session for invalidation. The session is deleted from the store and the session
     * cookie is cleared at the end of the current request.
     */
    void invalidate();

    /**
     * Regenerates this session's ID, keeping its attributes but replacing its identity. The old
     * ID is deleted from the store and a new session cookie is issued at the end of the current
     * request.
     * <p>
     * Call this immediately after authenticating a user (i.e. right after storing
     * {@link SessionMiddleware#PRINCIPAL_KEY}) to defend against session fixation — an attacker
     * who planted a session ID in the victim's browser before login cannot hijack the
     * authenticated session afterward, since the ID changes on login and the planted ID is no
     * longer valid.
     *
     * @return the new session ID
     */
    String regenerateId();

    /**
     * Returns whether {@link #invalidate()} has been called on this session.
     *
     * @return {@code true} if the session is invalidated
     */
    boolean isInvalidated();

    /**
     * Returns whether this session was created during the current request (as opposed to being
     * loaded from the store).
     *
     * @return {@code true} if the session is new
     */
    boolean isNew();

    /**
     * Returns an unmodifiable snapshot of all attributes on this session.
     *
     * @return an unmodifiable attribute map
     */
    Map<String, Object> attributes();
}
