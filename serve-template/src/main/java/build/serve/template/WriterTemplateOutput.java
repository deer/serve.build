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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/**
 * A {@link TemplateOutput} that writes content to an underlying {@link Writer}.
 * <p>
 * Any {@link IOException} from the writer is wrapped in an {@link UncheckedIOException}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class WriterTemplateOutput implements TemplateOutput {

    private final Writer writer;

    WriterTemplateOutput(final Writer writer) {
        this.writer = writer;
    }

    @Override
    public void writeContent(final String value) {
        try {
            writer.write(value);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeContent(final char value) {
        try {
            writer.write(value);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeContent(final char[] value, final int offset, final int count) {
        try {
            writer.write(value, offset, count);
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
