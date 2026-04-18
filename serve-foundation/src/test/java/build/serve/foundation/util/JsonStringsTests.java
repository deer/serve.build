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
package build.serve.foundation.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonStringsTests {

    @Test
    void shouldLeaveCleanStringUnchanged() {
        assertThat(JsonStrings.escape("hello world")).isEqualTo("hello world");
    }

    @Test
    void shouldEscapeDoubleQuote() {
        assertThat(JsonStrings.escape("say \"hi\"")).isEqualTo("say \\\"hi\\\"");
    }

    @Test
    void shouldEscapeBackslash() {
        assertThat(JsonStrings.escape("C:\\path")).isEqualTo("C:\\\\path");
    }

    @Test
    void shouldEscapeControlCharacters() {
        assertThat(JsonStrings.escape("line\nbreak")).isEqualTo("line\\u000abreak");
        assertThat(JsonStrings.escape("tab\there")).isEqualTo("tab\\u0009here");
    }

    @Test
    void shouldHandleEmptyString() {
        assertThat(JsonStrings.escape("")).isEqualTo("");
    }

    @Test
    void shouldEscapeBothQuoteAndBackslash() {
        assertThat(JsonStrings.escape("db\"shard\\1")).isEqualTo("db\\\"shard\\\\1");
    }
}
