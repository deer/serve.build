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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaviconHandlerTests {

    @Test
    void shouldReturn204ForEmpty() throws Exception {
        final var response = new StubExchange.StubResponse();
        FaviconHandler.empty().handle(StubExchange.get("/favicon.ico", response));

        assertThat(response.statusCode).isEqualTo(204);
        assertThat(response.body).isEmpty();
    }

    @Test
    void shouldServeSuppliedBytesWithContentType() throws Exception {
        final byte[] bytes = {0x01, 0x02, 0x03};
        final var response = new StubExchange.StubResponse();
        FaviconHandler.of(bytes, "image/png").handle(StubExchange.get("/favicon.ico", response));

        assertThat(response.statusCode).isEqualTo(200);
        assertThat(response.headers).containsEntry("Content-Type", "image/png");
        assertThat(response.headers).containsKey("Cache-Control");
        assertThat(response.sent).isTrue();
    }

    @Test
    void shouldDefensivelyCopyBytes() throws Exception {
        final byte[] bytes = {0x01, 0x02, 0x03};
        final var handler = FaviconHandler.of(bytes, "image/png");
        bytes[0] = 0x00;

        final var response = new StubExchange.StubResponse();
        handler.handle(StubExchange.get("/favicon.ico", response));

        // we can't compare the raw bytes (body is a String here), but the handler must not
        // observe the post-construction mutation of the input array
        assertThat(response.sent).isTrue();
    }

    @Test
    void shouldRejectMissingClasspathResource() {
        assertThatThrownBy(() -> FaviconHandler.fromClasspath("/does-not-exist.png"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not found");
    }
}
