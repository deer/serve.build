/*-
 * #%L
 * Serve Application
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
package build.serve.application;

import build.codemodel.dependency.injection.Context;
import build.serve.foundation.Handler;
import build.serve.foundation.middleware.Middleware;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;

import java.util.Objects;

/**
 * A {@link RouterBuilder} wrapper that supports dependency-injection-aware route registration.
 * <p>
 * In addition to the standard handler-based route methods, this builder accepts {@link Handler}
 * {@link Class}es which are instantiated via the injection {@link Context} at build time.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class DependencyInjectionRouterBuilder {

    /**
     * The underlying {@link RouterBuilder}.
     */
    private final RouterBuilder delegate;

    /**
     * The injection {@link Context} used to create handler instances.
     */
    private final Context context;

    /**
     * Constructs a {@link DependencyInjectionRouterBuilder}.
     *
     * @param context the injection {@link Context}
     */
    private DependencyInjectionRouterBuilder(final Context context) {
        this.delegate = RouterBuilder.create();
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    /**
     * Creates a new {@link DependencyInjectionRouterBuilder} with the specified {@link Context}.
     *
     * @param context the injection {@link Context}
     * @return a new {@link DependencyInjectionRouterBuilder}
     */
    public static DependencyInjectionRouterBuilder create(final Context context) {
        return new DependencyInjectionRouterBuilder(context);
    }

    /**
     * Registers a route for any HTTP method using a {@link Handler} instance.
     *
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder route(final String pattern,
                                                  final Handler handler) {
        delegate.route(pattern, handler);

        return this;
    }

    /**
     * Registers a route for any HTTP method using a {@link Handler} class, instantiated via
     * the injection {@link Context}.
     *
     * @param pattern      the path pattern
     * @param handlerClass the {@link Handler} class to instantiate
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder route(final String pattern,
                                                  final Class<? extends Handler> handlerClass) {
        delegate.route(pattern, context.create(handlerClass));

        return this;
    }

    /**
     * Registers a route for HTTP GET using a {@link Handler} instance.
     *
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder get(final String pattern,
                                                final Handler handler) {
        delegate.get(pattern, handler);

        return this;
    }

    /**
     * Registers a route for HTTP GET using a {@link Handler} class.
     *
     * @param pattern      the path pattern
     * @param handlerClass the {@link Handler} class to instantiate
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder get(final String pattern,
                                                final Class<? extends Handler> handlerClass) {
        delegate.get(pattern, context.create(handlerClass));

        return this;
    }

    /**
     * Registers a route for HTTP POST using a {@link Handler} instance.
     *
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder post(final String pattern,
                                                 final Handler handler) {
        delegate.post(pattern, handler);

        return this;
    }

    /**
     * Registers a route for HTTP POST using a {@link Handler} class.
     *
     * @param pattern      the path pattern
     * @param handlerClass the {@link Handler} class to instantiate
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder post(final String pattern,
                                                 final Class<? extends Handler> handlerClass) {
        delegate.post(pattern, context.create(handlerClass));

        return this;
    }

    /**
     * Registers a route for HTTP PUT using a {@link Handler} instance.
     *
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder put(final String pattern,
                                                final Handler handler) {
        delegate.put(pattern, handler);

        return this;
    }

    /**
     * Registers a route for HTTP PUT using a {@link Handler} class.
     *
     * @param pattern      the path pattern
     * @param handlerClass the {@link Handler} class to instantiate
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder put(final String pattern,
                                                final Class<? extends Handler> handlerClass) {
        delegate.put(pattern, context.create(handlerClass));

        return this;
    }

    /**
     * Registers a route for HTTP DELETE using a {@link Handler} instance.
     *
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder delete(final String pattern,
                                                   final Handler handler) {
        delegate.delete(pattern, handler);

        return this;
    }

    /**
     * Registers a route for HTTP DELETE using a {@link Handler} class.
     *
     * @param pattern      the path pattern
     * @param handlerClass the {@link Handler} class to instantiate
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder delete(final String pattern,
                                                   final Class<? extends Handler> handlerClass) {
        delegate.delete(pattern, context.create(handlerClass));

        return this;
    }

    /**
     * Registers a {@link Middleware} to apply to all routes.
     *
     * @param middleware the {@link Middleware}
     * @return this builder for fluent chaining
     */
    public DependencyInjectionRouterBuilder middleware(final Middleware middleware) {
        delegate.middleware(middleware);

        return this;
    }

    /**
     * Builds the {@link Router} from the registered routes and middleware.
     *
     * @return a new {@link Router}
     */
    public Router build() {
        return delegate.build();
    }
}
