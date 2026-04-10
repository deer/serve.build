/*-
 * #%L
 * Serve Foundation
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
package build.serve.foundation.context;

import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.middleware.Middleware;

import java.util.Objects;
import java.util.function.Function;

/**
 * A {@link Middleware} that binds a {@link ScopedValue} for the duration of request handling.
 * <p>
 * Example usage:
 * <pre>{@code
 * new ScopedValueMiddleware<>(RequestContext.PRINCIPAL, exchange -> authenticate(exchange))
 * }</pre>
 *
 * @param <T> the type of the scoped value
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class ScopedValueMiddleware<T> implements Middleware {

    /**
     * The {@link ScopedValue} to bind.
     */
    private final ScopedValue<T> scopedValue;

    /**
     * The function to extract the value from the {@link Exchange}.
     */
    private final Function<Exchange, T> valueExtractor;

    /**
     * Constructs a {@link ScopedValueMiddleware}.
     *
     * @param scopedValue    the {@link ScopedValue} to bind
     * @param valueExtractor the function to extract the value from the {@link Exchange}
     */
    public ScopedValueMiddleware(final ScopedValue<T> scopedValue,
                                 final Function<Exchange, T> valueExtractor) {
        this.scopedValue = Objects.requireNonNull(scopedValue, "scopedValue must not be null");
        this.valueExtractor = Objects.requireNonNull(valueExtractor, "valueExtractor must not be null");
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final T value = valueExtractor.apply(exchange);
            ScopedValue.where(scopedValue, value).run(() -> {
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
}
