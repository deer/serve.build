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
import build.serve.auth.ApiKeyStrategy;
import build.serve.auth.AuthMiddleware;
import build.serve.auth.SimplePrincipal;
import build.serve.compression.CompressionMiddleware;
import build.serve.cors.CorsMiddleware;
import build.serve.devtools.ChromeDevToolsHandler;
import build.serve.devtools.DevErrorHandler;
import build.serve.devtools.Editor;
import build.serve.devtools.FaviconHandler;
import build.serve.devtools.LiveReload;
import build.serve.devtools.ServerTimingMiddleware;
import build.serve.example.api.TaskApiHandler;
import build.serve.example.domain.TaskService;
import build.serve.example.graphql.TaskGraphQlHandler;
import build.serve.example.web.TaskWebHandler;
import build.serve.example.ws.TaskBroadcaster;
import build.serve.foundation.error.ErrorHandler;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.health.HealthCheck;
import build.serve.health.HealthHandler;
import build.serve.logging.RequestLoggingMiddleware;
import build.serve.ratelimit.RateLimitMiddleware;
import build.serve.security.SecurityHeadersMiddleware;
import build.serve.session.InMemorySessionStore;
import build.serve.session.SessionMiddleware;
import build.serve.transport.json.JsonMiddleware;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

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
 *   <li>API key authentication and rate limiting on {@code /api} and {@code /graphql}</li>
 *   <li>Cookie-based sessions on the browser UI</li>
 *   <li>Dev tooling: Chrome DevTools workspace, favicon shim, {@code Server-Timing} header,
 *       pretty HTML error page, and live-reload that pushes a refresh on every recompile</li>
 * </ul>
 * <p>
 * Run with {@code ./mvnw -pl serve-example exec:java -Dexec.mainClass=build.serve.example.ExampleApp}
 * or {@code java --enable-preview -m build.serve.example/build.serve.example.ExampleApp}.
 * Pass {@code -Dserve.devtools.editor=vscode|intellij|zed|sublime} to control which editor
 * the dev error page links into; defaults to IntelliJ.
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

    private static final Set<String> VALID_API_KEYS = Set.of("demo-key-1", "demo-key-2");

    @Override
    protected Router configure() {
        final var service = new TaskService();
        final var broadcaster = new TaskBroadcaster(service);
        final var api = new TaskApiHandler(service);
        final var graphql = new TaskGraphQlHandler(service);
        final var web = new TaskWebHandler(service);

        final var apiAuth = AuthMiddleware.builder()
            .strategy(ApiKeyStrategy.fromHeader("X-Api-Key",
                key -> VALID_API_KEYS.contains(key)
                    ? Optional.of(SimplePrincipal.of(key))
                    : Optional.empty()))
            .build();

        final var apiRateLimit = RateLimitMiddleware.builder()
            .limit(60)
            .per(Duration.ofMinutes(1))
            .build();

        final var sessionMiddleware = SessionMiddleware.builder()
            .store(InMemorySessionStore.create())
            .build();

        // Live-reload: cwd is the module dir under exec:java, so this resolves to
        // serve-example/target/classes — rebuild and connected browser tabs reload.
        final var liveReload = LiveReload.watching(Path.of("target/classes"));

        return RouterBuilder.create()
            // Middleware stack — first registered = outermost.
            // ServerTimingMiddleware sits at the top so its `total` covers everything else
            // we control (logging, CORS, security, compression, JSON, route handling).
            .middleware(ServerTimingMiddleware.create())
            .middleware(RequestLoggingMiddleware.defaults())
            .middleware(CorsMiddleware.allowAll())
            .middleware(SecurityHeadersMiddleware.defaults())
            .middleware(CompressionMiddleware.defaults())
            .middleware(new JsonMiddleware())
            // API routes — rate-limited and API-key protected
            .group(g -> g
                .middleware(apiRateLimit)
                .middleware(apiAuth)
                .route("/api", api.router())
                .post("/graphql", graphql.graphqlHandler())
                .get("/graphiql", graphql.graphiqlHandler()))
            // WebSocket and health need no auth
            .route("/ws/tasks", broadcaster.handler())
            .route("/health", HealthHandler.create()
                .liveness("/live")
                .readiness("/ready", HealthCheck.of("tasks", () -> true))
                .build())
            // Dev tooling — Chrome DevTools workspace, favicon shim, live-reload WebSocket
            .get(ChromeDevToolsHandler.PATH, ChromeDevToolsHandler.forWorkspace(Path.of(".")))
            .get("/favicon.ico", FaviconHandler.empty())
            .route(LiveReload.PATH, liveReload.handler())
            // Browser UI — session-backed
            .group(g -> g
                .middleware(sessionMiddleware)
                .route("/", web.router()))
            .build();
    }

    @Override
    protected ErrorHandler errorHandler() {
        // Dev-mode pretty error page with "open in editor" links. cwd is the example module
        // under exec:java, so the repo root is one directory up.
        // Override the editor at launch with -Dserve.devtools.editor=vscode|intellij|zed|sublime.
        final var editor = Editor.fromSystemProperty("serve.devtools.editor").orElse(Editor.INTELLIJ);
        return DevErrorHandler.builder()
            .editor(editor)
            .discoverSourceRoots(Path.of(".."))
            .build();
    }
}
