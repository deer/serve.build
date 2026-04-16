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
package build.serve.example;

import build.serve.example.api.TaskApiHandler;
import build.serve.example.domain.Task;
import build.serve.example.domain.TaskService;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import build.serve.transport.json.JsonMiddleware;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskApiTests {

    private static TestServer server() {
        final var service = new TaskService();
        final var api = new TaskApiHandler(service);
        return TestServer.of(RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .route("/api", api.router())
            .build());
    }

    @Test
    void shouldReturnEmptyListInitially() {
        try (final var s = server()) {
            s.get("/api/tasks")
                .send()
                .assertStatus(200)
                .assertBody("[]");
        }
    }

    @Test
    void shouldCreateTask() {
        try (final var s = server()) {
            final var response = s.post("/api/tasks")
                .jsonBody(new CreateRequest("Buy milk"))
                .send()
                .assertStatus(201);

            final var task = response.bodyAs(Task.class);
            assertThat(task.title()).isEqualTo("Buy milk");
            assertThat(task.done()).isFalse();
            assertThat(task.id()).isGreaterThan(0);
        }
    }

    @Test
    void shouldListCreatedTasks() {
        try (final var s = server()) {
            s.post("/api/tasks").jsonBody(new CreateRequest("Task A")).send().assertStatus(201);
            s.post("/api/tasks").jsonBody(new CreateRequest("Task B")).send().assertStatus(201);

            final var response = s.get("/api/tasks").send().assertStatus(200);
            final var tasks = response.bodyAs(Task[].class);
            assertThat(tasks).hasSize(2);
            assertThat(tasks).extracting(Task::title).contains("Task A", "Task B");
        }
    }

    @Test
    void shouldToggleTask() {
        try (final var s = server()) {
            final var created = s.post("/api/tasks")
                .jsonBody(new CreateRequest("Toggle me"))
                .send()
                .bodyAs(Task.class);

            final var toggled = s.put("/api/tasks/" + created.id())
                .send()
                .assertStatus(200)
                .bodyAs(Task.class);

            assertThat(toggled.done()).isTrue();
            assertThat(toggled.id()).isEqualTo(created.id());
        }
    }

    @Test
    void shouldDeleteTask() {
        try (final var s = server()) {
            final var created = s.post("/api/tasks")
                .jsonBody(new CreateRequest("Delete me"))
                .send()
                .bodyAs(Task.class);

            s.delete("/api/tasks/" + created.id())
                .send()
                .assertStatus(204);

            s.get("/api/tasks")
                .send()
                .assertStatus(200)
                .assertBody("[]");
        }
    }

    @Test
    void shouldReturn404ForMissingTask() {
        try (final var s = server()) {
            s.put("/api/tasks/999").send().assertStatus(404);
            s.delete("/api/tasks/999").send().assertStatus(404);
        }
    }

    private record CreateRequest(String title) {}
}
