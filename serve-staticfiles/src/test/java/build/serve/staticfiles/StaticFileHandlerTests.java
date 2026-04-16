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

import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class StaticFileHandlerTests {

    @TempDir
    Path tempDir;

    @Test
    void servesFileWithCorrectContentAndMimeType() throws Exception {
        Files.writeString(tempDir.resolve("hello.html"), "<h1>Hello</h1>");

        var handler = StaticFileHandler.directory(tempDir);
        var response = new StubResponse();
        var exchange = createExchange("GET", "/hello.html", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "text/html");
        assertThat(new String(response.bodyBytes)).isEqualTo("<h1>Hello</h1>");
    }

    @Test
    void returns404ForMissingFile() throws Exception {
        var handler = StaticFileHandler.directory(tempDir);
        var response = new StubResponse();
        var exchange = createExchange("GET", "/missing.txt", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(404);
    }

    @Test
    void pathTraversalIsBlocked() throws Exception {
        var handler = StaticFileHandler.directory(tempDir);
        var response = new StubResponse();
        var exchange = createExchange("GET", "/../../../etc/passwd", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isIn(400, 404);
    }

    @Test
    void servesIndexFileForDirectoryPath() throws Exception {
        Files.writeString(tempDir.resolve("index.html"), "<h1>Index</h1>");

        var handler = StaticFileHandler.directory(tempDir);
        var response = new StubResponse();
        var exchange = createExchange("GET", "/", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(new String(response.bodyBytes)).isEqualTo("<h1>Index</h1>");
    }

    @Test
    void etagAndNotModified() throws Exception {
        Files.writeString(tempDir.resolve("style.css"), "body { color: red; }");

        var handler = StaticFileHandler.directory(tempDir);

        // First request to get ETag
        var response1 = new StubResponse();
        var exchange1 = createExchange("GET", "/style.css", response1);

        handler.handle(exchange1);

        assertThat(response1.statusCode).isEqualTo(200);
        assertThat(response1.headers).containsKey("ETag");

        var etag = response1.headers.get("ETag");

        // Second request with If-None-Match
        var response2 = new StubResponse();
        var exchange2 = createExchangeWithHeaders("GET", "/style.css", response2,
            Map.of("If-None-Match", etag));

        handler.handle(exchange2);

        assertThat(response2.statusCode).isEqualTo(304);
    }

    @Test
    void servesClasspathResource() throws Exception {
        var handler = StaticFileHandler.resources(StaticFileHandlerTests.class, "/test-static");
        var response = new StubResponse();
        var exchange = createExchange("GET", "/test-file.txt", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(new String(response.bodyBytes)).isEqualTo("hello from classpath");
    }

    @Test
    void classpathResourceReturns404ForMissing() throws Exception {
        var handler = StaticFileHandler.resources(StaticFileHandlerTests.class, "/test-static");
        var response = new StubResponse();
        var exchange = createExchange("GET", "/nonexistent.txt", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(404);
    }

    @Test
    void detectsCommonMimeTypes() {
        assertThat(StaticFileHandler.detectMimeType("file.html")).isEqualTo("text/html");
        assertThat(StaticFileHandler.detectMimeType("file.css")).isEqualTo("text/css");
        assertThat(StaticFileHandler.detectMimeType("file.js")).isEqualTo("application/javascript");
        assertThat(StaticFileHandler.detectMimeType("file.json")).isEqualTo("application/json");
        assertThat(StaticFileHandler.detectMimeType("file.svg")).isEqualTo("image/svg+xml");
        assertThat(StaticFileHandler.detectMimeType("file.png")).isEqualTo("image/png");
        assertThat(StaticFileHandler.detectMimeType("file.jpg")).isEqualTo("image/jpeg");
    }

    @Test
    void builderSetsCacheControl() throws Exception {
        Files.writeString(tempDir.resolve("app.js"), "console.log('hi');");

        var handler = StaticFileHandler.builder()
            .directory(tempDir)
            .index("index.html")
            .cacheControl("max-age=3600")
            .build();

        var response = new StubResponse();
        var exchange = createExchange("GET", "/app.js", response);

        handler.handle(exchange);

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Cache-Control", "max-age=3600");
    }

    // --- Test helpers ---

    private SimpleExchange createExchange(final String method,
                                          final String path,
                                          final StubResponse response) {
        return createExchangeWithHeaders(method, path, response, Map.of());
    }

    private SimpleExchange createExchangeWithHeaders(final String method,
                                                     final String path,
                                                     final StubResponse response,
                                                     final Map<String, String> headers) {
        return new SimpleExchange(new StubRequest(method, path, headers), response);
    }

    private static final class StubRequest implements Request {

        private final String method;
        private final String path;
        private final Map<String, String> headers;

        StubRequest(final String method,
                    final String path,
                    final Map<String, String> headers) {
            this.method = method;
            this.path = path;
            this.headers = headers;
        }

        @Override
        public String method() {
            return method;
        }

        @Override
        public URI uri() {
            return URI.create(path);
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public Optional<String> pathParam(final String name) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(final String name) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(final String name) {
            return Optional.empty();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Map.of();
        }

        @Override
        public Optional<String> header(final String name) {
            return Optional.ofNullable(headers.get(name));
        }

        @Override
        public InputStream bodyAsStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public <T> T body(final Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubResponse implements Response {

        final Map<String, String> headers = new LinkedHashMap<>();
        int statusCode = 200;
        byte[] bodyBytes = new byte[0];

        @Override
        public Response status(final int statusCode) {
            this.statusCode = statusCode;

            return this;
        }

        @Override
        public int status() {
            return statusCode;
        }

        @Override
        public Response header(final String name, final String value) {
            headers.put(name, value);

            return this;
        }

        @Override
        public void send(final String body) {
            bodyBytes = body.getBytes();
        }

        @Override
        public void send(final byte[] body) {
            bodyBytes = body;
        }

        @Override
        public void json(final Object body) {
        }

        @Override
        public OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
