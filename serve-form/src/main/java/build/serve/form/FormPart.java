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

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * A single part from a {@code multipart/form-data} request body.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class FormPart {

    private final String name;
    private final Optional<String> filename;
    private final Optional<String> contentType;
    private final byte[] bytes;

    FormPart(final String name,
             final Optional<String> filename,
             final Optional<String> contentType,
             final byte[] bytes) {
        this.name = name;
        this.filename = filename;
        this.contentType = contentType;
        this.bytes = bytes.clone();
    }

    /**
     * The form field name from the {@code Content-Disposition} header.
     *
     * @return the field name
     */
    public String name() {
        return name;
    }

    /**
     * The client-supplied filename, if this part represents a file upload.
     * <p>
     * Any path components the client embedded (e.g. {@code ../../etc/passwd}) have already been
     * stripped, so this is always a bare filename — safe to use as the final path segment when
     * saving the upload, but callers should still resolve it against a known directory rather than
     * trusting it as a full path.
     *
     * @return the filename, or empty for plain form fields
     */
    public Optional<String> filename() {
        return filename;
    }

    /**
     * The {@code Content-Type} of this part, if declared.
     *
     * @return the content type, or empty if not specified
     */
    public Optional<String> contentType() {
        return contentType;
    }

    /**
     * The raw bytes of this part's body.
     *
     * @return the part body as a byte array
     */
    public byte[] bytes() {
        return bytes.clone();
    }

    /**
     * The part body decoded as a UTF-8 string.
     *
     * @return the part body as a string
     */
    public String value() {
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
