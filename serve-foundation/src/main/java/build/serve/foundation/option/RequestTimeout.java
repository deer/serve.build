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
package build.serve.foundation.option;

import build.base.configuration.Option;

import java.time.Duration;

/**
 * An {@link Option} to define the maximum time allowed per request handler.
 * <p>
 * When set, the transport will interrupt the handler thread if it exceeds the timeout,
 * protecting against Slowloris and other slow-request attacks.
 *
 * @param duration the maximum time to allow a request handler to run; {@link Duration#ZERO} disables enforcement
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public record RequestTimeout(Duration duration)
    implements Option {

    /**
     * No request timeout — handlers run until completion.
     */
    public static final RequestTimeout NONE = new RequestTimeout(Duration.ZERO);

    /**
     * Creates a {@link RequestTimeout} for the specified duration.
     *
     * @param duration the request timeout duration
     * @return a new {@link RequestTimeout}
     */
    public static RequestTimeout of(final Duration duration) {
        return new RequestTimeout(duration);
    }

    /**
     * Creates a {@link RequestTimeout} for the specified number of seconds.
     *
     * @param seconds the request timeout in seconds
     * @return a new {@link RequestTimeout}
     */
    public static RequestTimeout ofSeconds(final long seconds) {
        return new RequestTimeout(Duration.ofSeconds(seconds));
    }
}
