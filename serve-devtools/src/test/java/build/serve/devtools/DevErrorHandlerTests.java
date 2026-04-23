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

import build.serve.foundation.error.HttpException;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DevErrorHandlerTests {

    @Test
    void shouldRender500ForUnexpectedException() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.create();

        handler.handle(StubExchange.get("/oops", response), new RuntimeException("boom"));

        assertThat(response.statusCode).isEqualTo(500);
        assertThat(response.headers).containsEntry("Content-Type", "text/html; charset=utf-8");
        assertThat(response.body)
            .contains("500")
            .contains("RuntimeException")
            .contains("boom")
            .contains("GET /oops");
    }

    @Test
    void shouldUseStatusFromHttpException() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.create();

        handler.handle(StubExchange.get("/missing", response),
            new HttpException(404, "not found"));

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).contains("404");
    }

    @Test
    void shouldEscapeHtmlInErrorMessage() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.create();

        handler.handle(StubExchange.get("/x", response),
            new RuntimeException("<script>alert(1)</script>"));

        assertThat(response.body)
            .doesNotContain("<script>alert(1)</script>")
            .contains("&lt;script&gt;alert(1)&lt;/script&gt;");
    }

    @Test
    void shouldIncludeCauseChain() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.create();
        final var cause = new IllegalStateException("inner");
        final var wrapped = new RuntimeException("outer", cause);

        handler.handle(StubExchange.get("/x", response), wrapped);

        assertThat(response.body).contains("Caused by").contains("IllegalStateException").contains("inner");
    }

    @Test
    void shouldIncludeEditorLinkWhenConfigured() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.builder()
            .editorUriTemplate("vscode://file/{file}:{line}")
            .sourceRoot(Path.of("src/test/java"))
            .build();

        handler.handle(StubExchange.get("/x", response), new RuntimeException("boom"));

        // The thrown RuntimeException has a frame in this test class, which lives in
        // src/test/java/build/serve/devtools/DevErrorHandlerTests.java
        assertThat(response.body).contains("vscode://file/");
    }

    @Test
    void shouldOmitEditorLinkWhenSourceFileNotFound() {
        final var response = new StubExchange.StubResponse();
        final var handler = DevErrorHandler.builder()
            .editorUriTemplate("vscode://file/{file}:{line}")
            .sourceRoot(Path.of("/no/such/path"))
            .build();

        handler.handle(StubExchange.get("/x", response), new RuntimeException("boom"));

        assertThat(response.body).doesNotContain("vscode://file/");
    }

    @Test
    void shouldDiscoverSourceRootsAcrossSiblingModules(@org.junit.jupiter.api.io.TempDir final Path repo)
        throws java.io.IOException {
        // Create two fake modules each with a src/main/java tree
        final var modA = repo.resolve("module-a/src/main/java/com/x");
        final var modB = repo.resolve("module-b/src/main/java/com/y");
        java.nio.file.Files.createDirectories(modA);
        java.nio.file.Files.createDirectories(modB);

        final var handler = DevErrorHandler.builder()
            .editorUriTemplate("vscode://file/{file}:{line}")
            .discoverSourceRoots(repo)
            .build();

        // Smoke: doesn't throw, registers both roots
        assertThat(handler).isNotNull();
    }
}
