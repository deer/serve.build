/*-
 * #%L
 * Serve DevTools
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
package build.serve.devtools;

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.error.ErrorHandler;
import build.serve.foundation.error.HttpException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A development {@link ErrorHandler} that renders an HTML page showing the thrown exception,
 * its stack trace, the request that triggered it, and (optionally) clickable
 * "open in editor" links next to each stack frame.
 * <p>
 * <strong>Do not use in production.</strong> The page exposes internal file paths, class names,
 * and request headers.
 * <pre>{@code
 * @Override
 * protected ErrorHandler errorHandler() {
 *     return DevErrorHandler.builder()
 *         .editorUriTemplate("vscode://file/{file}:{line}")
 *         .sourceRoot(Path.of("src/main/java"))
 *         .build();
 * }
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class DevErrorHandler implements ErrorHandler {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML = "text/html; charset=utf-8";

    private final String editorUriTemplate;
    private final List<Path> sourceRoots;

    private DevErrorHandler(final Builder builder) {
        this.editorUriTemplate = builder.editorUriTemplate;
        this.sourceRoots = List.copyOf(builder.sourceRoots);
    }

    /**
     * Creates a {@link DevErrorHandler} with no editor integration.
     *
     * @return a new {@link DevErrorHandler}
     */
    public static DevErrorHandler create() {
        return builder().build();
    }

    /**
     * Creates a {@link Builder} for customising the handler.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void handle(final Exchange exchange, final Throwable error) {
        final int statusCode = error instanceof HttpException http ? http.statusCode() : 500;
        final var html = renderPage(exchange.request(), error, statusCode);
        exchange.response()
            .status(statusCode)
            .header(CONTENT_TYPE, TEXT_HTML)
            .send(html);
    }

    private String renderPage(final Request request,
                              final Throwable error,
                              final int statusCode) {
        final var sb = new StringBuilder(4_096);
        sb.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\">")
            .append("<title>").append(statusCode).append(" — ")
            .append(escape(error.getClass().getSimpleName())).append("</title>")
            .append("<style>")
            .append("*,*::before,*::after{box-sizing:border-box}")
            .append("body{font-family:system-ui,sans-serif;margin:0;background:#1e1e1e;color:#e0e0e0}")
            .append(".banner{background:#b71c1c;color:#fff;padding:1.5rem 2rem}")
            .append(".banner h1{margin:0;font-size:1.5rem;font-weight:600}")
            .append(".banner .type{opacity:0.85;font-size:0.9rem;margin-top:0.25rem}")
            .append(".message{background:#2a1d1d;padding:1rem 2rem;border-bottom:1px solid #3a3a3a;")
            .append("font-family:ui-monospace,monospace;white-space:pre-wrap;word-break:break-word}")
            .append("section{padding:1.25rem 2rem;border-bottom:1px solid #3a3a3a}")
            .append("section h2{margin:0 0 0.75rem;font-size:0.8rem;text-transform:uppercase;")
            .append("letter-spacing:0.1em;color:#9e9e9e}")
            .append(".frames{font-family:ui-monospace,monospace;font-size:0.875rem;line-height:1.6}")
            .append(".frame{padding:0.15rem 0}")
            .append(".frame .cls{color:#81d4fa}")
            .append(".frame .mth{color:#e0e0e0}")
            .append(".frame .loc{color:#bdbdbd}")
            .append(".frame a{color:#90caf9;text-decoration:none}")
            .append(".frame a:hover{text-decoration:underline}")
            .append(".kv{font-family:ui-monospace,monospace;font-size:0.875rem}")
            .append(".kv .k{color:#9e9e9e;display:inline-block;min-width:10rem}")
            .append(".kv .v{color:#e0e0e0}")
            .append("</style></head><body>");

        sb.append("<div class=\"banner\"><h1>").append(statusCode).append(" — ")
            .append(escape(error.getClass().getName())).append("</h1>");
        sb.append("<div class=\"type\">")
            .append(escape(request.method())).append(' ').append(escape(request.path()))
            .append("</div></div>");

        if (error.getMessage() != null && !error.getMessage().isBlank()) {
            sb.append("<div class=\"message\">").append(escape(error.getMessage())).append("</div>");
        }

        sb.append("<section><h2>Stack trace</h2><div class=\"frames\">");
        renderThrowable(error, sb);
        sb.append("</div></section>");

        sb.append("<section><h2>Request</h2><div class=\"kv\">");
        kv(sb, "Method", request.method());
        kv(sb, "Path", request.path());
        kv(sb, "URI", request.uri().toString());
        sb.append("</div></section>");

        if (!request.headers().isEmpty()) {
            sb.append("<section><h2>Headers</h2><div class=\"kv\">");
            request.headers().forEach((name, values) -> kv(sb, name, String.join(", ", values)));
            sb.append("</div></section>");
        }

        sb.append("</body></html>");
        return sb.toString();
    }

    private void renderThrowable(final Throwable error, final StringBuilder sb) {
        Throwable current = error;
        boolean first = true;
        while (current != null) {
            if (!first) {
                sb.append("<div class=\"frame\" style=\"margin-top:0.75rem;color:#bdbdbd\">")
                    .append("Caused by: ").append(escape(current.getClass().getName()));
                if (current.getMessage() != null) {
                    sb.append(": ").append(escape(current.getMessage()));
                }
                sb.append("</div>");
            }
            for (final var frame : current.getStackTrace()) {
                renderFrame(frame, sb);
            }
            current = current.getCause();
            first = false;
        }
    }

    private void renderFrame(final StackTraceElement frame, final StringBuilder sb) {
        sb.append("<div class=\"frame\"><span class=\"cls\">")
            .append(escape(frame.getClassName()))
            .append("</span>.<span class=\"mth\">")
            .append(escape(frame.getMethodName()))
            .append("</span>");

        final var file = frame.getFileName();
        if (file != null) {
            final var line = frame.getLineNumber();
            final var loc = file + (line > 0 ? ":" + line : "");
            final var link = editorLink(frame);
            sb.append(" <span class=\"loc\">(");
            if (link.isPresent()) {
                sb.append("<a href=\"").append(escape(link.get())).append("\">").append(escape(loc)).append("</a>");
            } else {
                sb.append(escape(loc));
            }
            sb.append(")</span>");
        }
        sb.append("</div>");
    }

    private Optional<String> editorLink(final StackTraceElement frame) {
        if (editorUriTemplate == null || sourceRoots.isEmpty() || frame.getFileName() == null) {
            return Optional.empty();
        }
        final var pkg = frame.getClassName().contains(".")
            ? frame.getClassName().substring(0, frame.getClassName().lastIndexOf('.')).replace('.', '/')
            : "";
        for (final var root : sourceRoots) {
            final var candidate = root.toAbsolutePath().normalize()
                .resolve(pkg).resolve(frame.getFileName());
            if (Files.exists(candidate)) {
                final var uri = editorUriTemplate
                    .replace("{file}", candidate.toString())
                    .replace("{line}", String.valueOf(Math.max(1, frame.getLineNumber())));
                return Optional.of(uri);
            }
        }
        return Optional.empty();
    }

    private static void kv(final StringBuilder sb, final String key, final String value) {
        sb.append("<div><span class=\"k\">").append(escape(key))
            .append("</span><span class=\"v\">").append(escape(value))
            .append("</span></div>");
    }

    private static String escape(final String s) {
        if (s == null) {
            return "";
        }
        final var sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            switch (c) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * A builder for {@link DevErrorHandler}.
     *
     * @author reed.vonredwitz
     * @since Apr-2026
     */
    public static final class Builder {

        private String editorUriTemplate;
        private final List<Path> sourceRoots = new ArrayList<>();

        private Builder() {
        }

        /**
         * Configures the editor URI template used for "open in editor" links. The template may
         * contain the placeholders {@code {file}} (absolute path) and {@code {line}}.
         * <p>Common templates:
         * <ul>
         *   <li>VS Code: {@code vscode://file/{file}:{line}}</li>
         *   <li>IntelliJ IDEA: {@code idea://open?file={file}&line={line}}</li>
         * </ul>
         *
         * @param template the editor URI template
         * @return this {@link Builder}
         */
        public Builder editorUriTemplate(final String template) {
            this.editorUriTemplate = Objects.requireNonNull(template, "template");
            return this;
        }

        /**
         * Sets the editor URL template from a known {@link Editor}. Equivalent to
         * {@code editorUriTemplate(editor.template())}.
         *
         * @param editor the editor
         * @return this {@link Builder}
         */
        public Builder editor(final Editor editor) {
            return editorUriTemplate(Objects.requireNonNull(editor, "editor").template());
        }

        /**
         * Adds a source-code root directory to the resolver. The handler tries each in order
         * and links to the first one that contains the stack frame's file. Typically
         * {@code Path.of("src/main/java")} for single-module apps; for multi-module projects
         * use {@link #discoverSourceRoots(Path)} or call this method once per module.
         *
         * @param sourceRoot the source root
         * @return this {@link Builder}
         */
        public Builder sourceRoot(final Path sourceRoot) {
            this.sourceRoots.add(Objects.requireNonNull(sourceRoot, "sourceRoot"));
            return this;
        }

        /**
         * Adds multiple source-code roots in one call.
         *
         * @param roots the source roots
         * @return this {@link Builder}
         */
        public Builder sourceRoots(final Iterable<Path> roots) {
            Objects.requireNonNull(roots, "roots");
            roots.forEach(this::sourceRoot);
            return this;
        }

        /**
         * Adds every {@code src/main/java} directory found one level under {@code repoRoot}.
         * Convenient for multi-module Maven/Gradle reactors where each sibling module owns its
         * own source tree.
         *
         * @param repoRoot the repository root containing module subdirectories
         * @return this {@link Builder}
         */
        public Builder discoverSourceRoots(final Path repoRoot) {
            Objects.requireNonNull(repoRoot, "repoRoot");
            try (Stream<Path> children = Files.list(repoRoot)) {
                children.filter(Files::isDirectory)
                    .map(p -> p.resolve("src/main/java"))
                    .filter(Files::isDirectory)
                    .forEach(this::sourceRoot);
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
            return this;
        }

        /**
         * Builds the {@link DevErrorHandler}.
         *
         * @return a new {@link DevErrorHandler}
         */
        public DevErrorHandler build() {
            return new DevErrorHandler(this);
        }
    }
}
