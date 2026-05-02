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

import build.base.json.JsonObject;
import build.serve.example.api.TaskApiHandler;
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
                .jsonBody(JsonObject.builder().put("title", "Buy milk").build())
                .send()
                .assertStatus(201);

            final var task = response.bodyAsJson().asObject();
            assertThat(task.getString("title")).isEqualTo("Buy milk");
            assertThat(task.get("done").asBoolean().value()).isFalse();
            assertThat(task.get("id").asNumber().toNumber().longValue()).isGreaterThan(0);
        }
    }

    @Test
    void shouldListCreatedTasks() {
        try (final var s = server()) {
            s.post("/api/tasks").jsonBody(JsonObject.builder().put("title", "Task A").build()).send().assertStatus(201);
            s.post("/api/tasks").jsonBody(JsonObject.builder().put("title", "Task B").build()).send().assertStatus(201);

            final var tasks = s.get("/api/tasks").send().assertStatus(200).bodyAsJson().asArray().values();
            assertThat(tasks).hasSize(2);
            final var titles = tasks.stream().map(t -> t.asObject().getString("title")).toList();
            assertThat(titles).containsExactlyInAnyOrder("Task A", "Task B");
        }
    }

    @Test
    void shouldToggleTask() {
        try (final var s = server()) {
            final var created = s.post("/api/tasks")
                .jsonBody(JsonObject.builder().put("title", "Toggle me").build())
                .send()
                .bodyAsJson().asObject();

            final var toggled = s.put("/api/tasks/" + created.get("id").asNumber().toNumber().longValue())
                .send()
                .assertStatus(200)
                .bodyAsJson().asObject();

            assertThat(toggled.get("done").asBoolean().value()).isTrue();
            assertThat(toggled.get("id").asNumber().toNumber().longValue())
                .isEqualTo(created.get("id").asNumber().toNumber().longValue());
        }
    }

    @Test
    void shouldDeleteTask() {
        try (final var s = server()) {
            final var created = s.post("/api/tasks")
                .jsonBody(JsonObject.builder().put("title", "Delete me").build())
                .send()
                .bodyAsJson().asObject();

            s.delete("/api/tasks/" + created.get("id").asNumber().toNumber().longValue())
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
}
