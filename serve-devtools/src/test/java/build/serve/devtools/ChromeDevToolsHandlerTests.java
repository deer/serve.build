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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ChromeDevToolsHandlerTests {

    @Test
    void shouldExposeTheWellKnownPath() {
        assertThat(ChromeDevToolsHandler.PATH)
            .isEqualTo("/.well-known/appspecific/com.chrome.devtools.json");
    }

    @Test
    void shouldReturnWorkspaceJsonWithAbsolutePathAndUuid() throws Exception {
        final var root = Path.of("/tmp/example-project");
        final var uuid = UUID.fromString("11111111-2222-3333-4444-555555555555");

        final var response = new StubExchange.StubResponse();
        ChromeDevToolsHandler.forWorkspace(root, uuid).handle(StubExchange.get(ChromeDevToolsHandler.PATH, response));

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "application/json");
        assertThat(response.body).isEqualTo(
            "{\"workspace\":{\"root\":\"/tmp/example-project\","
                + "\"uuid\":\"11111111-2222-3333-4444-555555555555\"}}");
    }

    @Test
    void shouldDeriveStableUuidWhenNotSupplied() throws Exception {
        final var root = Path.of("/tmp/example-project");

        final var first = new StubExchange.StubResponse();
        final var second = new StubExchange.StubResponse();
        ChromeDevToolsHandler.forWorkspace(root).handle(StubExchange.get(ChromeDevToolsHandler.PATH, first));
        ChromeDevToolsHandler.forWorkspace(root).handle(StubExchange.get(ChromeDevToolsHandler.PATH, second));

        assertThat(first.body).isEqualTo(second.body);
    }

    @Test
    void shouldEscapeBackslashesInPath() throws Exception {
        final var root = Path.of("C:\\Users\\dev\\project");
        final var uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

        final var response = new StubExchange.StubResponse();
        ChromeDevToolsHandler.forWorkspace(root, uuid).handle(StubExchange.get(ChromeDevToolsHandler.PATH, response));

        assertThat(response.body).contains("\\\\");
    }
}
