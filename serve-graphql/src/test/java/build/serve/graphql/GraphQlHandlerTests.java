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

import build.base.json.Json;
import build.base.json.JsonArray;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
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
class GraphQlHandlerTests {

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
    void shouldExecuteSimpleQuery() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ hello }\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(json.get("data").asObject().getString("hello")).isEqualTo("Hello, World!");
        assertThat(json.has("errors")).isFalse();
    }

    @Test
    void shouldExecuteQueryWithArgument() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ hello(name: \\\"Reed\\\") }\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(json.get("data").asObject().getString("hello")).isEqualTo("Hello, Reed!");
    }

    @Test
    void shouldResolveNestedType() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ user(id: \\\"42\\\") { id name } }\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        final var user = json.get("data").asObject().get("user").asObject();
        assertThat(user.getString("id")).isEqualTo("42");
        assertThat(user.getString("name")).isEqualTo("User 42");
    }

    @Test
    void shouldAllowIntrospectionByDefault() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ __typename }\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(json.get("data").asObject().getString("__typename")).isEqualTo("Query");
    }

    @Test
    void shouldReturnErrorsForInvalidQuery() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ invalid syntax !!!\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(((JsonArray) json.get("errors")).values()).isNotEmpty();
    }

    @Test
    void shouldStripControlCharactersFromDataFetcherExceptionMessage() throws Exception {
        final var throwingSchema = GraphQlSchema.builder("""
                type Query {
                    boom: String
                }
                """)
            .fetcher("Query", "boom", env -> {
                throw new RuntimeException("line one\r\nline two\tinjected");
            })
            .build();

        final var throwingRouter = RouterBuilder.create()
            .post("/graphql", GraphQlHandler.graphql(throwingSchema))
            .build();

        try (var throwingServer = TestServer.of(throwingRouter)) {
            final var response = throwingServer.post("/graphql")
                .header("Content-Type", "application/json")
                .body("{\"query\":\"{ boom }\"}")
                .send()
                .assertStatus(200);

            final var json = Json.parse(response.body()).asObject();
            final var errors = (JsonArray) json.get("errors");
            final var message = errors.values().get(0).asObject().getString("message");

            assertThat(message).doesNotContain("\r").doesNotContain("\n").doesNotContain("\t");
            assertThat(message).contains("line one  line two injected");
        }
    }

    @Test
    void shouldBlockIntrospectionWhenDisabled() throws Exception {
        var schema = GraphQlSchema.builder("""
                type Query { hello: String }
                """)
            .fetcher("Query", "hello", env -> "hi")
            .build();

        var restrictedServer = TestServer.of(
            RouterBuilder.create()
                .post("/graphql", GraphQlHandler.graphql(schema,
                    GraphQlOptions.builder().disableIntrospection().build()))
                .build());

        try {
            var response = restrictedServer.post("/graphql")
                .header("Content-Type", "application/json")
                .body("{\"query\":\"{ __schema { types { name } } }\"}")
                .send();

            assertThat(response.status()).isEqualTo(400);
            var json = Json.parse(response.body()).asObject();
            assertThat(((JsonArray) json.get("errors")).values().get(0).asObject().getString("message"))
                .isEqualTo("Introspection is not allowed");
        } finally {
            restrictedServer.close();
        }
    }

    @Test
    void shouldAllowFullIntrospectionByDefault() throws Exception {
        var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ __schema { queryType { name } } }\"}")
            .send()
            .assertStatus(200);

        var json = Json.parse(response.body()).asObject();
        assertThat(json.get("data").asObject().get("__schema").asObject().get("queryType").asObject().getString("name"))
            .isEqualTo("Query");
    }

    @Test
    void shouldRejectQueryExceedingMaxDepth() throws Exception {
        var schema = GraphQlSchema.builder("""
                type Query { user: User }
                type User { friend: Friend }
                type Friend { name: String }
                """)
            .fetcher("Query", "user", env -> java.util.Map.of())
            .fetcher("User", "friend", env -> java.util.Map.of("name", "Bob"))
            .build();

        var depthServer = TestServer.of(
            RouterBuilder.create()
                .post("/graphql", GraphQlHandler.graphql(schema,
                    GraphQlOptions.builder().maxDepth(1).build()))
                .build());

        try {
            var response = depthServer.post("/graphql")
                .header("Content-Type", "application/json")
                .body("{\"query\":\"{ user { friend { name } } }\"}")
                .send()
                .assertStatus(200);

            var json = Json.parse(response.body()).asObject();
            assertThat(((JsonArray) json.get("errors")).values()).isNotEmpty();
        } finally {
            depthServer.close();
        }
    }

    @Test
    void shouldRejectDeeplyNestedQueryByDefault() throws Exception {
        var schema = GraphQlSchema.builder("""
                type Query { user: User }
                type User { friend: User name: String }
                """)
            .fetcher("Query", "user", env -> java.util.Map.of())
            .fetcher("User", "friend", env -> java.util.Map.of("name", "Bob"))
            .build();

        var defaultServer = TestServer.of(
            RouterBuilder.create()
                .post("/graphql", GraphQlHandler.graphql(schema))
                .build());

        try {
            final var open = "{ user { friend ".repeat(20);
            final var close = "} ".repeat(20) + "}";
            final var query = open + "name" + close;

            var response = defaultServer.post("/graphql")
                .header("Content-Type", "application/json")
                .body("{\"query\":\"" + query + "\"}")
                .send()
                .assertStatus(200);

            var json = Json.parse(response.body()).asObject();
            assertThat(((JsonArray) json.get("errors")).values()).isNotEmpty();
        } finally {
            defaultServer.close();
        }
    }

    @Test
    void shouldReturnErrorsForUnknownField() throws Exception {
        final var response = server.post("/graphql")
            .header("Content-Type", "application/json")
            .body("{\"query\":\"{ nonexistent }\"}")
            .send()
            .assertStatus(200);

        final var json = Json.parse(response.body()).asObject();
        assertThat(((JsonArray) json.get("errors")).values()).isNotEmpty();
    }

    @Test
    void shouldSerializeResultWithNullErrors() {
        final var result = new GraphQlResult(Map.of("hello", "world"), null);
        final var json = result.toJson().asObject();
        assertThat(json.get("data").asObject().getString("hello")).isEqualTo("world");
        assertThat(json.has("errors")).isFalse();
    }
}
