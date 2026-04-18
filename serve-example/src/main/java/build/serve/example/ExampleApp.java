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

import build.base.network.option.Port;
import build.serve.application.Launcher;
import build.serve.application.ServerApplication;
import build.serve.compression.CompressionMiddleware;
import build.serve.cors.CorsMiddleware;
import build.serve.example.api.TaskApiHandler;
import build.serve.example.domain.TaskService;
import build.serve.example.graphql.TaskGraphQlHandler;
import build.serve.example.web.TaskWebHandler;
import build.serve.example.ws.TaskBroadcaster;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.health.HealthCheck;
import build.serve.health.HealthHandler;
import build.serve.logging.RequestLoggingMiddleware;
import build.serve.security.SecurityHeadersMiddleware;
import build.serve.transport.json.JsonMiddleware;

/**
 * Entry point for the serve.build example application.
 * <p>
 * Demonstrates the full serve.build feature set in a single runnable module:
 * <ul>
 *   <li>REST JSON API with {@code JsonMiddleware} and {@code RouterBuilder}</li>
 *   <li>HTMX-driven HTML UI with JTE templates and partial swaps</li>
 *   <li>WebSocket push for live task updates across browser tabs</li>
 *   <li>Full middleware stack: logging, CORS, security headers, gzip compression</li>
 *   <li>Health endpoints at {@code /health/live} and {@code /health/ready}</li>
 * </ul>
 * <p>
 * Run with {@code ./mvnw -pl serve-example exec:java -Dexec.mainClass=build.serve.example.ExampleApp}
 * or {@code java --enable-preview -m build.serve.example/build.serve.example.ExampleApp}
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class ExampleApp extends ServerApplication.Implementation {

    /**
     * Launches the example on port 8080.
     */
    public static void main(final String[] args) {
        Launcher.launch(new ExampleApp(), Port.of(8080));
    }

    @Override
    protected Router configure() {
        final var service = new TaskService();
        final var broadcaster = new TaskBroadcaster(service);
        final var api = new TaskApiHandler(service);
        final var graphql = new TaskGraphQlHandler(service);
        final var web = new TaskWebHandler(service);

        return RouterBuilder.create()
            // Middleware stack — first registered = outermost
            .middleware(RequestLoggingMiddleware.defaults())
            .middleware(CorsMiddleware.allowAll())
            .middleware(SecurityHeadersMiddleware.defaults())
            .middleware(CompressionMiddleware.defaults())
            .middleware(new JsonMiddleware())
            // Routes
            .route("/api", api.router())
            .post("/graphql", graphql.graphqlHandler())
            .get("/graphiql", graphql.graphiqlHandler())
            .route("/ws/tasks", broadcaster.handler())
            .route("/health", HealthHandler.create()
                .liveness("/live")
                .readiness("/ready", HealthCheck.of("tasks", () -> true))
                .build())
            .route("/", web.router())
            .build();
    }
}
