/*-
 * #%L
 * Serve HTMX
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
package build.serve.htmx;

import build.serve.foundation.Request;

import java.util.Objects;
import java.util.Optional;

/**
 * Wraps a {@link Request} to provide typed access to HTMX request headers.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class HtmxRequest {

    private final Request request;

    private HtmxRequest(final Request request) {
        this.request = Objects.requireNonNull(request, "request");
    }

    /**
     * Wraps the given {@link Request} to provide HTMX header access.
     *
     * @param request the incoming {@link Request}
     * @return an {@link HtmxRequest} wrapping the given request
     */
    public static HtmxRequest of(final Request request) {
        return new HtmxRequest(request);
    }

    /**
     * Returns {@code true} if the {@code HX-Request} header is {@code "true"}, indicating this is an HTMX request.
     *
     * @return whether this is an HTMX request
     */
    public boolean isHtmxRequest() {
        return isTrue(HtmxHeaders.HX_REQUEST);
    }

    /**
     * Returns {@code true} if the {@code HX-Boosted} header is {@code "true"}, indicating this request was made
     * via an element with {@code hx-boost}.
     *
     * @return whether this is a boosted request
     */
    public boolean isBoosted() {
        return isTrue(HtmxHeaders.HX_BOOSTED);
    }

    /**
     * Returns the value of the {@code HX-Current-URL} header, representing the current URL of the browser.
     *
     * @return the current URL, or empty if not present
     */
    public Optional<String> currentUrl() {
        return request.header(HtmxHeaders.HX_CURRENT_URL);
    }

    /**
     * Returns {@code true} if the {@code HX-History-Restore-Request} header is {@code "true"}, indicating this
     * is a history restoration request.
     *
     * @return whether this is a history restore request
     */
    public boolean isHistoryRestoreRequest() {
        return isTrue(HtmxHeaders.HX_HISTORY_RESTORE_REQUEST);
    }

    /**
     * Returns the value of the {@code HX-Prompt} header, representing the user's response to an
     * {@code hx-prompt}.
     *
     * @return the prompt response, or empty if not present
     */
    public Optional<String> prompt() {
        return request.header(HtmxHeaders.HX_PROMPT);
    }

    /**
     * Returns the value of the {@code HX-Target} header, representing the id of the target element.
     *
     * @return the target element id, or empty if not present
     */
    public Optional<String> target() {
        return request.header(HtmxHeaders.HX_TARGET);
    }

    /**
     * Returns the value of the {@code HX-Trigger} header, representing the id of the triggered element.
     *
     * @return the trigger element id, or empty if not present
     */
    public Optional<String> triggerId() {
        return request.header(HtmxHeaders.HX_TRIGGER);
    }

    /**
     * Returns the value of the {@code HX-Trigger-Name} header, representing the name of the triggered element.
     *
     * @return the trigger element name, or empty if not present
     */
    public Optional<String> triggerName() {
        return request.header(HtmxHeaders.HX_TRIGGER_NAME);
    }

    private boolean isTrue(final String headerName) {
        return "true".equals(request.header(headerName).orElse(null));
    }
}
