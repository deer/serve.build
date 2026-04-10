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
 * An {@link Option} to define the graceful shutdown drain timeout.
 *
 * @param duration the maximum time to wait for in-flight requests to complete
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record ShutdownTimeout(Duration duration)
    implements Option {

    /**
     * The default {@link ShutdownTimeout} (5 seconds).
     */
    public static final ShutdownTimeout DEFAULT = new ShutdownTimeout(Duration.ofSeconds(5));

    /**
     * Creates a {@link ShutdownTimeout} for the specified duration.
     *
     * @param duration the shutdown timeout duration
     * @return a new {@link ShutdownTimeout}
     */
    public static ShutdownTimeout of(final Duration duration) {
        return new ShutdownTimeout(duration);
    }

    /**
     * Creates a {@link ShutdownTimeout} for the specified number of seconds.
     *
     * @param seconds the shutdown timeout in seconds
     * @return a new {@link ShutdownTimeout}
     */
    public static ShutdownTimeout ofSeconds(final long seconds) {
        return new ShutdownTimeout(Duration.ofSeconds(seconds));
    }
}
