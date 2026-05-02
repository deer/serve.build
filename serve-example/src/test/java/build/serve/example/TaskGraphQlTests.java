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

import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonNull;
import build.serve.example.domain.TaskService;
import build.serve.example.graphql.TaskGraphQlHandler;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskGraphQlTests {

    private static TestServer server() {
        final var service = new TaskService();
        final var graphql = new TaskGraphQlHandler(service);
        return TestServer.of(RouterBuilder.create()
            .post("/graphql", graphql.graphqlHandler())
            .build());
    }

    @Test
    void shouldQueryEmptyTaskList() throws Exception {
        try (final var s = server()) {
            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ tasks { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            assertThat(json.get("data").asObject().get("tasks")).isInstanceOf(JsonArray.class);
            assertThat(((JsonArray) json.get("data").asObject().get("tasks")).values()).isEmpty();
            assertThat(json.has("errors")).isFalse();
        }
    }

    @Test
    void shouldCreateTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Buy milk\\\") { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            final var task = json.get("data").asObject().get("createTask").asObject();
            assertThat(task.getString("title")).isEqualTo("Buy milk");
            assertThat(task.get("done").asBoolean().value()).isFalse();
            assertThat(Long.parseLong(task.getString("id"))).isGreaterThan(0);
            assertThat(json.has("errors")).isFalse();
        }
    }

    @Test
    void shouldQueryTaskById() throws Exception {
        try (final var s = server()) {
            final var created = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Find me\\\") { id } }\"}")
                    .send()
                    .body()).asObject();

            final var id = created.get("data").asObject().get("createTask").asObject().getString("id");

            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ task(id: \\\"" + id + "\\\") { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            assertThat(json.get("data").asObject().get("task").asObject().getString("title")).isEqualTo("Find me");
        }
    }

    @Test
    void shouldToggleTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var created = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Toggle me\\\") { id } }\"}")
                    .send()
                    .body()).asObject();

            final var id = created.get("data").asObject().get("createTask").asObject().getString("id");

            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { toggleTask(id: \\\"" + id + "\\\") { id done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            assertThat(json.get("data").asObject().get("toggleTask").asObject().get("done").asBoolean().value()).isTrue();
            assertThat(json.has("errors")).isFalse();
        }
    }

    @Test
    void shouldDeleteTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var created = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Delete me\\\") { id } }\"}")
                    .send()
                    .body()).asObject();

            final var id = created.get("data").asObject().get("createTask").asObject().getString("id");

            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { deleteTask(id: \\\"" + id + "\\\") }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            assertThat(json.get("data").asObject().get("deleteTask").asBoolean().value()).isTrue();
            assertThat(json.has("errors")).isFalse();
        }
    }

    @Test
    void shouldReturnNullForMissingTask() throws Exception {
        try (final var s = server()) {
            final var json = Json.parse(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ task(id: \\\"999\\\") { id title } }\"}")
                    .send()
                    .assertStatus(200)
                    .body()).asObject();

            assertThat(json.get("data").asObject().get("task")).isInstanceOf(JsonNull.class);
            assertThat(json.has("errors")).isFalse();
        }
    }
}
