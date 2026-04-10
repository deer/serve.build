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
import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Represents an HTTP exchange — a paired {@link Request} and {@link Response}.
 * <p>
 * Each {@link Exchange} is processed on its own virtual thread.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface Exchange {

    /**
     * The attribute key used to store path parameters extracted by the router.
     */
    String PATH_PARAMS_ATTRIBUTE = "_pathParams";

    /**
     * The attribute key used to store a {@link BiFunction} that deserializes the request body.
     * <p>
     * The function accepts a {@link Request} and a {@link Class} and returns the deserialized object.
     * Registered by transport modules such as the JSON middleware.
     */
    String BODY_READER_ATTRIBUTE = "_bodyReader";

    /**
     * The attribute key used to store a body writer function that serializes an object to the response.
     * <p>
     * The function accepts a {@link Response} and an {@link Object} and writes it as the response body.
     * Registered by transport modules such as the JSON middleware.
     */
    String BODY_WRITER_ATTRIBUTE = "_bodyWriter";

    /**
     * The attribute key used to store the underlying {@code com.sun.net.httpserver.HttpExchange}.
     * <p>
     * Set by the HTTP transport layer. Required by protocol-upgrade handlers such as WebSocket
     * and Server-Sent Events that need access to the raw connection streams.
     */
    String HTTP_EXCHANGE_ATTRIBUTE = "_httpExchange";

    /**
     * Obtains the {@link Request} for this {@link Exchange}.
     *
     * @return the {@link Request}
     */
    Request request();

    /**
     * Obtains the {@link Response} for this {@link Exchange}.
     *
     * @return the {@link Response}
     */
    Response response();

    /**
     * Obtains a per-request {@link Option} associated with this {@link Exchange}.
     *
     * @param <T>         the type of {@link Option}
     * @param optionClass the {@link Class} of the {@link Option}
     * @return an {@link Optional} containing the {@link Option}, or empty if not set
     */
    <T extends Option> Optional<T> option(Class<T> optionClass);

    /**
     * Sets a per-request {@link Option} on this {@link Exchange}.
     *
     * @param option the {@link Option} to set
     */
    void option(Option option);

    /**
     * Obtains an attribute stored on this {@link Exchange} by key.
     *
     * @param <T>  the type of the attribute
     * @param key  the attribute key
     * @param type the {@link Class} of the attribute type
     * @return an {@link Optional} containing the attribute, or empty if not set
     */
    <T> Optional<T> attribute(String key, Class<T> type);

    /**
     * Sets an attribute on this {@link Exchange}.
     *
     * @param key   the attribute key
     * @param value the attribute value
     */
    void attribute(String key, Object value);

    /**
     * Obtains the path parameters extracted by the router for this {@link Exchange}.
     *
     * @return an unmodifiable {@link Map} of path parameter names to values, or an empty map if none
     */
    @SuppressWarnings("unchecked")
    default Map<String, String> pathParams() {
        return attribute(PATH_PARAMS_ATTRIBUTE, Map.class)
            .map(m -> (Map<String, String>) m)
            .orElse(Map.of());
    }

    /**
     * Obtains a single path parameter by name.
     *
     * @param name the name of the path parameter
     * @return an {@link Optional} containing the parameter value, or empty if not present
     */
    default Optional<String> pathParam(final String name) {
        return Optional.ofNullable(pathParams().get(name));
    }

    /**
     * Deserializes the request body as the specified type using the registered body reader.
     * <p>
     * Requires a body reader to be registered on this exchange (e.g., via the JSON middleware).
     *
     * @param <T>  the target type
     * @param type the {@link Class} of the target type
     * @return the deserialized body
     * @throws UnsupportedOperationException if no body reader is registered
     */
    @SuppressWarnings("unchecked")
    default <T> T bodyAs(final Class<T> type) {
        final var reader = attribute(BODY_READER_ATTRIBUTE, BiFunction.class);

        if (reader.isPresent()) {
            return (T) reader.get().apply(request(), type);
        }

        throw new UnsupportedOperationException(
            "Body deserialization requires a transport module (e.g., serve-transport-json)");
    }

    /**
     * Sends the specified object as a serialized response body (e.g., JSON) using the registered body writer.
     * <p>
     * Requires a body writer to be registered on this exchange (e.g., via the JSON middleware).
     *
     * @param body the object to serialize and send
     * @throws UnsupportedOperationException if no body writer is registered
     */
    @SuppressWarnings("unchecked")
    default void sendBody(final Object body) {
        final var writer = attribute(BODY_WRITER_ATTRIBUTE, java.util.function.BiConsumer.class);

        if (writer.isPresent()) {
            writer.get().accept(response(), body);

            return;
        }

        throw new UnsupportedOperationException(
            "Body serialization requires a transport module (e.g., serve-transport-json)");
    }
}
