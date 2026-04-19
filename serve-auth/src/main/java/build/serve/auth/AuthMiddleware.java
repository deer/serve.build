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

import build.serve.foundation.Handler;
import build.serve.foundation.context.RequestContext;
import build.serve.foundation.middleware.Middleware;

import java.util.Objects;

/**
 * A {@link Middleware} that authenticates every request using the configured {@link AuthStrategy}.
 * <p>
 * On success, the authenticated {@link Principal} is bound to {@link AuthContext} for the duration
 * of the request. On failure, a 401 response is sent with an appropriate {@code WWW-Authenticate}
 * challenge and the handler chain is not invoked.
 * <pre>{@code
 * AuthMiddleware.builder()
 *     .strategy(BearerTokenStrategy.of(token -> validate(token)))
 *     .build()
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class AuthMiddleware implements Middleware {

    private final AuthStrategy strategy;

    private AuthMiddleware(final Builder builder) {
        this.strategy = builder.strategy;
    }

    /**
     * Creates a new {@link Builder} for configuring an {@link AuthMiddleware}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var principal = strategy.authenticate(exchange.request());
            if (principal.isEmpty()) {
                exchange.response()
                    .header("WWW-Authenticate", strategy.challenge())
                    .status(401)
                    .send("");
                return;
            }
            ScopedValue.where(RequestContext.PRINCIPAL, principal.get()).run(() -> {
                try {
                    next.handle(exchange);
                } catch (final RuntimeException e) {
                    throw e;
                } catch (final Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }

    /**
     * A builder for configuring an {@link AuthMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Apr-2026
     */
    public static final class Builder {

        private AuthStrategy strategy;

        private Builder() {
        }

        /**
         * Sets the {@link AuthStrategy} used to authenticate requests.
         *
         * @param strategy the authentication strategy
         * @return this {@link Builder}
         */
        public Builder strategy(final AuthStrategy strategy) {
            this.strategy = Objects.requireNonNull(strategy, "strategy");
            return this;
        }

        /**
         * Builds the {@link AuthMiddleware}.
         *
         * @return a new {@link AuthMiddleware}
         */
        public AuthMiddleware build() {
            Objects.requireNonNull(strategy, "strategy");
            return new AuthMiddleware(this);
        }
    }
}
