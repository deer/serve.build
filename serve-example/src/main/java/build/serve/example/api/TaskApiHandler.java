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
package build.serve.example.api;

import build.serve.example.domain.TaskService;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;

/**
 * JSON REST handler for tasks.
 * <p>
 * Mounted at {@code /api} by {@link build.serve.example.ExampleApp}. All responses are
 * serialized to JSON by the {@code JsonMiddleware} registered on the parent router.
 * Routes: {@code GET /api/tasks} (list), {@code POST /api/tasks} (create, body {@code {"title":"…"}}),
 * {@code PUT /api/tasks/{id}} (toggle done), {@code DELETE /api/tasks/{id}} (delete).
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TaskApiHandler {

    private record CreateRequest(String title) {
    }

    private final TaskService service;

    /**
     * Constructs a {@link TaskApiHandler} backed by the given service.
     */
    public TaskApiHandler(final TaskService service) {
        this.service = service;
    }

    /**
     * Builds and returns the {@link Router} for all task API routes.
     */
    public Router router() {
        return RouterBuilder.create()
            .get("/tasks", exchange -> {
                exchange.sendBody(service.list());
            })
            .post("/tasks", exchange -> {
                final var req = exchange.bodyAs(CreateRequest.class);
                final var task = service.create(req.title());
                exchange.response().status(201);
                exchange.sendBody(task);
            })
            .put("/tasks/{id}", exchange -> {
                final var id = Long.parseLong(exchange.pathParam("id").orElseThrow());
                final var updated = service.toggle(id);
                if (updated.isPresent()) {
                    exchange.sendBody(updated.get());
                } else {
                    exchange.response().status(404).send("{\"error\":\"not found\"}");
                }
            })
            .delete("/tasks/{id}", exchange -> {
                final var id = Long.parseLong(exchange.pathParam("id").orElseThrow());
                if (service.delete(id)) {
                    exchange.response().status(204).send(new byte[0]);
                } else {
                    exchange.response().status(404).send("{\"error\":\"not found\"}");
                }
            })
            .build();
    }
}
