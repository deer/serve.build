/*-
 * #%L
 * Serve GraphQL
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
package build.serve.graphql;

/**
 * Security and execution controls for a GraphQL endpoint.
 * <p>
 * Use {@link #defaults()} for no restrictions, or {@link #builder()} for fine-grained control.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class GraphQlOptions {

    private final boolean disableIntrospection;
    private final int maxDepth;
    private final int maxComplexity;

    private GraphQlOptions(final Builder builder) {
        this.disableIntrospection = builder.disableIntrospection;
        this.maxDepth = builder.maxDepth;
        this.maxComplexity = builder.maxComplexity;
    }

    /**
     * Returns {@link GraphQlOptions} with no restrictions — introspection enabled, no depth or complexity limits.
     * <p>
     * This is an explicit, unrestricted opt-in — prefer {@link #safeDefaults()} unless you have a
     * specific reason to run without depth/complexity limits (e.g. a trusted internal caller).
     *
     * @return default {@link GraphQlOptions}
     */
    public static GraphQlOptions defaults() {
        return builder().build();
    }

    /**
     * Returns {@link GraphQlOptions} with sane out-of-the-box limits: introspection enabled,
     * max query depth {@code 15}, max query complexity {@code 10,000}. Used by
     * {@link GraphQlHandler#graphql(GraphQlSchema)} so that endpoint is not open to unbounded
     * nested-query denial of service by default. Call {@link #builder()} directly for different
     * limits, or {@link #defaults()} for no limits at all.
     *
     * @return safe default {@link GraphQlOptions}
     */
    public static GraphQlOptions safeDefaults() {
        return builder().maxDepth(15).maxComplexity(10_000).build();
    }

    /**
     * Returns a new {@link Builder}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns whether introspection queries ({@code __schema}, {@code __type}) are blocked.
     *
     * @return {@code true} if introspection is disabled
     */
    public boolean disableIntrospection() {
        return disableIntrospection;
    }

    /**
     * Returns the maximum allowed query depth, or {@code 0} for no limit.
     *
     * @return the max depth
     */
    public int maxDepth() {
        return maxDepth;
    }

    /**
     * Returns the maximum allowed query complexity (node count), or {@code 0} for no limit.
     *
     * @return the max complexity
     */
    public int maxComplexity() {
        return maxComplexity;
    }

    /**
     * A builder for {@link GraphQlOptions}.
     *
     * @author reed.vonredwitz
     * @since Apr-2026
     */
    public static final class Builder {

        private boolean disableIntrospection;
        private int maxDepth;
        private int maxComplexity;

        private Builder() {
        }

        /**
         * Blocks introspection queries ({@code __schema} and {@code __type}).
         *
         * @return this {@link Builder}
         */
        public Builder disableIntrospection() {
            this.disableIntrospection = true;

            return this;
        }

        /**
         * Rejects queries that exceed the specified depth.
         *
         * @param maxDepth maximum query depth; must be positive
         * @return this {@link Builder}
         */
        public Builder maxDepth(final int maxDepth) {
            if (maxDepth <= 0) {
                throw new IllegalArgumentException("maxDepth must be positive");
            }

            this.maxDepth = maxDepth;

            return this;
        }

        /**
         * Rejects queries that exceed the specified complexity (node count).
         *
         * @param maxComplexity maximum query complexity; must be positive
         * @return this {@link Builder}
         */
        public Builder maxComplexity(final int maxComplexity) {
            if (maxComplexity <= 0) {
                throw new IllegalArgumentException("maxComplexity must be positive");
            }

            this.maxComplexity = maxComplexity;

            return this;
        }

        /**
         * Builds the {@link GraphQlOptions}.
         *
         * @return a new {@link GraphQlOptions}
         */
        public GraphQlOptions build() {
            return new GraphQlOptions(this);
        }
    }
}
