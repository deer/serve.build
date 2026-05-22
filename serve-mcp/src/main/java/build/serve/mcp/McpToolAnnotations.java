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

import java.util.List;
import java.util.Optional;

/**
 * Optional behavioral hints attached to an {@link McpTool}.
 *
 * <p>All fields are optional. Clients may use these hints to inform UI decisions
 * (e.g. showing a warning before invoking a destructive tool) but must not rely on
 * them for security enforcement — they are informational only.
 *
 * @param audience        roles this tool is intended for ({@code "user"}, {@code "assistant"}, or both)
 * @param readOnlyHint    if {@code true}, the tool does not modify state
 * @param destructiveHint if {@code true}, the tool may cause irreversible side-effects
 * @param idempotentHint  if {@code true}, repeated calls with the same arguments have no additional effect
 * @param openWorldHint   if {@code true}, the tool interacts with external entities beyond the server
 * @author reed.vonredwitz
 * @since May-2026
 */
public record McpToolAnnotations(Optional<List<String>> audience,
                                 Optional<Boolean> readOnlyHint,
                                 Optional<Boolean> destructiveHint,
                                 Optional<Boolean> idempotentHint,
                                 Optional<Boolean> openWorldHint
) {

    /**
     * Returns a new {@link Builder}.
     *
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link McpToolAnnotations}.
     */
    public static final class Builder {

        private List<String> audience;
        private Boolean readOnlyHint;
        private Boolean destructiveHint;
        private Boolean idempotentHint;
        private Boolean openWorldHint;

        private Builder() {
        }

        /**
         * Sets the intended audience for this tool.
         *
         * @param audience list of roles (e.g. {@code List.of("user", "assistant")})
         * @return this builder
         */
        public Builder audience(final List<String> audience) {
            this.audience = audience;
            return this;
        }

        /**
         * Hints that this tool does not modify state.
         *
         * @param readOnly whether the tool is read-only
         * @return this builder
         */
        public Builder readOnlyHint(final boolean readOnly) {
            this.readOnlyHint = readOnly;
            return this;
        }

        /**
         * Hints that this tool may cause irreversible side-effects.
         *
         * @param destructive whether the tool is destructive
         * @return this builder
         */
        public Builder destructiveHint(final boolean destructive) {
            this.destructiveHint = destructive;
            return this;
        }

        /**
         * Hints that repeated calls with the same arguments have no additional effect.
         *
         * @param idempotent whether the tool is idempotent
         * @return this builder
         */
        public Builder idempotentHint(final boolean idempotent) {
            this.idempotentHint = idempotent;
            return this;
        }

        /**
         * Hints that the tool interacts with external entities beyond the server.
         *
         * @param openWorld whether the tool has open-world interactions
         * @return this builder
         */
        public Builder openWorldHint(final boolean openWorld) {
            this.openWorldHint = openWorld;
            return this;
        }

        /**
         * Builds the {@link McpToolAnnotations}.
         *
         * @return the annotations
         */
        public McpToolAnnotations build() {
            return new McpToolAnnotations(
                Optional.ofNullable(audience == null ? null : List.copyOf(audience)),
                Optional.ofNullable(readOnlyHint),
                Optional.ofNullable(destructiveHint),
                Optional.ofNullable(idempotentHint),
                Optional.ofNullable(openWorldHint)
            );
        }
    }
}
