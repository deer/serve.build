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
        Objects.requireNonNull(schema, "schema must not be null");

        return exchange -> {
            final var request = MAPPER.readValue(
                exchange.request().bodyAsStream(), GraphQlRequest.class);
            final var result = schema.execute(request);

            final var json = MAPPER.writeValueAsBytes(result);
            exchange.response().header("Content-Type", "application/json");
            exchange.response().send(json);
        };
    }
}
