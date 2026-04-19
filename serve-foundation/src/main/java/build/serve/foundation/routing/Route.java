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
package build.serve.foundation.routing;

import build.serve.foundation.Handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a single route definition mapping an HTTP method and path pattern to a {@link Handler}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class Route {

    /**
     * The HTTP method (e.g., GET, POST), or {@code null} for any method.
     */
    private final String method;

    /**
     * The path pattern (e.g., "/users/{id}").
     */
    private final String pattern;

    /**
     * The compiled regex pattern for matching.
     */
    private final Pattern compiledPattern;

    /**
     * The parameter names extracted from the pattern.
     */
    private final String[] paramNames;

    /**
     * Whether this route matches as a prefix (for nested routers).
     */
    private final boolean prefix;

    /**
     * The {@link Handler} for this route.
     */
    private final Handler handler;

    /**
     * Constructs a {@link Route}.
     *
     * @param method  the HTTP method, or {@code null} for any method
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     */
    public Route(final String method,
                 final String pattern,
                 final Handler handler) {
        this(method, pattern, handler, false);
    }

    /**
     * Constructs a {@link Route}.
     *
     * @param method  the HTTP method, or {@code null} for any method
     * @param pattern the path pattern
     * @param handler the {@link Handler}
     * @param prefix  whether to match as a prefix (for nested routers)
     */
    public Route(final String method,
                 final String pattern,
                 final Handler handler,
                 final boolean prefix) {
        this.method = method;
        this.pattern = Objects.requireNonNull(pattern, "pattern must not be null");
        this.handler = Objects.requireNonNull(handler, "handler must not be null");
        this.prefix = prefix;

        // Extract parameter names and build regex
        final var paramNameList = new java.util.ArrayList<String>();
        final var regex = new StringBuilder();

        final var parts = pattern.split("/");

        for (final var part : parts) {
            if (part.isEmpty()) {
                continue;
            }

            regex.append("/");

            if (part.startsWith("{") && part.endsWith("}")) {
                final var paramName = part.substring(1, part.length() - 1);

                paramNameList.add(paramName);
                regex.append("([^/]+)");
            } else {
                regex.append(Pattern.quote(part));
            }
        }

        if (regex.isEmpty() && !prefix) {
            regex.append("/");
        }

        final var suffix = prefix ? "(/.*)?" : "";

        this.compiledPattern = Pattern.compile("^" + regex + suffix + "$");
        this.paramNames = paramNameList.toArray(new String[0]);
    }

    /**
     * Returns the HTTP method constraint for this route, or {@code null} for any method.
     *
     * @return the HTTP method, or {@code null}
     */
    public String method() {
        return method;
    }

    /**
     * Returns the path pattern for this route (e.g., {@code "/users/{id}"}).
     *
     * @return the path pattern
     */
    public String pattern() {
        return pattern;
    }

    /**
     * Returns whether this route matches as a prefix (used for nested router mounting).
     *
     * @return {@code true} if this is a prefix route
     */
    public boolean isPrefix() {
        return prefix;
    }

    /**
     * Returns the handler for this route.
     *
     * @return the {@link Handler}
     */
    public Handler handler() {
        return handler;
    }

    /**
     * Attempts to match this {@link Route} against the specified method and path.
     *
     * @param requestMethod the HTTP method
     * @param requestPath   the request path
     * @return an {@link Optional} containing the {@link Match} if successful
     */
    public Optional<Match> match(final String requestMethod,
                                 final String requestPath) {
        if (method != null && !method.equalsIgnoreCase(requestMethod)) {
            return Optional.empty();
        }

        final Matcher matcher = compiledPattern.matcher(requestPath);

        if (!matcher.matches()) {
            return Optional.empty();
        }

        final Map<String, String> params = new HashMap<>();

        for (int i = 0; i < paramNames.length; i++) {
            params.put(paramNames[i], matcher.group(i + 1));
        }

        final var remainingPath = prefix
            ? matcher.group(paramNames.length + 1)
            : null;

        return Optional.of(new Match(handler, params,
            remainingPath != null ? remainingPath : "/"));
    }

    /**
     * The result of a successful route match, containing the {@link Handler} and extracted path parameters.
     *
     * @param handler       the matched {@link Handler}
     * @param params        the extracted path parameters
     * @param remainingPath the remaining path after the prefix, or "/" if not a prefix route
     */
    public record Match(Handler handler, Map<String, String> params, String remainingPath) {
    }
}
