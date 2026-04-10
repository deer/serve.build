/*-
 * #%L
 * Serve GraphQL
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
package build.serve.graphql;

import build.serve.testing.TestServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphiQlHandler}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
class GraphiQlHandlerTest {

    private TestServer server;

    @BeforeEach
    void setUp() {
        server = TestServer.of(GraphiQlHandler.graphiql("/graphql"));
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void returnsHtmlContentType() throws Exception {
        final var response = server.get("/")
            .send()
            .assertStatus(200);

        assertThat(response.contentType()).isEqualTo("text/html; charset=utf-8");
    }

    @Test
    void bodyContainsEndpoint() throws Exception {
        final var response = server.get("/")
            .send()
            .assertStatus(200);

        assertThat(response.body()).contains("/graphql");
    }

    @Test
    void bodyContainsGraphiQlMarkers() throws Exception {
        final var response = server.get("/")
            .send()
            .assertStatus(200);

        assertThat(response.body())
            .contains("graphiql")
            .contains("GraphiQL");
    }
}
