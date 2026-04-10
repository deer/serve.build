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
package build.serve.example.graphql;

import build.serve.example.domain.TaskService;
import build.serve.foundation.Handler;
import build.serve.graphql.GraphQlHandler;
import build.serve.graphql.GraphQlSchema;
import build.serve.graphql.GraphiQlHandler;

/**
 * GraphQL endpoint for the task domain.
 * <p>
 * Mounts two routes:
 * <ul>
 *   <li>{@code POST /graphql} — executes GraphQL operations</li>
 *   <li>{@code GET  /graphiql} — serves the GraphiQL browser IDE</li>
 * </ul>
 *
 * Supported operations:
 * <pre>
 * query  { tasks { id title done } }
 * query  { task(id: "1") { id title done } }
 * mutation { createTask(title: "Buy milk") { id title done } }
 * mutation { toggleTask(id: "1") { id title done } }
 * mutation { deleteTask(id: "1") }
 * </pre>
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TaskGraphQlHandler {

    private static final String SDL = """
        type Task {
            id: ID!
            title: String!
            done: Boolean!
        }

        type Query {
            tasks: [Task!]!
            task(id: ID!): Task
        }

        type Mutation {
            createTask(title: String!): Task!
            toggleTask(id: ID!): Task
            deleteTask(id: ID!): Boolean!
        }
        """;

    private final Handler graphql;
    private final Handler graphiql;

    /**
     * Constructs a {@link TaskGraphQlHandler} backed by the given service.
     */
    public TaskGraphQlHandler(final TaskService service) {
        final var schema = GraphQlSchema.builder(SDL)
            .fetcher("Query", "tasks", env -> service.list())
            .fetcher("Query", "task", env -> {
                final var id = Long.parseLong(env.<String>getArgument("id"));
                return service.findById(id).orElse(null);
            })
            .fetcher("Mutation", "createTask", env -> {
                final var title = env.<String>getArgument("title");
                return service.create(title);
            })
            .fetcher("Mutation", "toggleTask", env -> {
                final var id = Long.parseLong(env.<String>getArgument("id"));
                return service.toggle(id).orElse(null);
            })
            .fetcher("Mutation", "deleteTask", env -> {
                final var id = Long.parseLong(env.<String>getArgument("id"));
                return service.delete(id);
            })
            .build();

        this.graphql = GraphQlHandler.graphql(schema);
        this.graphiql = GraphiQlHandler.graphiql("/graphql");
    }

    /**
     * Returns the GraphQL execution {@link Handler} (for {@code POST /graphql}).
     */
    public Handler graphqlHandler() {
        return graphql;
    }

    /**
     * Returns the GraphiQL IDE {@link Handler} (for {@code GET /graphiql}).
     */
    public Handler graphiqlHandler() {
        return graphiql;
    }
}
