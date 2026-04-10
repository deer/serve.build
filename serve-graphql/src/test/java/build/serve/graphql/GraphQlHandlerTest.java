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

import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GraphQlHandler}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
class GraphQlHandlerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TestServer server;

    @BeforeEach
    void setUp() {
        final var schema = GraphQlSchema.builder("""
                type Query {
                    hello(name: String): String
                    user(id: ID!): User
                }
                type User {
                    id: ID!
                    name: String!
                }
                """)
            .fetcher("Query", "hello", env -> {
                final var name = env.<String>getArgument("name");
                return "Hello, " + (name != null ? name : "World") + "!";
            })
            .fetcher("Query", "user", env -> {
                final var id = env.<String>getArgument("id");
                return Map.of("id", id, "name", "User " + id);
            })
            .build();

        final var router = RouterBuilder.create()
            .post("/graphql", GraphQlHandler.graphql(schema))
            .build();

        server = TestServer.of(router);
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    @Test
    void simpleQueryTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ hello }\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("data").path("hello").asText()).isEqualTo("Hello, World!");
        assertThat(json.path("errors").isMissingNode()).isTrue();
    }

    @Test
    void queryWithArgTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ hello(name: \\\"Reed\\\") }\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("data").path("hello").asText()).isEqualTo("Hello, Reed!");
    }

    @Test
    void nestedTypeTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ user(id: \\\"42\\\") { id name } }\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("data").path("user").path("id").asText()).isEqualTo("42");
        assertThat(json.path("data").path("user").path("name").asText()).isEqualTo("User 42");
    }

    @Test
    void introspectionTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ __typename }\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("data").path("__typename").asText()).isEqualTo("Query");
    }

    @Test
    void invalidQueryTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ invalid syntax !!!\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("errors")).isNotEmpty();
    }

    @Test
    void unknownFieldTest() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ nonexistent }\"}")
            .send()
            .assertStatus(200);

        final var json = MAPPER.readTree(response.body());
        assertThat(json.path("errors")).isNotEmpty();
    }
}
