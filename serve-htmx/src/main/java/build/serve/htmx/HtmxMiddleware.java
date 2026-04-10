/*-
 * #%L
 * Serve HTMX
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
package build.serve.htmx;

import build.serve.foundation.middleware.Middleware;

/**
 * A collection of HTMX-related {@link Middleware} factories.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class HtmxMiddleware {

    private HtmxMiddleware() {
    }

    /**
     * Returns a {@link Middleware} that only passes through HTMX requests (those with an {@code HX-Request: true}
     * header). Non-HTMX requests receive a {@code 400 Bad Request} response and are not forwarded to the next
     * handler.
     *
     * @return an HTMX-only guard {@link Middleware}
     */
    public static Middleware htmxOnly() {
        return next -> exchange -> {
            if (!HtmxRequest.of(exchange.request()).isHtmxRequest()) {
                exchange.response().status(400).send("");

                return;
            }

            next.handle(exchange);
        };
    }
}
