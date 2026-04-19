/*-
 * #%L
 * Serve Form
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
package build.serve.form;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FormDataTests {

    @Test
    void shouldParseSingleField() {
        var form = FormData.parse("name=Alice");
        assertThat(form.get("name")).contains("Alice");
    }

    @Test
    void shouldParseMultipleFields() {
        var form = FormData.parse("name=Alice&age=30");
        assertThat(form.get("name")).contains("Alice");
        assertThat(form.get("age")).contains("30");
    }

    @Test
    void shouldReturnEmptyForMissingField() {
        var form = FormData.parse("name=Alice");
        assertThat(form.get("missing")).isEmpty();
    }

    @Test
    void shouldDecodeUrlEncodedValues() {
        var form = FormData.parse("name=Hello+World&city=New+York");
        assertThat(form.get("name")).contains("Hello World");
        assertThat(form.get("city")).contains("New York");
    }

    @Test
    void shouldDecodePercentEncodedValues() {
        var form = FormData.parse("email=user%40example.com");
        assertThat(form.get("email")).contains("user@example.com");
    }

    @Test
    void shouldCollectMultipleValuesForSameName() {
        var form = FormData.parse("tag=java&tag=music&tag=web");
        assertThat(form.getAll("tag")).containsExactly("java", "music", "web");
    }

    @Test
    void shouldReturnFirstValueForRepeatedField() {
        var form = FormData.parse("tag=java&tag=music");
        assertThat(form.get("tag")).contains("java");
    }

    @Test
    void shouldHandleEmptyBody() {
        var form = FormData.parse("");
        assertThat(form.fields()).isEmpty();
    }

    @Test
    void shouldHandleFieldWithNoValue() {
        var form = FormData.parse("flag=");
        assertThat(form.get("flag")).contains("");
    }

    @Test
    void shouldHandleFieldWithNoEquals() {
        var form = FormData.parse("flag");
        assertThat(form.get("flag")).contains("");
    }

    @Test
    void shouldExposeAllFields() {
        var form = FormData.parse("a=1&b=2");
        assertThat(form.fields()).containsOnlyKeys("a", "b");
    }
}
