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

/**
 * Constants for HTMX request and response headers.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class HtmxHeaders {

    // Request headers

    /**
     * {@code HX-Request} — indicates the request was made by HTMX.
     */
    public static final String HX_REQUEST = "HX-Request";

    /**
     * {@code HX-Boosted} — indicates the request is via an element with {@code hx-boost}.
     */
    public static final String HX_BOOSTED = "HX-Boosted";

    /**
     * {@code HX-Current-URL} — the current URL of the browser.
     */
    public static final String HX_CURRENT_URL = "HX-Current-URL";

    /**
     * {@code HX-History-Restore-Request} — indicates a history restoration request.
     */
    public static final String HX_HISTORY_RESTORE_REQUEST = "HX-History-Restore-Request";

    /**
     * {@code HX-Prompt} — the user response to an {@code hx-prompt}.
     */
    public static final String HX_PROMPT = "HX-Prompt";

    /**
     * {@code HX-Target} — the id of the target element.
     */
    public static final String HX_TARGET = "HX-Target";

    /**
     * {@code HX-Trigger} — the id of the triggered element.
     */
    public static final String HX_TRIGGER = "HX-Trigger";

    /**
     * {@code HX-Trigger-Name} — the name of the triggered element.
     */
    public static final String HX_TRIGGER_NAME = "HX-Trigger-Name";

    // Response headers

    /**
     * {@code HX-Location} — client-side redirect without a full page reload.
     */
    public static final String HX_LOCATION = "HX-Location";

    /**
     * {@code HX-Push-Url} — push a new URL into the history stack.
     */
    public static final String HX_PUSH_URL = "HX-Push-Url";

    /**
     * {@code HX-Replace-Url} — replace the current URL in the history stack.
     */
    public static final String HX_REPLACE_URL = "HX-Replace-Url";

    /**
     * {@code HX-Redirect} — client-side redirect that causes a full page reload.
     */
    public static final String HX_REDIRECT = "HX-Redirect";

    /**
     * {@code HX-Refresh} — instructs the client to perform a full page refresh.
     */
    public static final String HX_REFRESH = "HX-Refresh";

    /**
     * {@code HX-Reswap} — overrides the swap method used by HTMX.
     */
    public static final String HX_RESWAP = "HX-Reswap";

    /**
     * {@code HX-Retarget} — overrides the target element for the swap.
     */
    public static final String HX_RETARGET = "HX-Retarget";

    /**
     * {@code HX-Reselect} — chooses which part of the response to use in the swap.
     */
    public static final String HX_RESELECT = "HX-Reselect";

    /**
     * {@code HX-Trigger-After-Settle} — triggers client-side events after settle.
     */
    public static final String HX_TRIGGER_AFTER_SETTLE = "HX-Trigger-After-Settle";

    /**
     * {@code HX-Trigger-After-Swap} — triggers client-side events after swap.
     */
    public static final String HX_TRIGGER_AFTER_SWAP = "HX-Trigger-After-Swap";

    private HtmxHeaders() {
    }
}
