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
import build.serve.example.domain.TaskService;
import build.serve.example.ws.TaskBroadcaster;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import build.serve.transport.json.JsonMiddleware;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class TaskWebSocketTest {

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

        try (final var server = TestServer.of(router)) {
            final var received = new CompletableFuture<String>();

            HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                    URI.create("ws://127.0.0.1:" + server.port() + "/ws/tasks"),
                    new WebSocket.Listener() {
                        @Override
                        public void onOpen(final WebSocket ws) {
                            ws.request(1);
                        }

                        @Override
                        public java.util.concurrent.CompletionStage<?> onText(
                                final WebSocket ws,
                                final CharSequence data,
                                final boolean last) {
                            received.complete(data.toString());
                            return null;
                        }
                    })
                .get(5, TimeUnit.SECONDS);

            // Give the WebSocket handshake time to complete
            Thread.sleep(100);

            server.post("/api/tasks")
                .jsonBody(new CreateRequest("WebSocket test"))
                .send()
                .assertStatus(201);

            final var message = received.get(5, TimeUnit.SECONDS);
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
            // Create a task first
            final var created = server.post("/api/tasks")
                .jsonBody(new CreateRequest("To delete"))
                .send()
                .bodyAs(build.serve.example.domain.Task.class);

            final var received = new CompletableFuture<String>();

            HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                    URI.create("ws://127.0.0.1:" + server.port() + "/ws/tasks"),
                    new WebSocket.Listener() {
                        @Override
                        public void onOpen(final WebSocket ws) {
                            ws.request(1);
                        }

                        @Override
                        public java.util.concurrent.CompletionStage<?> onText(
                                final WebSocket ws,
                                final CharSequence data,
                                final boolean last) {
                            received.complete(data.toString());
                            return null;
                        }
                    })
                .get(5, TimeUnit.SECONDS);

            Thread.sleep(100);

            server.delete("/api/tasks/" + created.id())
                .send()
                .assertStatus(204);

            final var message = received.get(5, TimeUnit.SECONDS);
            assertThat(message).contains("\"event\":\"deleted\"");
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
                .jsonBody(new CreateRequest("Toggle me"))
                .send()
                .bodyAs(build.serve.example.domain.Task.class);

            final var received = new CompletableFuture<String>();

            HttpClient.newHttpClient()
                .newWebSocketBuilder()
                .buildAsync(
                    URI.create("ws://127.0.0.1:" + server.port() + "/ws/tasks"),
                    new WebSocket.Listener() {
                        @Override
                        public void onOpen(final WebSocket ws) {
                            ws.request(1);
                        }

                        @Override
                        public java.util.concurrent.CompletionStage<?> onText(
                                final WebSocket ws,
                                final CharSequence data,
                                final boolean last) {
                            received.complete(data.toString());
                            return null;
                        }
                    })
                .get(5, TimeUnit.SECONDS);

            Thread.sleep(100);

            server.put("/api/tasks/" + created.id())
                .send()
                .assertStatus(200);

            final var message = received.get(5, TimeUnit.SECONDS);
            assertThat(message).contains("\"event\":\"updated\"");
            assertThat(message).contains("\"done\":true");
        }
    }

    private record CreateRequest(String title) {}
}
