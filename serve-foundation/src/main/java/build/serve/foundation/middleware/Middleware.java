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
package build.serve.foundation.middleware;

import build.serve.foundation.Handler;

/**
 * A {@link Middleware} wraps a {@link Handler} to provide cross-cutting behavior such as logging, authentication,
 * CORS, compression, etc.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@FunctionalInterface
public interface Middleware {

    /**
     * Applies this {@link Middleware} to the specified {@link Handler}, returning a new {@link Handler} that
     * includes the middleware behavior.
     *
     * @param next the next {@link Handler} in the chain
     * @return a {@link Handler} that wraps the next handler
     */
    Handler apply(Handler next);
}
