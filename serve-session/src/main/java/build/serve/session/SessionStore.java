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
 * A pluggable backend for storing and retrieving {@link Session} data.
 * <p>
 * The default implementation is {@link InMemorySessionStore}. For multi-instance deployments,
 * provide a store backed by Redis, a database, or another shared medium.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public interface SessionStore {

    /**
     * Loads the session with the given ID, or returns empty if it does not exist or has expired.
     *
     * @param sessionId the session ID
     * @return the session, or empty if not found
     */
    Optional<Session> load(String sessionId);

    /**
     * Persists the session, extending its TTL.
     *
     * @param session the session to save
     */
    void save(Session session);

    /**
     * Deletes the session with the given ID. A no-op if the session does not exist.
     *
     * @param sessionId the session ID
     */
    void delete(String sessionId);
}
