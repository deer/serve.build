/*-
 * #%L
 * Serve Transport (HTTP)
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
package build.serve.transport.http;

import build.serve.foundation.error.PayloadTooLargeException;

import java.io.IOException;
import java.io.InputStream;

/**
 * An {@link InputStream} wrapper that enforces a maximum byte limit.
 * Throws {@link PayloadTooLargeException} (HTTP 413) when the limit is exceeded.
 */
class LimitedInputStream extends InputStream {

    private final InputStream delegate;
    private final long maxBytes;
    private long bytesRead;

    LimitedInputStream(final InputStream delegate, final long maxBytes) {
        this.delegate = delegate;
        this.maxBytes = maxBytes;
    }

    @Override
    public int read() throws IOException {
        final int b = delegate.read();

        if (b != -1) {
            checkLimit(1);
        }

        return b;
    }

    @Override
    public int read(final byte[] buf, final int off, final int len) throws IOException {
        final int n = delegate.read(buf, off, len);

        if (n > 0) {
            checkLimit(n);
        }

        return n;
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    private void checkLimit(final long n) {
        bytesRead += n;

        if (bytesRead > maxBytes) {
            throw new PayloadTooLargeException("Request body exceeds maximum allowed size of " + maxBytes + " bytes");
        }
    }
}
