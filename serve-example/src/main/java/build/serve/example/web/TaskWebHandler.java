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
package build.serve.example.web;

import build.serve.example.domain.Task;
import build.serve.example.domain.TaskService;
import build.serve.foundation.Exchange;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.jte.JteTemplateEngine;
import build.serve.template.TemplateHandler;
import build.serve.template.TemplateOutput;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * JTE + HTMX handler for the browser-facing task UI.
 * <p>
 * Mounts at {@code /} by {@link build.serve.example.ExampleApp}. The full page at {@code GET /}
 * renders {@code tasks.jte}. HTMX mutation endpoints return the {@code taskItem.jte} partial so
 * the browser can swap just the affected list item without a full reload.
 * <p>
 * Templates in {@code src/main/jte/} are precompiled by the JTE Maven plugin at build time.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TaskWebHandler {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML = "text/html; charset=utf-8";

    private final TaskService service;
    private final JteTemplateEngine engine;

    /**
     * Constructs a {@link TaskWebHandler}.
     * <p>
     * Templates are loaded from precompiled classes generated at build time by the JTE Maven plugin.
     */
    public TaskWebHandler(final TaskService service) {
        this.service = service;
        this.engine = JteTemplateEngine.production(TaskWebHandler.class);
    }

    /**
     * Builds and returns the {@link Router} for all web UI routes.
     */
    public Router router() {
        return RouterBuilder.create()
            .get("/", TemplateHandler.render(engine, "tasks.jte",
                exchange -> Map.of("tasks", service.list())))
            .post("/tasks", exchange -> {
                final var title = parseField(exchange.request().bodyAsString(), "title");
                final var task = service.create(title);
                renderItem(exchange, task);
            })
            .put("/tasks/{id}/toggle", exchange -> {
                final var id = Long.parseLong(exchange.pathParam("id").orElseThrow());
                final var updated = service.toggle(id);
                if (updated.isPresent()) {
                    renderItem(exchange, updated.get());
                } else {
                    exchange.response().status(404).send("");
                }
            })
            .delete("/tasks/{id}", exchange -> {
                final var id = Long.parseLong(exchange.pathParam("id").orElseThrow());
                service.delete(id);
                exchange.response().status(200).send("");
            })
            .build();
    }

    private void renderItem(final Exchange exchange, final Task task) throws Exception {
        final var output = TemplateOutput.capture();
        engine.template("taskItem.jte").render(output, Map.of("task", task));
        exchange.response().header(CONTENT_TYPE, TEXT_HTML).send(output.get());
    }

    /**
     * Parses a single field from a {@code application/x-www-form-urlencoded} body.
     */
    private static String parseField(final String body, final String field) {
        for (final var pair : body.split("&")) {
            final var parts = pair.split("=", 2);
            if (parts.length == 2) {
                final var key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                if (key.equals(field)) {
                    return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }
}
