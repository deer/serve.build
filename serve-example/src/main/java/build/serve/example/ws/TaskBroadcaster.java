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
package build.serve.example.ws;

import build.base.json.JsonObject;
import build.serve.example.domain.Task;
import build.serve.example.domain.TaskService;
import build.serve.foundation.Handler;
import build.serve.websocket.WebSocket;
import build.serve.websocket.WebSocketUpgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Maintains a set of live WebSocket sessions and broadcasts {@link TaskService} mutation
 * events to all connected clients as JSON text frames.
 * <p>
 * Each browser tab that opens the page connects to {@code /ws/tasks} and receives events like:
 * <pre>{"event":"created","task":{"id":1,"title":"Buy milk","done":false}}</pre>
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TaskBroadcaster {

    private final CopyOnWriteArrayList<WebSocket> sessions = new CopyOnWriteArrayList<>();

    /**
     * Constructs a {@link TaskBroadcaster} that listens for events from the given service.
     */
    public TaskBroadcaster(final TaskService service) {
        service.onEvent(event -> {
            if (sessions.isEmpty()) {
                return;
            }
            final var json = JsonObject.builder()
                .put("event", event.type())
                .put("task", taskToJson(event.task()))
                .build()
                .toJsonString();
            final List<WebSocket> dead = new ArrayList<>();
            for (final var ws : sessions) {
                if (!ws.isOpen()) {
                    dead.add(ws);
                } else {
                    try {
                        ws.sendText(json);
                    } catch (final Exception ignored) {
                        dead.add(ws);
                    }
                }
            }
            sessions.removeAll(dead);
        });
    }

    /**
     * Returns a {@link Handler} that accepts WebSocket upgrade requests and registers
     * the connection for broadcasts.
     */
    public Handler handler() {
        return WebSocketUpgrade.upgrade(ws -> {
            sessions.add(ws);
            ws.onClose(() -> sessions.remove(ws));
        });
    }

    private static JsonObject taskToJson(final Task task) {
        return JsonObject.builder()
            .put("id", task.id())
            .put("title", task.title())
            .put("done", task.done())
            .build();
    }
}
