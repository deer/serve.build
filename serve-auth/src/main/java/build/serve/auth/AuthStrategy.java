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

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A strategy for authenticating an inbound {@link Request}.
 * <p>
 * Returning {@link Optional#empty()} signals that the request is unauthenticated;
 * {@link AuthMiddleware} will respond with 401. Returning a {@link Principal} allows
 * the request to proceed with that identity bound to {@link AuthContext}.
 * <p>
 * Strategies compose via {@link #anyOf(AuthStrategy...)}:
 * <pre>{@code
 * AuthStrategy.anyOf(bearerToken, apiKey)
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
@FunctionalInterface
public interface AuthStrategy {

    /**
     * Attempts to authenticate the given {@link Request}.
     *
     * @param request the inbound request
     * @return the authenticated {@link Principal}, or empty if authentication fails
     */
    Optional<Principal> authenticate(Request request);

    /**
     * Returns the {@code WWW-Authenticate} challenge value sent on 401 responses.
     * <p>
     * Concrete strategies override this to return scheme-specific values
     * (e.g., {@code Basic realm="app"}).
     *
     * @return the challenge string
     */
    default String challenge() {
        return "Bearer";
    }

    /**
     * Returns a composite strategy that tries each delegate in order, returning the first
     * non-empty result. If all delegates return empty, the result is empty.
     *
     * @param strategies the strategies to try
     * @return a composite {@link AuthStrategy}
     */
    static AuthStrategy anyOf(final AuthStrategy... strategies) {
        final var compositeChallenge = Arrays.stream(strategies)
            .map(AuthStrategy::challenge)
            .collect(Collectors.joining(", "));
        return new AuthStrategy() {
            @Override
            public Optional<Principal> authenticate(final Request request) {
                for (final var strategy : strategies) {
                    final var result = strategy.authenticate(request);
                    if (result.isPresent()) {
                        return result;
                    }
                }
                return Optional.empty();
            }

            @Override
            public String challenge() {
                return compositeChallenge;
            }
        };
    }
}
