/*-
 * #%L
 * Serve WebSocket
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
package build.serve.websocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

/**
 * Represents an active WebSocket connection.
 * <p>
 * Provides methods for sending frames and registering asynchronous frame listeners.
 * Each connection runs its read loop on a dedicated virtual thread.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public interface WebSocket extends AutoCloseable {

    /**
     * Sends a text frame.
     *
     * @param text the text payload
     * @throws IOException if an I/O error occurs
     */
    void sendText(String text) throws IOException;

    /**
     * Sends a binary frame.
     *
     * @param data the binary payload
     * @throws IOException if an I/O error occurs
     */
    void sendBinary(byte[] data) throws IOException;

    /**
     * Sends a ping frame.
     *
     * @param data the ping payload (at most 125 bytes)
     * @throws IOException if an I/O error occurs
     */
    void ping(byte[] data) throws IOException;

    /**
     * Sends a pong frame.
     *
     * @param data the pong payload (at most 125 bytes)
     * @throws IOException if an I/O error occurs
     */
    void pong(byte[] data) throws IOException;

    /**
     * Registers a handler for incoming text frames.
     *
     * @param handler the handler to invoke when a text frame is received
     */
    void onText(Consumer<String> handler);

    /**
     * Registers a handler for incoming binary frames.
     *
     * @param handler the handler to invoke when a binary frame is received
     */
    void onBinary(Consumer<byte[]> handler);

    /**
     * Registers a handler for incoming ping frames.
     *
     * @param handler the handler to invoke when a ping frame is received
     */
    void onPing(Consumer<byte[]> handler);

    /**
     * Registers a handler for incoming pong frames.
     *
     * @param handler the handler to invoke when a pong frame is received
     */
    void onPong(Consumer<byte[]> handler);

    /**
     * Registers a handler for the connection close event.
     *
     * @param handler the handler to invoke when the connection is closed
     */
    void onClose(Runnable handler);

    /**
     * Obtains the remote address of the connected peer.
     *
     * @return the remote {@link InetSocketAddress}
     */
    InetSocketAddress remoteAddress();

    /**
     * Returns whether this WebSocket connection is still open.
     *
     * @return {@code true} if the connection is open
     */
    boolean isOpen();
}
