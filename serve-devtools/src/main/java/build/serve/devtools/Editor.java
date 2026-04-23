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

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Known editors and the URL template that opens a file at a specific line. Used by
 * {@link DevErrorHandler.Builder#editor(Editor)} to attach "open in editor" links to stack
 * traces. Templates use {@code {file}} for the absolute path and {@code {line}} for the line
 * number.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum Editor {

    /**
     * Visual Studio Code — {@code vscode://file/{file}:{line}}.
     */
    VSCODE("vscode://file/{file}:{line}"),

    /**
     * IntelliJ IDEA — {@code idea://open?file={file}&line={line}}.
     */
    INTELLIJ("idea://open?file={file}&line={line}"),

    /**
     * Zed — {@code zed://file/{file}:{line}}.
     */
    ZED("zed://file/{file}:{line}"),

    /**
     * Sublime Text — {@code subl://open?url=file://{file}&line={line}}.
     */
    SUBLIME("subl://open?url=file://{file}&line={line}");

    private final String template;

    Editor(final String template) {
        this.template = template;
    }

    /**
     * Returns the URL template for this editor.
     *
     * @return the template
     */
    public String template() {
        return template;
    }

    /**
     * Looks up an {@link Editor} by name (case-insensitive). Accepts the enum constant names
     * such as {@code "vscode"}, {@code "intellij"}, {@code "idea"} (alias), {@code "zed"},
     * {@code "sublime"}.
     *
     * @param name the editor name
     * @return the matching {@link Editor}, or empty if no match
     */
    public static Optional<Editor> fromName(final String name) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }
        final var normalized = name.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "vscode", "code" -> Optional.of(VSCODE);
            case "intellij", "idea" -> Optional.of(INTELLIJ);
            case "zed" -> Optional.of(ZED);
            case "sublime", "subl" -> Optional.of(SUBLIME);
            default -> Optional.empty();
        };
    }

    /**
     * Reads the named system property (or environment variable, fallback) and resolves it to
     * an {@link Editor}. Convenience for letting users pick an editor at launch with e.g.
     * {@code -Dserve.devtools.editor=intellij}.
     *
     * @param systemProperty the system property name
     * @return the resolved {@link Editor}, or empty if unset / unrecognised
     */
    public static Optional<Editor> fromSystemProperty(final String systemProperty) {
        Objects.requireNonNull(systemProperty, "systemProperty");
        var value = System.getProperty(systemProperty);
        if (value == null || value.isBlank()) {
            value = System.getenv(systemProperty.toUpperCase(Locale.ROOT).replace('.', '_'));
        }
        return fromName(value);
    }
}
