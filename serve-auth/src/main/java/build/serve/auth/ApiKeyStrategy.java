/*-
 * #%L
 * Serve Auth
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
package build.serve.auth;

import build.serve.foundation.Request;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * An {@link AuthStrategy} that validates an API key extracted from a request header or query parameter.
 * <pre>{@code
 * ApiKeyStrategy.fromHeader("X-Api-Key", key -> lookup(key))
 * ApiKeyStrategy.fromQueryParam("api_key", key -> lookup(key))
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class ApiKeyStrategy implements AuthStrategy {

    private final Function<Request, Optional<String>> extractor;
    private final Function<String, Optional<Principal>> validator;

    private ApiKeyStrategy(final Function<Request, Optional<String>> extractor,
                           final Function<String, Optional<Principal>> validator) {
        this.extractor = extractor;
        this.validator = validator;
    }

    /**
     * Creates an {@link ApiKeyStrategy} that reads the key from a request header.
     *
     * @param headerName the header name (e.g., {@code "X-Api-Key"})
     * @param validator  a function mapping the key to a {@link Principal}, or empty if invalid
     * @return a new {@link ApiKeyStrategy}
     */
    public static ApiKeyStrategy fromHeader(final String headerName,
                                            final Function<String, Optional<Principal>> validator) {
        Objects.requireNonNull(headerName, "headerName");
        Objects.requireNonNull(validator, "validator");
        return new ApiKeyStrategy(req -> req.header(headerName), validator);
    }

    /**
     * Creates an {@link ApiKeyStrategy} that reads the key from a query parameter.
     *
     * @param paramName the query parameter name (e.g., {@code "api_key"})
     * @param validator a function mapping the key to a {@link Principal}, or empty if invalid
     * @return a new {@link ApiKeyStrategy}
     */
    public static ApiKeyStrategy fromQueryParam(final String paramName,
                                                final Function<String, Optional<Principal>> validator) {
        Objects.requireNonNull(paramName, "paramName");
        Objects.requireNonNull(validator, "validator");
        return new ApiKeyStrategy(req -> req.queryParam(paramName), validator);
    }

    @Override
    public Optional<Principal> authenticate(final Request request) {
        return extractor.apply(request).flatMap(validator);
    }
}
