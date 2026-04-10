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

import build.serve.example.domain.TaskService;
import build.serve.example.graphql.TaskGraphQlHandler;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskGraphQlTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

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
            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ tasks { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            assertThat(json.path("data").path("tasks").isArray()).isTrue();
            assertThat(json.path("data").path("tasks")).isEmpty();
            assertThat(json.path("errors").isMissingNode()).isTrue();
        }
    }

    @Test
    void shouldCreateTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Buy milk\\\") { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            final var task = json.path("data").path("createTask");
            assertThat(task.path("title").asText()).isEqualTo("Buy milk");
            assertThat(task.path("done").asBoolean()).isFalse();
            assertThat(task.path("id").asLong()).isGreaterThan(0);
            assertThat(json.path("errors").isMissingNode()).isTrue();
        }
    }

    @Test
    void shouldQueryTaskById() throws Exception {
        try (final var s = server()) {
            // Create first
            final var created = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Find me\\\") { id } }\"}")
                    .send()
                    .body());

            final var id = created.path("data").path("createTask").path("id").asText();

            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ task(id: \\\"" + id + "\\\") { id title done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            assertThat(json.path("data").path("task").path("title").asText()).isEqualTo("Find me");
        }
    }

    @Test
    void shouldToggleTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var created = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Toggle me\\\") { id } }\"}")
                    .send()
                    .body());

            final var id = created.path("data").path("createTask").path("id").asText();

            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { toggleTask(id: \\\"" + id + "\\\") { id done } }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            assertThat(json.path("data").path("toggleTask").path("done").asBoolean()).isTrue();
            assertThat(json.path("errors").isMissingNode()).isTrue();
        }
    }

    @Test
    void shouldDeleteTaskViaMutation() throws Exception {
        try (final var s = server()) {
            final var created = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { createTask(title: \\\"Delete me\\\") { id } }\"}")
                    .send()
                    .body());

            final var id = created.path("data").path("createTask").path("id").asText();

            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"mutation { deleteTask(id: \\\"" + id + "\\\") }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            assertThat(json.path("data").path("deleteTask").asBoolean()).isTrue();
            assertThat(json.path("errors").isMissingNode()).isTrue();
        }
    }

    @Test
    void shouldReturnNullForMissingTask() throws Exception {
        try (final var s = server()) {
            final var json = MAPPER.readTree(
                s.post("/graphql")
                    .header("Content-Type", "application/json")
                    .body("{\"query\":\"{ task(id: \\\"999\\\") { id title } }\"}")
                    .send()
                    .assertStatus(200)
                    .body());

            assertThat(json.path("data").path("task").isNull()).isTrue();
            assertThat(json.path("errors").isMissingNode()).isTrue();
        }
    }
}
