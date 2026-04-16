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
import build.serve.example.web.TaskWebHandler;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.Test;

class TaskWebTests {

    private static TestServer server() {
        final var service = new TaskService();
        final var web = new TaskWebHandler(service);
        return TestServer.of(web.router());
    }

    @Test
    void shouldRenderFullPage() {
        try (final var s = server()) {
            s.get("/")
                .send()
                .assertStatus(200)
                .assertContentType("text/html")
                .assertBodyContains("Tasks")
                .assertBodyContains("htmx");
        }
    }

    @Test
    void shouldCreateTaskAndReturnPartial() {
        try (final var s = server()) {
            s.post("/tasks")
                .contentType("application/x-www-form-urlencoded")
                .body("title=Write+tests")
                .send()
                .assertStatus(200)
                .assertContentType("text/html")
                .assertBodyContains("Write tests")
                .assertBodyContains("task-item");
        }
    }

    @Test
    void shouldToggleTaskAndReturnPartial() {
        try (final var s = server()) {
            s.post("/tasks")
                .contentType("application/x-www-form-urlencoded")
                .body("title=Toggle+me")
                .send()
                .assertStatus(200);

            // Task ID 1 was just created
            s.put("/tasks/1/toggle")
                .send()
                .assertStatus(200)
                .assertContentType("text/html")
                .assertBodyContains("task-1")
                .assertBodyContains("done");
        }
    }

    @Test
    void shouldDeleteTask() {
        try (final var s = server()) {
            s.post("/tasks")
                .contentType("application/x-www-form-urlencoded")
                .body("title=Delete+me")
                .send()
                .assertStatus(200);

            s.delete("/tasks/1")
                .send()
                .assertStatus(200);
        }
    }
}
