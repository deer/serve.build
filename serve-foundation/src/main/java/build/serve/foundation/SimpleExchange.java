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
package build.serve.foundation;

import build.base.configuration.Option;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple implementation of {@link Exchange} backed by a {@link Request} and {@link Response}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class SimpleExchange
    implements Exchange {

    /**
     * The {@link Request}.
     */
    private final Request request;

    /**
     * The {@link Response}.
     */
    private final Response response;

    /**
     * The per-request {@link Option}s.
     */
    private final Map<Class<? extends Option>, Option> options;

    /**
     * The per-request attributes.
     */
    private final Map<String, Object> attributes;

    /**
     * Constructs a {@link SimpleExchange}.
     *
     * @param request  the {@link Request}
     * @param response the {@link Response}
     */
    public SimpleExchange(final Request request,
                          final Response response) {
        this.request = Objects.requireNonNull(request, "request must not be null");
        this.response = Objects.requireNonNull(response, "response must not be null");
        this.options = new ConcurrentHashMap<>();
        this.attributes = new ConcurrentHashMap<>();
    }

    @Override
    public Request request() {
        return request;
    }

    @Override
    public Response response() {
        return response;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Option> Optional<T> option(final Class<T> optionClass) {
        return Optional.ofNullable((T) options.get(optionClass));
    }

    @Override
    public void option(final Option option) {
        Objects.requireNonNull(option, "option must not be null");
        options.put(option.getClass(), option);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> attribute(final String key,
                                     final Class<T> type) {
        return Optional.ofNullable((T) attributes.get(key));
    }

    @Override
    public void attribute(final String key,
                          final Object value) {
        attributes.put(key, value);
    }
}
