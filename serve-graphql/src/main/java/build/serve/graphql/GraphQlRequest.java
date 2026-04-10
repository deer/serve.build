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

import java.util.Map;

/**
 * A GraphQL request containing the query, optional operation name, and variables.
 *
 * @param query         the GraphQL query string
 * @param operationName the operation name, or {@code null}
 * @param variables     the variables map, or {@code null}
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record GraphQlRequest(
    String query,
    String operationName,
    Map<String, Object> variables
) {
}
