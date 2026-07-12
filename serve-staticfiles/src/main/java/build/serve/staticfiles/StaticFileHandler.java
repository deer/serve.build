/*-
 * #%L
 * Serve Static
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
package build.serve.staticfiles;

import build.serve.foundation.Exchange;
import build.serve.foundation.routing.Router;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * A {@link Router} that serves static files from the filesystem or classpath resources.
 * <p>
 * Implements {@link Router} so it can be mounted as a prefix route via
 * {@link build.serve.foundation.routing.RouterBuilder#route(String, Router)},
 * which automatically strips the mount prefix from the request path.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Serve from a filesystem directory
 * StaticFileHandler.directory(Path.of("/var/www/public"))
 *
 * // Serve from classpath resources
 * StaticFileHandler.resources(MyClass.class, "/static")
 *
 * // With options
 * StaticFileHandler.builder()
 *     .directory(Path.of("/var/www/public"))
 *     .index("index.html")
 *     .cacheControl("max-age=3600")
 *     .build()
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class StaticFileHandler
    implements Router {

    /**
     * Default maximum file size (bytes) served from disk, matching {@code serve-transport-http}'s
     * default request body size cap. Prevents an unbounded {@code Files.readAllBytes} allocation
     * — and the memory-exhaustion DoS that comes with it — from serving an arbitrarily large file.
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Common MIME type mappings for web assets.
     */
    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
        Map.entry(".html", "text/html"),
        Map.entry(".htm", "text/html"),
        Map.entry(".css", "text/css"),
        Map.entry(".js", "application/javascript"),
        Map.entry(".json", "application/json"),
        Map.entry(".svg", "image/svg+xml"),
        Map.entry(".png", "image/png"),
        Map.entry(".jpg", "image/jpeg"),
        Map.entry(".jpeg", "image/jpeg"),
        Map.entry(".gif", "image/gif"),
        Map.entry(".ico", "image/x-icon"),
        Map.entry(".woff", "font/woff"),
        Map.entry(".woff2", "font/woff2"),
        Map.entry(".txt", "text/plain"),
        Map.entry(".xml", "application/xml"),
        Map.entry(".pdf", "application/pdf"),
        Map.entry(".webp", "image/webp"),
        Map.entry(".map", "application/json")
    );

    /**
     * The filesystem root directory, or {@code null} if serving classpath resources.
     */
    private final Path directory;

    /**
     * The classpath resource anchor class, or {@code null} if serving from the filesystem.
     */
    private final Class<?> resourceClass;

    /**
     * The classpath resource prefix, or {@code null} if serving from the filesystem.
     */
    private final String resourcePrefix;

    /**
     * The index file name (e.g., "index.html"), or {@code null} if disabled.
     */
    private final String index;

    /**
     * The Cache-Control header value, or {@code null} if not set.
     */
    private final String cacheControl;

    /**
     * The maximum file size (bytes) served from disk; larger files receive a 413 response.
     */
    private final long maxFileSize;

    /**
     * Constructs a {@link StaticFileHandler}.
     *
     * @param directory      the filesystem root directory, or {@code null}
     * @param resourceClass  the classpath resource anchor class, or {@code null}
     * @param resourcePrefix the classpath resource prefix, or {@code null}
     * @param index          the index file name, or {@code null}
     * @param cacheControl   the Cache-Control header value, or {@code null}
     * @param maxFileSize    the maximum file size (bytes) served from disk
     */
    private StaticFileHandler(final Path directory,
                              final Class<?> resourceClass,
                              final String resourcePrefix,
                              final String index,
                              final String cacheControl,
                              final long maxFileSize) {
        this.directory = directory;
        this.resourceClass = resourceClass;
        this.resourcePrefix = resourcePrefix;
        this.index = index;
        this.cacheControl = cacheControl;
        this.maxFileSize = maxFileSize;
    }

    /**
     * Creates a {@link StaticFileHandler} that serves files from the specified filesystem directory.
     *
     * @param directory the root directory
     * @return a new {@link StaticFileHandler}
     */
    public static StaticFileHandler directory(final Path directory) {
        Objects.requireNonNull(directory, "directory must not be null");

        return new StaticFileHandler(directory, null, null, "index.html", null, DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * Creates a {@link StaticFileHandler} that serves classpath resources.
     *
     * @param anchorClass    the class whose classloader is used to load resources
     * @param resourcePrefix the resource path prefix (e.g., "/static")
     * @return a new {@link StaticFileHandler}
     */
    public static StaticFileHandler resources(final Class<?> anchorClass,
                                              final String resourcePrefix) {
        Objects.requireNonNull(anchorClass, "anchorClass must not be null");
        Objects.requireNonNull(resourcePrefix, "resourcePrefix must not be null");

        return new StaticFileHandler(null, anchorClass, resourcePrefix, null, null, DEFAULT_MAX_FILE_SIZE);
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link StaticFileHandler}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void handle(final Exchange exchange) throws Exception {
        final var request = exchange.request();
        final var response = exchange.response();
        var path = request.path();

        // Normalize path
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        if (directory != null) {
            handleDirectory(exchange, path);
        } else {
            handleClasspath(exchange, path);
        }
    }

    /**
     * Serves a file from the filesystem directory.
     *
     * @param exchange the exchange
     * @param path     the normalized request path
     * @throws IOException if an I/O error occurs
     */
    private void handleDirectory(final Exchange exchange,
                                 final String path) throws IOException {
        final var response = exchange.response();
        var filePath = directory.resolve(stripLeadingSlash(path)).normalize();

        // Ensure the resolved path is syntactically within the root directory (cheap first pass,
        // rejects ".." traversal without touching the filesystem)
        if (!filePath.startsWith(directory.toAbsolutePath().normalize())) {
            response.status(400).send("Bad Request");

            return;
        }

        // If it's a directory, try the index file
        if (Files.isDirectory(filePath) && index != null) {
            filePath = filePath.resolve(index);
        }

        if (!Files.isRegularFile(filePath)) {
            response.status(404).send("Not Found");

            return;
        }

        // Resolve symlinks and re-check against the real root: normalize()+startsWith() alone
        // doesn't catch a symlink inside the root whose target points outside it.
        final Path realPath;
        final Path realRoot;

        try {
            realPath = filePath.toRealPath();
            realRoot = directory.toRealPath();
        } catch (final NoSuchFileException e) {
            response.status(404).send("Not Found");

            return;
        }

        if (!realPath.startsWith(realRoot)) {
            response.status(404).send("Not Found");

            return;
        }

        final var size = Files.size(filePath);

        if (size > maxFileSize) {
            response.status(413).send("Payload Too Large");

            return;
        }

        final var lastModified = Files.getLastModifiedTime(filePath).toMillis();
        final var etag = "\"" + Long.toHexString(lastModified) + "-" + Long.toHexString(size) + "\"";

        // Check If-None-Match
        final var ifNoneMatch = exchange.request().header("If-None-Match");

        if (ifNoneMatch.isPresent() && ifNoneMatch.get().equals(etag)) {
            response.status(304).send("");

            return;
        }

        final var contentType = detectMimeType(filePath.getFileName().toString());

        response.status(200);
        response.header("Content-Type", contentType);
        response.header("ETag", etag);

        if (cacheControl != null) {
            response.header("Cache-Control", cacheControl);
        }

        response.send(Files.readAllBytes(filePath));
    }

    /**
     * Serves a resource from the classpath.
     *
     * @param exchange the exchange
     * @param path     the normalized request path
     * @throws IOException if an I/O error occurs
     */
    private void handleClasspath(final Exchange exchange,
                                 final String path) throws IOException {
        final var response = exchange.response();
        final var resourcePath = resourcePrefix + (path.equals("/") ? "" : path);

        try (InputStream stream = resourceClass.getResourceAsStream(resourcePath)) {
            if (stream == null) {
                response.status(404).send("Not Found");

                return;
            }

            final var bytes = stream.readAllBytes();
            final var contentType = detectMimeType(resourcePath);

            response.status(200);
            response.header("Content-Type", contentType);

            if (cacheControl != null) {
                response.header("Cache-Control", cacheControl);
            }

            response.send(bytes);
        }
    }

    /**
     * Strips the leading slash from a path.
     *
     * @param path the path
     * @return the path without leading slash
     */
    private static String stripLeadingSlash(final String path) {
        if (path.startsWith("/")) {
            return path.substring(1);
        }

        return path;
    }

    /**
     * Detects the MIME type for the specified filename.
     *
     * @param filename the filename
     * @return the MIME type
     */
    static String detectMimeType(final String filename) {
        final var dotIndex = filename.lastIndexOf('.');

        if (dotIndex >= 0) {
            final var extension = filename.substring(dotIndex).toLowerCase();
            final var mapped = MIME_TYPES.get(extension);

            if (mapped != null) {
                return mapped;
            }
        }

        // Fallback to JDK detection
        try {
            final var probed = Files.probeContentType(Path.of(filename));

            if (probed != null) {
                return probed;
            }
        } catch (final IOException ignored) {
            // Fall through to default
        }

        return "application/octet-stream";
    }

    /**
     * A builder for configuring a {@link StaticFileHandler}.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        /**
         * The filesystem directory.
         */
        private Path directory;

        /**
         * The classpath resource anchor class.
         */
        private Class<?> resourceClass;

        /**
         * The classpath resource prefix.
         */
        private String resourcePrefix;

        /**
         * The index file name.
         */
        private String index;

        /**
         * The Cache-Control header value.
         */
        private String cacheControl;

        /**
         * The maximum file size (bytes) served from disk.
         */
        private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

        /**
         * Constructs a {@link Builder}.
         */
        private Builder() {
        }

        /**
         * Sets the filesystem directory to serve files from.
         *
         * @param dir the root directory
         * @return this {@link Builder} for fluent chaining
         */
        public Builder directory(final Path dir) {
            this.directory = Objects.requireNonNull(dir, "directory must not be null");

            return this;
        }

        /**
         * Sets the classpath resource anchor class and prefix.
         *
         * @param anchorClass the class whose classloader is used
         * @param prefix      the resource path prefix
         * @return this {@link Builder} for fluent chaining
         */
        public Builder resources(final Class<?> anchorClass,
                                 final String prefix) {
            this.resourceClass = Objects.requireNonNull(anchorClass, "anchorClass must not be null");
            this.resourcePrefix = Objects.requireNonNull(prefix, "resourcePrefix must not be null");

            return this;
        }

        /**
         * Sets the index file name served for directory requests.
         *
         * @param indexFile the index file name (e.g., "index.html")
         * @return this {@link Builder} for fluent chaining
         */
        public Builder index(final String indexFile) {
            this.index = indexFile;

            return this;
        }

        /**
         * Sets the Cache-Control header value.
         *
         * @param value the Cache-Control header value
         * @return this {@link Builder} for fluent chaining
         */
        public Builder cacheControl(final String value) {
            this.cacheControl = value;

            return this;
        }

        /**
         * Sets the maximum file size (bytes) served from disk. Requests for larger files receive
         * a {@code 413 Payload Too Large} response instead of being buffered fully into memory.
         * Defaults to 10 MiB.
         *
         * @param bytes the maximum file size in bytes
         * @return this {@link Builder} for fluent chaining
         */
        public Builder maxFileSize(final long bytes) {
            this.maxFileSize = bytes;

            return this;
        }

        /**
         * Builds the {@link StaticFileHandler}.
         *
         * @return a new {@link StaticFileHandler}
         */
        public StaticFileHandler build() {
            return new StaticFileHandler(directory, resourceClass, resourcePrefix, index, cacheControl, maxFileSize);
        }
    }
}
