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

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Factory for creating a serve.build {@link Handler} that serves the GraphiQL IDE.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class GraphiQlHandler {

    private static final String TEMPLATE = """
        <!doctype html>
        <html lang="en">
        <head>
            <meta charset="UTF-8"/>
            <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
            <title>GraphiQL</title>
            <style>
                body { margin: 0; }
                #graphiql { height: 100dvh; }
            </style>
            <link rel="stylesheet" href="https://unpkg.com/graphiql@3/graphiql.min.css"/>
            <script crossorigin src="https://unpkg.com/react@18/umd/react.production.min.js"></script>
            <script crossorigin src="https://unpkg.com/react-dom@18/umd/react-dom.production.min.js"></script>
            <script crossorigin src="https://unpkg.com/graphiql@3/graphiql.min.js"></script>
        </head>
        <body>
            <div id="graphiql">Loading\u2026</div>
            <script>
                const root = ReactDOM.createRoot(document.getElementById('graphiql'));
                root.render(React.createElement(GraphiQL, {
                    fetcher: GraphiQL.createFetcher({ url: '{{ENDPOINT}}' })
                }));
            </script>
        </body>
        </html>
        """;

    private GraphiQlHandler() {
    }

    /**
     * Creates a {@link Handler} that serves the GraphiQL IDE, configured to send
     * queries to the given GraphQL endpoint.
     *
     * @param endpoint the GraphQL endpoint path (e.g. {@code "/graphql"})
     * @return a new {@link Handler}
     */
    public static Handler graphiql(final String endpoint) {
        Objects.requireNonNull(endpoint, "endpoint must not be null");

        final var html = TEMPLATE.replace("{{ENDPOINT}}", endpoint)
            .getBytes(StandardCharsets.UTF_8);

        return exchange -> {
            exchange.response().header("Content-Type", "text/html; charset=utf-8");
            exchange.response().send(html);
        };
    }
}
