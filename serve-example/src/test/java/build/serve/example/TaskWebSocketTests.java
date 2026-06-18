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
import build.serve.example.ws.TaskBroadcaster;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import build.serve.transport.json.JsonMiddleware;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWebSocketTests {

    @Test
    void shouldBroadcastCreatedEventToConnectedClients() throws Exception {
        final var service = new TaskService();
        final var broadcaster = new TaskBroadcaster(service);
        final var api = new TaskApiHandler(service);

        final var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .route("/api", api.router())
            .get("/ws/tasks", broadcaster.handler())
            .build();

        try (final var server = TestServer.of(router);
             final var ws = server.connectWebSocket("/ws/tasks")) {

            server.post("/api/tasks")
                .jsonBody(JsonObject.builder().put("title", "WebSocket test").build())
                .send()
                .assertStatus(201);

            final var message = ws.nextText();
            assertThat(message).contains("\"event\":\"created\"");
            assertThat(message).contains("\"title\":\"WebSocket test\"");
        }
    }

    @Test
    void shouldBroadcastDeletedEvent() throws Exception {
        final var service = new TaskService();
        final var broadcaster = new TaskBroadcaster(service);
        final var api = new TaskApiHandler(service);

        final var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .route("/api", api.router())
            .get("/ws/tasks", broadcaster.handler())
            .build();

        try (final var server = TestServer.of(router)) {
            final var created = server.post("/api/tasks")
                .jsonBody(JsonObject.builder().put("title", "To delete").build())
                .send()
                .bodyAsJson().asObject();

            try (final var ws = server.connectWebSocket("/ws/tasks")) {
                server.delete("/api/tasks/" + created.get("id").asNumber().toNumber().longValue())
                    .send()
                    .assertStatus(204);

                final var message = ws.nextText();
                assertThat(message).contains("\"event\":\"deleted\"");
            }
        }
    }

    @Test
    void shouldBroadcastUpdatedEventOnToggle() throws Exception {
        final var service = new TaskService();
        final var broadcaster = new TaskBroadcaster(service);
        final var api = new TaskApiHandler(service);

        final var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .route("/api", api.router())
            .get("/ws/tasks", broadcaster.handler())
            .build();

        try (final var server = TestServer.of(router)) {
            final var created = server.post("/api/tasks")
                .jsonBody(JsonObject.builder().put("title", "Toggle me").build())
                .send()
                .bodyAsJson().asObject();

            try (final var ws = server.connectWebSocket("/ws/tasks")) {
                server.put("/api/tasks/" + created.get("id").asNumber().toNumber().longValue())
                    .send()
                    .assertStatus(200);

                final var message = ws.nextText();
                assertThat(message).contains("\"event\":\"updated\"");
                assertThat(message).contains("\"done\":true");
            }
        }
    }
}
