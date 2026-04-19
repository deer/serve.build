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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * An {@link AuthStrategy} that validates {@code Authorization: Basic <base64(user:pass)>} credentials.
 * <pre>{@code
 * BasicAuthStrategy.of("myapp", (user, pass) ->
 *     validUsers.authenticate(user, pass) ? Optional.of(SimplePrincipal.of(user)) : Optional.empty())
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class BasicAuthStrategy implements AuthStrategy {

    private static final String PREFIX = "Basic ";

    private final String realm;
    private final BiFunction<String, String, Optional<Principal>> validator;

    private BasicAuthStrategy(final String realm,
                              final BiFunction<String, String, Optional<Principal>> validator) {
        this.realm = realm;
        this.validator = validator;
    }

    /**
     * Creates a {@link BasicAuthStrategy} with the given validator and default realm {@code "serve"}.
     *
     * @param validator a function mapping (username, password) to a {@link Principal}, or empty if invalid
     * @return a new {@link BasicAuthStrategy}
     */
    public static BasicAuthStrategy of(final BiFunction<String, String, Optional<Principal>> validator) {
        return new BasicAuthStrategy("serve", Objects.requireNonNull(validator, "validator"));
    }

    /**
     * Creates a {@link BasicAuthStrategy} with the given realm and validator.
     *
     * @param realm     the authentication realm shown to clients
     * @param validator a function mapping (username, password) to a {@link Principal}, or empty if invalid
     * @return a new {@link BasicAuthStrategy}
     */
    public static BasicAuthStrategy of(final String realm,
                                       final BiFunction<String, String, Optional<Principal>> validator) {
        return new BasicAuthStrategy(
            Objects.requireNonNull(realm, "realm"),
            Objects.requireNonNull(validator, "validator")
        );
    }

    @Override
    public Optional<Principal> authenticate(final Request request) {
        return request.header("Authorization")
            .filter(h -> h.startsWith(PREFIX))
            .map(h -> h.substring(PREFIX.length()))
            .flatMap(encoded -> {
                try {
                    final var decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
                    final var colon = decoded.indexOf(':');
                    if (colon < 0) {
                        return Optional.empty();
                    }
                    return validator.apply(decoded.substring(0, colon), decoded.substring(colon + 1));
                } catch (final IllegalArgumentException e) {
                    return Optional.empty();
                }
            });
    }

    @Override
    public String challenge() {
        return "Basic realm=\"" + realm + "\"";
    }
}
