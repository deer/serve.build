/*-
 * #%L
 * Serve Template
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
package build.serve.template;

/**
 * A source of rendered HTML content, independent of the underlying template engine.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
@FunctionalInterface
public interface HtmlContent {

    /**
     * Returns the rendered HTML string.
     * <p>
     * <strong>The returned value is written to the response verbatim, with no escaping.</strong>
     * Only implement this directly (e.g. as a lambda) for content that is already known-safe HTML
     * — typically the output of a template engine's own auto-escaping render. To wrap untrusted or
     * user-supplied text, use {@link #escaped(String)} instead, which escapes it first.
     *
     * @return rendered HTML
     */
    String get();

    /**
     * Wraps text as {@link HtmlContent}, HTML-escaping {@code &}, {@code <}, {@code >}, {@code "},
     * and {@code '} first so the result is always safe to send verbatim.
     * <p>
     * This is the correct way to include untrusted or user-supplied text in a response built with
     * {@link HtmlContent} — passing raw text directly as a lambda (e.g. {@code () -> userInput})
     * sends it unescaped and introduces an XSS vulnerability.
     *
     * @param text the untrusted text to escape
     * @return an {@link HtmlContent} that renders the escaped text
     */
    static HtmlContent escaped(final String text) {
        return () -> escapeHtml(text);
    }

    private static String escapeHtml(final String text) {
        final var result = new StringBuilder(text.length());

        for (var i = 0; i < text.length(); i++) {
            final var c = text.charAt(i);

            switch (c) {
                case '&' -> result.append("&amp;");
                case '<' -> result.append("&lt;");
                case '>' -> result.append("&gt;");
                case '"' -> result.append("&quot;");
                case '\'' -> result.append("&#39;");
                default -> result.append(c);
            }
        }

        return result.toString();
    }
}
