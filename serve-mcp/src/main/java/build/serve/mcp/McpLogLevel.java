/*-
 * #%L
 * Serve MCP
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
package build.serve.mcp;

import java.util.Optional;

/**
 * MCP logging severity levels, ordered from least to most severe (matching the 2025-03-26 spec).
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public enum McpLogLevel {
    debug,
    info,
    notice,
    warning,
    error,
    critical,
    alert,
    emergency;

    boolean meets(final McpLogLevel threshold) {
        return this.ordinal() >= threshold.ordinal();
    }

    static Optional<McpLogLevel> fromString(final String s) {
        for (final var level : values()) {
            if (level.name().equals(s)) {
                return Optional.of(level);
            }
        }
        return Optional.empty();
    }
}
