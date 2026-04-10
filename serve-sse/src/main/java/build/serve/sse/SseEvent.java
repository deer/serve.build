/*-
 * #%L
 * Serve SSE
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
package build.serve.sse;

import java.util.Objects;

/**
 * A value type representing a single Server-Sent Event (SSE) per RFC 8895.
 *
 * @param data  the event data (required)
 * @param id    the event id (optional, {@code null} to omit)
 * @param event the event type (optional, {@code null} to omit)
 * @param retry the reconnection time in milliseconds (optional, {@code null} to omit)
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record SseEvent(
    String data,
    String id,
    String event,
    Integer retry
) {

    /**
     * Creates an {@link SseEvent} with only data.
     *
     * @param data the event data
     * @return a new {@link SseEvent}
     */
    public static SseEvent of(final String data) {
        Objects.requireNonNull(data, "data must not be null");
        return new SseEvent(data, null, null, null);
    }

    /**
     * Creates an {@link SseEvent} with an event type and data.
     *
     * @param event the event type
     * @param data  the event data
     * @return a new {@link SseEvent}
     */
    public static SseEvent of(final String event, final String data) {
        Objects.requireNonNull(data, "data must not be null");
        Objects.requireNonNull(event, "event must not be null");
        return new SseEvent(data, null, event, null);
    }

    /**
     * Serializes this event to the SSE wire format.
     *
     * @return the SSE-formatted string
     */
    public String serialize() {
        final var sb = new StringBuilder();

        if (id != null) {
            sb.append("id: ").append(id).append('\n');
        }
        if (event != null) {
            sb.append("event: ").append(event).append('\n');
        }
        if (retry != null) {
            sb.append("retry: ").append(retry).append('\n');
        }

        // Multi-line data: each line gets its own data: field
        for (final var line : data.split("\n", -1)) {
            sb.append("data: ").append(line).append('\n');
        }

        sb.append('\n');
        return sb.toString();
    }
}
