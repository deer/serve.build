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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlContentTests {

    @Test
    void shouldNotEscapeRawContent() {
        HtmlContent content = () -> "<p>hello</p>";

        assertThat(content.get()).isEqualTo("<p>hello</p>");
    }

    @Test
    void escapedShouldEscapeHtmlSpecialCharacters() {
        var content = HtmlContent.escaped("<script>alert('xss')</script> & \"quoted\"");

        assertThat(content.get())
            .isEqualTo("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt; &amp; &quot;quoted&quot;");
    }

    @Test
    void escapedShouldLeavePlainTextUnchanged() {
        var content = HtmlContent.escaped("just plain text");

        assertThat(content.get()).isEqualTo("just plain text");
    }
}
