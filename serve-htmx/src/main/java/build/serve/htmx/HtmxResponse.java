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

import build.serve.foundation.Response;
import build.serve.template.HtmlContent;

import java.util.Objects;

/**
 * Wraps a {@link Response} to provide a fluent API for setting HTMX response headers.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class HtmxResponse {

    private final Response response;

    private HtmxResponse(final Response response) {
        this.response = Objects.requireNonNull(response, "response");
    }

    /**
     * Wraps the given {@link Response} to provide HTMX header support.
     *
     * @param response the outgoing {@link Response}
     * @return an {@link HtmxResponse} wrapping the given response
     */
    public static HtmxResponse of(final Response response) {
        return new HtmxResponse(response);
    }

    /**
     * Sets the {@code HX-Location} header to perform a client-side redirect without a full page reload.
     *
     * @param path the path to redirect to
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse location(final String path) {
        response.header(HtmxHeaders.HX_LOCATION, path);

        return this;
    }

    /**
     * Sets the {@code HX-Push-Url} header to push a new URL into the browser history stack.
     *
     * @param url the URL to push
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse pushUrl(final String url) {
        response.header(HtmxHeaders.HX_PUSH_URL, url);

        return this;
    }

    /**
     * Sets the {@code HX-Replace-Url} header to replace the current URL in the browser history stack.
     *
     * @param url the URL to replace with
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse replaceUrl(final String url) {
        response.header(HtmxHeaders.HX_REPLACE_URL, url);

        return this;
    }

    /**
     * Sets the {@code HX-Redirect} header to perform a client-side redirect that causes a full page reload.
     *
     * @param url the URL to redirect to
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse redirect(final String url) {
        response.header(HtmxHeaders.HX_REDIRECT, url);

        return this;
    }

    /**
     * Sets the {@code HX-Refresh} header to {@code "true"}, instructing the client to perform a full page
     * refresh.
     *
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse refresh() {
        response.header(HtmxHeaders.HX_REFRESH, "true");

        return this;
    }

    /**
     * Sets the {@code HX-Reswap} header to override the swap method used by HTMX.
     *
     * @param swapMethod the swap method (e.g., {@code "outerHTML"}, {@code "innerHTML"})
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse reswap(final String swapMethod) {
        response.header(HtmxHeaders.HX_RESWAP, swapMethod);

        return this;
    }

    /**
     * Sets the {@code HX-Retarget} header to override the target element for the swap.
     *
     * @param selector the CSS selector of the target element
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse retarget(final String selector) {
        response.header(HtmxHeaders.HX_RETARGET, selector);

        return this;
    }

    /**
     * Sets the {@code HX-Reselect} header to choose which part of the response is used in the swap.
     *
     * @param selector the CSS selector of the element to select from the response
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse reselect(final String selector) {
        response.header(HtmxHeaders.HX_RESELECT, selector);

        return this;
    }

    /**
     * Sets the {@code HX-Trigger} header to trigger a client-side event.
     *
     * @param eventName the event name to trigger
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse trigger(final String eventName) {
        response.header(HtmxHeaders.HX_TRIGGER, eventName);

        return this;
    }

    /**
     * Sets the {@code HX-Trigger-After-Settle} header to trigger a client-side event after the settle step.
     *
     * @param eventName the event name to trigger
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse triggerAfterSettle(final String eventName) {
        response.header(HtmxHeaders.HX_TRIGGER_AFTER_SETTLE, eventName);

        return this;
    }

    /**
     * Sets the {@code HX-Trigger-After-Swap} header to trigger a client-side event after the swap step.
     *
     * @param eventName the event name to trigger
     * @return this {@link HtmxResponse} for fluent chaining
     */
    public HtmxResponse triggerAfterSwap(final String eventName) {
        response.header(HtmxHeaders.HX_TRIGGER_AFTER_SWAP, eventName);

        return this;
    }

    /**
     * Sends the given HTML content as a {@code 200 text/html} response.
     * This is the terminal step in the fluent chain.
     *
     * @param content the content to send
     */
    public void send(final HtmlContent content) {
        response.status(200).header("Content-Type", "text/html; charset=utf-8").send(content.get());
    }
}
