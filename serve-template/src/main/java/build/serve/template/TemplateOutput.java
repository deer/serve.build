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

import java.io.OutputStream;
import java.io.Writer;

/**
 * Output sink for rendered template content.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface TemplateOutput {

    /**
     * Writes a string of content to this output.
     *
     * @param value the content to write
     */
    void writeContent(String value);

    /**
     * Writes a single character to this output.
     *
     * @param value the character to write
     */
    void writeContent(char value);

    /**
     * Writes a portion of a character array to this output.
     *
     * @param value  the character array
     * @param offset the start offset
     * @param count  the number of characters to write
     */
    void writeContent(char[] value, int offset, int count);

    /**
     * Writes a user-provided value, converting it to a string via {@link String#valueOf(Object)}.
     * <p>
     * Implementations may override this to apply HTML escaping or other output encoding.
     *
     * @param value the value to write
     */
    default void writeUserContent(final Object value) {
        if (value != null) {
            writeContent(String.valueOf(value));
        }
    }

    /**
     * Writes a user-provided string, potentially applying output encoding.
     *
     * @param value the string to write
     */
    default void writeUserContent(final String value) {
        if (value != null) {
            writeContent(value);
        }
    }

    /**
     * Creates a {@link TemplateOutput} that writes to the given {@link Writer}.
     * <p>
     * Any {@link java.io.IOException} from the writer is wrapped in an
     * {@link java.io.UncheckedIOException}.
     *
     * @param writer the target {@link Writer}
     * @return a new {@link TemplateOutput} backed by the writer
     */
    static TemplateOutput of(final Writer writer) {
        return new WriterTemplateOutput(writer);
    }

    /**
     * Creates a {@link TemplateOutput} that writes content to the given {@link OutputStream} using UTF-8 encoding.
     * <p>
     * Any {@link java.io.IOException} from the stream is wrapped in an {@link java.io.UncheckedIOException}.
     *
     * @param outputStream the target {@link OutputStream}
     * @return a new {@link TemplateOutput} backed by the output stream
     */
    static TemplateOutput of(final OutputStream outputStream) {
        return new OutputStreamTemplateOutput(outputStream);
    }

    /**
     * Creates a capturing {@link StringTemplateOutput} that accumulates content in memory.
     * <p>
     * Call {@link StringTemplateOutput#get()} to retrieve the captured string.
     *
     * @return a new {@link StringTemplateOutput}
     */
    static StringTemplateOutput capture() {
        return new StringTemplateOutput();
    }
}
