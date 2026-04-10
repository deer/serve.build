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
 * A {@link TemplateOutput} that captures rendered content into a {@link StringBuilder}.
 * <p>
 * Use {@link TemplateOutput#capture()} to create an instance.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class StringTemplateOutput implements TemplateOutput {

    private final StringBuilder buffer = new StringBuilder();

    public StringTemplateOutput() {
    }

    @Override
    public void writeContent(final String value) {
        buffer.append(value);
    }

    @Override
    public void writeContent(final char value) {
        buffer.append(value);
    }

    @Override
    public void writeContent(final char[] value, final int offset, final int count) {
        buffer.append(value, offset, count);
    }

    /**
     * Returns the captured rendered content.
     *
     * @return the accumulated output as a {@link String}
     */
    public String get() {
        return buffer.toString();
    }

    /**
     * Returns the captured rendered content.
     *
     * @return the accumulated output as a {@link String}
     */
    @Override
    public String toString() {
        return buffer.toString();
    }
}
