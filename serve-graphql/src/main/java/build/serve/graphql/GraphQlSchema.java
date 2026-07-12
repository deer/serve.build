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

import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.analysis.MaxQueryComplexityInstrumentation;
import graphql.analysis.MaxQueryDepthInstrumentation;
import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A GraphQL schema that wraps the execution engine with a clean, JPMS-native API.
 * <p>
 * Built via {@link Builder} using a schema-first (SDL) approach.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class GraphQlSchema {

    private final GraphQL graphQL;

    private GraphQlSchema(final GraphQL graphQL) {
        this.graphQL = graphQL;
    }

    /**
     * Executes a GraphQL request against this schema.
     *
     * @param request the {@link GraphQlRequest} to execute
     * @return the {@link GraphQlResult}
     */
    public GraphQlResult execute(final GraphQlRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        final var inputBuilder = ExecutionInput.newExecutionInput()
            .query(request.query());

        if (request.operationName() != null) {
            inputBuilder.operationName(request.operationName());
        }

        if (request.variables() != null) {
            inputBuilder.variables(request.variables());
        }

        final ExecutionResult result = graphQL.execute(inputBuilder.build());

        final List<GraphQlError> errors = result.getErrors().stream()
            .map(e -> new GraphQlError(
                sanitizeMessage(e.getMessage()),
                e.getPath() != null
                    ? e.getPath().stream().map(Object::toString).toList()
                    : null))
            .toList();

        return new GraphQlResult(result.getData(), errors);
    }

    /**
     * Strips control characters from a data-fetcher exception message before it reaches the
     * client. graphql-java's default exception handler forwards a thrown exception's
     * {@code getMessage()} verbatim into the error response; this matches the same
     * control-character stripping {@code DefaultErrorHandler} and {@code McpServer} apply to
     * exception messages at the HTTP and MCP layers, defending against CRLF/control-character
     * injection via a data fetcher's exception message.
     *
     * @param message the raw exception message, or {@code null}
     * @return the sanitized message, or {@code null} if the input was {@code null}
     */
    private static String sanitizeMessage(final String message) {
        if (message == null) {
            return null;
        }

        return message.replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F]", " ");
    }

    /**
     * Returns a new {@link GraphQlSchema} with instrumentations derived from the given options applied.
     * Used by {@link GraphQlHandler} to wire depth and complexity limits without rebuilding the full schema.
     */
    GraphQlSchema withOptions(final GraphQlOptions options) {
        final var instrumentations = new ArrayList<Instrumentation>();

        if (options.maxDepth() > 0) {
            instrumentations.add(new MaxQueryDepthInstrumentation(options.maxDepth()));
        }

        if (options.maxComplexity() > 0) {
            instrumentations.add(new MaxQueryComplexityInstrumentation(options.maxComplexity()));
        }

        if (instrumentations.isEmpty()) {
            return this;
        }

        return new GraphQlSchema(
            GraphQL.newGraphQL(graphQL.getGraphQLSchema())
                .instrumentation(new ChainedInstrumentation(instrumentations))
                .build()
        );
    }

    /**
     * Creates a new {@link Builder} from a GraphQL SDL string.
     *
     * @param sdl the schema definition language string
     * @return a new {@link Builder}
     */
    public static Builder builder(final String sdl) {
        return new Builder(sdl);
    }

    /**
     * A builder for constructing {@link GraphQlSchema} instances.
     *
     * @author reed.vonredwitz
     * @since Mar-2026
     */
    public static final class Builder {

        private final String sdl;
        private final Map<String, Map<String, DataFetcher<?>>> fetchers;

        private Builder(final String sdl) {
            this.sdl = Objects.requireNonNull(sdl, "sdl must not be null");
            this.fetchers = new HashMap<>();
        }

        /**
         * Registers a {@link DataFetcher} for a specific type and field.
         *
         * @param <T>       the fetcher return type
         * @param typeName  the GraphQL type name (e.g., "Query")
         * @param fieldName the field name
         * @param fetcher   the {@link DataFetcher}
         * @return this builder
         */
        public <T> Builder fetcher(final String typeName,
                                   final String fieldName,
                                   final DataFetcher<T> fetcher) {
            fetchers.computeIfAbsent(typeName, k -> new HashMap<>())
                .put(fieldName, fetcher);
            return this;
        }

        /**
         * Builds the {@link GraphQlSchema}.
         *
         * @return a new {@link GraphQlSchema}
         */
        public GraphQlSchema build() {
            final TypeDefinitionRegistry registry = new SchemaParser().parse(sdl);

            final var wiringBuilder = RuntimeWiring.newRuntimeWiring();

            for (final var typeEntry : fetchers.entrySet()) {
                wiringBuilder.type(typeEntry.getKey(), builder -> {
                    for (final var fieldEntry : typeEntry.getValue().entrySet()) {
                        final DataFetcher<?> ourFetcher = fieldEntry.getValue();
                        builder.dataFetcher(fieldEntry.getKey(), adaptFetcher(ourFetcher));
                    }
                    return builder;
                });
            }

            final var schema = new SchemaGenerator()
                .makeExecutableSchema(registry, wiringBuilder.build());

            return new GraphQlSchema(GraphQL.newGraphQL(schema).build());
        }

        private static graphql.schema.DataFetcher<?> adaptFetcher(final DataFetcher<?> fetcher) {
            return env -> fetcher.fetch(new DataFetchingEnvironment() {

                @Override
                @SuppressWarnings("unchecked")
                public <T> T getArgument(final String name) {
                    return (T) env.getArgument(name);
                }

                @Override
                @SuppressWarnings("unchecked")
                public <T> T getSource() {
                    return (T) env.getSource();
                }

                @Override
                public String getField() {
                    return env.getField().getName();
                }
            });
        }
    }
}
