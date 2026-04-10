/*-
 * #%L
 * Serve SSE
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
package build.serve.sse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Internal implementation of {@link SseEmitter} that writes SSE-formatted events
 * to an {@link OutputStream}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class SseEmitterImpl implements SseEmitter {

    private final OutputStream out;
    private volatile boolean open = true;

    /**
     * Creates a new emitter writing to the given output stream.
     *
     * @param out the output stream to write events to
     */
    SseEmitterImpl(final OutputStream out) {
        this.out = out;
    }

    @Override
    public void send(final String data) throws IOException {
        send(SseEvent.of(data));
    }

    @Override
    public synchronized void send(final SseEvent event) throws IOException {
        if (!open) {
            throw new IOException("Emitter is closed");
        }
        try {
            out.write(event.serialize().getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (final IOException e) {
            open = false;
            throw e;
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public synchronized void close() {
        if (open) {
            open = false;
            try {
                out.close();
            } catch (final IOException ignored) {
                // Best effort cleanup
            }
        }
    }
}
