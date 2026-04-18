/*-
 * #%L
 * Serve Example
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
package build.serve.example.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Thread-safe in-memory task store.
 * <p>
 * Notifies registered listeners on every mutation so downstream components
 * (e.g. {@link build.serve.example.ws.TaskBroadcaster}) can push updates.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TaskService {

    /**
     * A mutation event carrying the event type and affected task.
     */
    public record TaskEvent(String type, Task task) {
    }

    private final ConcurrentHashMap<Long, Task> tasks = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);
    private final CopyOnWriteArrayList<Consumer<TaskEvent>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Returns all tasks in insertion order.
     */
    public List<Task> list() {
        return new ArrayList<>(tasks.values());
    }

    /**
     * Finds a task by ID.
     */
    public Optional<Task> findById(final long id) {
        return Optional.ofNullable(tasks.get(id));
    }

    /**
     * Creates a new task and notifies listeners.
     */
    public Task create(final String title) {
        final var task = new Task(nextId.getAndIncrement(), title, false);
        tasks.put(task.id(), task);
        publish(new TaskEvent("created", task));
        return task;
    }

    /**
     * Toggles the {@code done} flag on a task and notifies listeners.
     *
     * @return the updated task, or empty if the ID was not found
     */
    public Optional<Task> toggle(final long id) {
        final var existing = tasks.get(id);
        if (existing == null) {
            return Optional.empty();
        }
        final var updated = new Task(existing.id(), existing.title(), !existing.done());
        tasks.put(id, updated);
        publish(new TaskEvent("updated", updated));
        return Optional.of(updated);
    }

    /**
     * Deletes a task and notifies listeners.
     *
     * @return {@code true} if the task existed
     */
    public boolean delete(final long id) {
        final var removed = tasks.remove(id);
        if (removed != null) {
            publish(new TaskEvent("deleted", removed));
            return true;
        }
        return false;
    }

    /**
     * Registers a listener that is called on every mutation.
     */
    public void onEvent(final Consumer<TaskEvent> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a previously registered listener.
     */
    public void removeListener(final Consumer<TaskEvent> listener) {
        listeners.remove(listener);
    }

    private void publish(final TaskEvent event) {
        for (final var listener : listeners) {
            listener.accept(event);
        }
    }
}
