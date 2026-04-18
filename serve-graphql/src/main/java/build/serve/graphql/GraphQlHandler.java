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

import build.serve.foundation.Handler;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * Factory for creating a serve.build {@link Handler} that serves GraphQL requests.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class GraphQlHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GraphQlHandler() {
    }

    /**
     * Creates a {@link Handler} that processes GraphQL requests against the given schema.
     * <p>
     * The handler expects a JSON POST body containing a {@link GraphQlRequest} and
     * returns a JSON response with the {@link GraphQlResult}.
     *
     * @param schema the {@link GraphQlSchema} to execute against
     * @return a new {@link Handler}
     */
    public static Handler graphql(final GraphQlSchema schema) {
        return graphql(schema, GraphQlOptions.defaults());
    }

    /**
     * Creates a {@link Handler} that processes GraphQL requests with the given security options.
     * <p>
     * Options control introspection, max query depth, and max query complexity.
     *
     * @param schema  the {@link GraphQlSchema} to execute against
     * @param options the {@link GraphQlOptions} to apply
     * @return a new {@link Handler}
     */
    public static Handler graphql(final GraphQlSchema schema, final GraphQlOptions options) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(options, "options must not be null");

        final var effectiveSchema = schema.withOptions(options);

        return exchange -> {
            final var request = MAPPER.readValue(
                exchange.request().bodyAsStream(), GraphQlRequest.class);

            if (options.disableIntrospection() && isIntrospectionQuery(request.query())) {
                final var error = MAPPER.writeValueAsBytes(new GraphQlResult(
                    null, List.of(new GraphQlError("Introspection is not allowed", null))));
                exchange.response()
                    .status(400)
                    .header("Content-Type", "application/json")
                    .send(error);

                return;
            }

            final var result = effectiveSchema.execute(request);
            exchange.response().header("Content-Type", "application/json");
            exchange.response().send(MAPPER.writeValueAsBytes(result));
        };
    }

    private static boolean isIntrospectionQuery(final String query) {
        return query != null && (query.contains("__schema") || query.contains("__type"));
    }
}
