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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * An in-process {@link SessionStore} backed by a {@link ConcurrentHashMap} with TTL-based expiry.
 * <p>
 * Sessions are evicted lazily on access and proactively by a background cleanup thread that runs
 * every ten minutes. This store is suitable for single-instance deployments only — sessions are not
 * shared across JVM instances.
 * <pre>{@code
 * var store = InMemorySessionStore.create();           // 30-minute TTL
 * var store = InMemorySessionStore.withTtl(Duration.ofHours(1));
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class InMemorySessionStore implements SessionStore {

    private final Duration ttl;
    private final int maxSessions;
    private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleaner;

    private record Entry(Map<String, Object> attributes, Instant expiresAt) {
    }

    private InMemorySessionStore(final Duration ttl, final int maxSessions) {
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.maxSessions = maxSessions;
        this.cleaner = Executors.newSingleThreadScheduledExecutor(r -> {
            final var thread = new Thread(r, "session-cleaner");
            thread.setDaemon(true);
            return thread;
        });
        cleaner.scheduleAtFixedRate(this::evictExpired, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * Creates a store with the default 30-minute session TTL and a 10,000-session cap.
     *
     * @return a new {@link InMemorySessionStore}
     */
    public static InMemorySessionStore create() {
        return new InMemorySessionStore(Duration.ofMinutes(30), 10_000);
    }

    /**
     * Creates a store with a custom session TTL and a 10,000-session cap.
     *
     * @param ttl the time-to-live for inactive sessions
     * @return a new {@link InMemorySessionStore}
     */
    public static InMemorySessionStore withTtl(final Duration ttl) {
        return new InMemorySessionStore(ttl, 10_000);
    }

    /**
     * Creates a store with a custom session TTL and a custom session cap.
     * <p>
     * When the cap is reached, {@link #save(Session)} silently discards new sessions until
     * existing ones expire and are evicted.
     *
     * @param ttl         the time-to-live for inactive sessions
     * @param maxSessions the maximum number of sessions held simultaneously
     * @return a new {@link InMemorySessionStore}
     */
    public static InMemorySessionStore withTtlAndMaxSessions(final Duration ttl, final int maxSessions) {
        if (maxSessions <= 0) {
            throw new IllegalArgumentException("maxSessions must be positive");
        }
        return new InMemorySessionStore(ttl, maxSessions);
    }

    @Override
    public Optional<Session> load(final String sessionId) {
        final var entry = sessions.get(sessionId);
        if (entry == null) {
            return Optional.empty();
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            sessions.remove(sessionId);
            return Optional.empty();
        }
        return Optional.of(new MapSession(sessionId, entry.attributes()));
    }

    @Override
    public void save(final Session session) {
        if (!sessions.containsKey(session.id()) && sessions.size() >= maxSessions) {
            return;
        }
        sessions.put(session.id(), new Entry(
            new HashMap<>(session.attributes()),
            Instant.now().plus(ttl)
        ));
    }

    @Override
    public void delete(final String sessionId) {
        sessions.remove(sessionId);
    }

    private void evictExpired() {
        final var now = Instant.now();
        sessions.entrySet().removeIf(e -> now.isAfter(e.getValue().expiresAt()));
    }
}
