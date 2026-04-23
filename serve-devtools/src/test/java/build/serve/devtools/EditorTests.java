/*-
 * #%L
 * Serve DevTools
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
package build.serve.devtools;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EditorTests {

    private static final String PROP = "serve.devtools.editor.test";

    @AfterEach
    void clearProperty() {
        System.clearProperty(PROP);
    }

    @Test
    void shouldExposeKnownTemplates() {
        assertThat(Editor.VSCODE.template()).isEqualTo("vscode://file/{file}:{line}");
        assertThat(Editor.INTELLIJ.template()).isEqualTo("idea://open?file={file}&line={line}");
        assertThat(Editor.ZED.template()).contains("zed://");
        assertThat(Editor.SUBLIME.template()).contains("subl://");
    }

    @Test
    void shouldResolveByCanonicalName() {
        assertThat(Editor.fromName("vscode")).contains(Editor.VSCODE);
        assertThat(Editor.fromName("intellij")).contains(Editor.INTELLIJ);
        assertThat(Editor.fromName("zed")).contains(Editor.ZED);
        assertThat(Editor.fromName("sublime")).contains(Editor.SUBLIME);
    }

    @Test
    void shouldResolveByAlias() {
        assertThat(Editor.fromName("code")).contains(Editor.VSCODE);
        assertThat(Editor.fromName("idea")).contains(Editor.INTELLIJ);
        assertThat(Editor.fromName("subl")).contains(Editor.SUBLIME);
    }

    @Test
    void shouldBeCaseInsensitive() {
        assertThat(Editor.fromName("INTELLIJ")).contains(Editor.INTELLIJ);
        assertThat(Editor.fromName(" Vscode ")).contains(Editor.VSCODE);
    }

    @Test
    void shouldReturnEmptyForUnknownName() {
        assertThat(Editor.fromName("emacs")).isEmpty();
        assertThat(Editor.fromName(null)).isEmpty();
        assertThat(Editor.fromName("")).isEmpty();
    }

    @Test
    void shouldReadFromSystemProperty() {
        System.setProperty(PROP, "intellij");
        assertThat(Editor.fromSystemProperty(PROP)).contains(Editor.INTELLIJ);
    }

    @Test
    void shouldReturnEmptyWhenSystemPropertyUnset() {
        assertThat(Editor.fromSystemProperty(PROP)).isEmpty();
    }
}
