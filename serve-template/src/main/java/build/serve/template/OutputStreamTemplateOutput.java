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
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * A {@link TemplateOutput} that writes content to an underlying {@link OutputStream} using UTF-8 encoding.
 * <p>
 * Any {@link IOException} from the stream is wrapped in an {@link UncheckedIOException}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class OutputStreamTemplateOutput implements TemplateOutput {

    private final OutputStream outputStream;

    OutputStreamTemplateOutput(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void writeContent(final String value) {
        try {
            outputStream.write(value.getBytes(StandardCharsets.UTF_8));
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    @Override
    public void writeContent(final char value) {
        writeContent(String.valueOf(value));
    }

    @Override
    public void writeContent(final char[] value, final int offset, final int count) {
        writeContent(new String(value, offset, count));
    }
}
