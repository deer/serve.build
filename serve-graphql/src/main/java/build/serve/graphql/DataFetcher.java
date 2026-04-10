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

/**
 * A fetcher that resolves a GraphQL field value.
 *
 * @param <T> the type of the resolved value
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@FunctionalInterface
public interface DataFetcher<T> {

    /**
     * Fetches the value for a GraphQL field.
     *
     * @param env the {@link DataFetchingEnvironment}
     * @return the resolved value
     * @throws Exception if an error occurs during fetching
     */
    T fetch(DataFetchingEnvironment env) throws Exception;
}
