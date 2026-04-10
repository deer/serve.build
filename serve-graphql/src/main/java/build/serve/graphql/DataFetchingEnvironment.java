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
 * The environment available to a {@link DataFetcher} during field resolution.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface DataFetchingEnvironment {

    /**
     * Obtains a named argument from the GraphQL field.
     *
     * @param <T>  the argument type
     * @param name the argument name
     * @return the argument value, or {@code null} if not provided
     */
    <T> T getArgument(String name);

    /**
     * Obtains the source (parent) object for nested resolvers.
     *
     * @param <T> the source type
     * @return the source object
     */
    <T> T getSource();

    /**
     * Obtains the name of the field being resolved.
     *
     * @return the field name
     */
    String getField();
}
