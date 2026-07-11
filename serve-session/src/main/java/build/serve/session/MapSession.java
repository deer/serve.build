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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ConcurrentHashMap-backed {@link Session} implementation.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
final class MapSession implements Session {

    private volatile String id;
    private final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
    private final boolean isNew;
    private volatile boolean invalidated = false;

    MapSession(final String id, final boolean isNew) {
        this.id = Objects.requireNonNull(id, "id");
        this.isNew = isNew;
    }

    MapSession(final String id, final Map<String, Object> attributes) {
        this.id = Objects.requireNonNull(id, "id");
        this.attributes.putAll(attributes);
        this.isNew = false;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(final String key, final Class<T> type) {
        final var value = attributes.get(key);
        if (value == null || !type.isInstance(value)) {
            return Optional.empty();
        }
        return Optional.of((T) value);
    }

    @Override
    public void set(final String key, final Object value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        attributes.put(key, value);
    }

    @Override
    public void remove(final String key) {
        attributes.remove(key);
    }

    @Override
    public void invalidate() {
        invalidated = true;
    }

    @Override
    public String regenerateId() {
        final var newId = UUID.randomUUID().toString();
        id = newId;
        return newId;
    }

    @Override
    public boolean isInvalidated() {
        return invalidated;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @Override
    public Map<String, Object> attributes() {
        return Collections.unmodifiableMap(attributes);
    }
}
