/*-
 * #%L
 * Serve Health
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
package build.serve.health;

import java.util.Objects;

/**
 * A health check that reports whether a component is healthy.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@FunctionalInterface
public interface HealthCheck {

    /**
     * Performs the health check.
     *
     * @return {@code true} if the component is healthy, {@code false} otherwise
     * @throws Exception if the check fails
     */
    boolean check() throws Exception;

    /**
     * Obtains the name of this health check.
     *
     * @return the name, defaults to "unnamed"
     */
    default String name() {
        return "unnamed";
    }

    /**
     * Creates a named {@link HealthCheck}.
     *
     * @param name  the name of the health check
     * @param check the health check to wrap
     * @return a named {@link HealthCheck}
     */
    static HealthCheck of(final String name,
                          final HealthCheck check) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(check, "check");

        return new NamedHealthCheck(name, check);
    }
}
