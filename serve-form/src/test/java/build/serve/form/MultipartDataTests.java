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

class MultipartDataTests {

    private static final String BOUNDARY = "----WebKitFormBoundary7MA4YWxkTrZu0gW";

    private static byte[] body(final String... lines) {
        return String.join("\r\n", lines).getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    @Test
    void shouldParseSimpleTextField() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"title\"",
            "",
            "My Song",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("title")).isPresent();
        assertThat(data.get("title").get().value()).isEqualTo("My Song");
    }

    @Test
    void shouldParseMultipleFields() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"title\"",
            "",
            "My Song",
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"genre\"",
            "",
            "Jazz",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("title").get().value()).isEqualTo("My Song");
        assertThat(data.get("genre").get().value()).isEqualTo("Jazz");
    }

    @Test
    void shouldParseFileUploadPart() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"file\"; filename=\"track.mid\"",
            "Content-Type: audio/midi",
            "",
            "MIDI_DATA",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        var part = data.get("file").orElseThrow();
        assertThat(part.filename()).contains("track.mid");
        assertThat(part.contentType()).contains("audio/midi");
        assertThat(part.value()).isEqualTo("MIDI_DATA");
    }

    @Test
    void shouldReturnEmptyForMissingField() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"title\"",
            "",
            "My Song",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("missing")).isEmpty();
    }

    @Test
    void shouldReturnAllParts() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"a\"",
            "",
            "1",
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"b\"",
            "",
            "2",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.parts()).hasSize(2);
    }

    @Test
    void shouldReturnAllPartsWithSameName() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"tag\"",
            "",
            "jazz",
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"tag\"",
            "",
            "piano",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.getAll("tag")).hasSize(2);
        assertThat(data.getAll("tag").stream().map(FormPart::value).toList())
            .containsExactly("jazz", "piano");
    }

    @Test
    void shouldStripPathTraversalFromFilename() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"file\"; filename=\"../../etc/passwd\"",
            "Content-Type: text/plain",
            "",
            "DATA",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("file").orElseThrow().filename()).contains("passwd");
    }

    @Test
    void shouldStripWindowsStylePathFromFilename() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"file\"; filename=\"C:\\\\Windows\\\\evil.exe\"",
            "Content-Type: application/octet-stream",
            "",
            "DATA",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("file").orElseThrow().filename()).contains("evil.exe");
    }

    @Test
    void shouldFallBackToPlaceholderForBareTraversalFilename() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"file\"; filename=\"../\"",
            "Content-Type: text/plain",
            "",
            "DATA",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("file").orElseThrow().filename()).contains("unnamed");
    }

    @Test
    void shouldReturnEmptyFilenameForPlainField() {
        var data = MultipartData.parse(body(
            "--" + BOUNDARY,
            "Content-Disposition: form-data; name=\"note\"",
            "",
            "hello",
            "--" + BOUNDARY + "--"
        ), BOUNDARY);

        assertThat(data.get("note").get().filename()).isEmpty();
    }
}
