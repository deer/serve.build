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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Manages a single WebSocket connection, implementing the frame protocol over raw I/O streams.
 * <p>
 * The read loop runs on a dedicated virtual thread. Writes are synchronized for thread safety.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class WebSocketConnection implements WebSocket {

    /**
     * The input stream for reading frames.
     */
    private final InputStream in;

    /**
     * The output stream for writing frames.
     */
    private final OutputStream out;

    /**
     * The remote address of the connected peer.
     */
    private final InetSocketAddress remoteAddress;

    /**
     * Whether this connection is open.
     */
    private final AtomicBoolean open;

    /**
     * Handler for incoming text frames.
     */
    private volatile Consumer<String> textHandler;

    /**
     * Handler for incoming binary frames.
     */
    private volatile Consumer<byte[]> binaryHandler;

    /**
     * Handler for incoming ping frames.
     */
    private volatile Consumer<byte[]> pingHandler;

    /**
     * Handler for incoming pong frames.
     */
    private volatile Consumer<byte[]> pongHandler;

    /**
     * Handler for the connection close event.
     */
    private volatile Runnable closeHandler;

    /**
     * Constructs a {@link WebSocketConnection}.
     *
     * @param in            the input stream for reading frames
     * @param out           the output stream for writing frames
     * @param remoteAddress the remote address of the connected peer
     */
    WebSocketConnection(final InputStream in,
                        final OutputStream out,
                        final InetSocketAddress remoteAddress) {
        this.in = in;
        this.out = out;
        this.remoteAddress = remoteAddress;
        this.open = new AtomicBoolean(true);
    }

    @Override
    public void sendText(final String text) throws IOException {
        sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_TEXT,
            text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void sendBinary(final byte[] data) throws IOException {
        sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_BINARY, data));
    }

    @Override
    public void ping(final byte[] data) throws IOException {
        sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_PING, data));
    }

    @Override
    public void pong(final byte[] data) throws IOException {
        sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_PONG, data));
    }

    @Override
    public void onText(final Consumer<String> handler) {
        this.textHandler = handler;
    }

    @Override
    public void onBinary(final Consumer<byte[]> handler) {
        this.binaryHandler = handler;
    }

    @Override
    public void onPing(final Consumer<byte[]> handler) {
        this.pingHandler = handler;
    }

    @Override
    public void onPong(final Consumer<byte[]> handler) {
        this.pongHandler = handler;
    }

    @Override
    public void onClose(final Runnable handler) {
        this.closeHandler = handler;
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public void close() throws IOException {
        if (open.compareAndSet(true, false)) {
            try {
                sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_CLOSE, new byte[0]));
            } catch (final IOException ignored) {
                // Connection may already be closed
            }

            final var handler = closeHandler;

            if (handler != null) {
                handler.run();
            }
        }
    }

    /**
     * Runs the frame reading loop. This method blocks until the connection is closed.
     * <p>
     * Handles fragmentation by accumulating continuation frames.
     */
    void readLoop() {
        try {
            int fragmentOpcode = -1;
            ByteArrayOutputStream fragmentBuffer = null;

            while (open.get()) {
                final var frame = WebSocketFrame.read(in);

                if (frame == null) {
                    break;
                }

                switch (frame.opcode()) {
                    case WebSocketFrame.OPCODE_TEXT, WebSocketFrame.OPCODE_BINARY -> {
                        if (frame.isFin()) {
                            dispatchDataFrame(frame.opcode(), frame.payload());
                        } else {
                            // Start of fragmented message
                            fragmentOpcode = frame.opcode();
                            fragmentBuffer = new ByteArrayOutputStream();
                            fragmentBuffer.write(frame.payload());
                        }
                    }

                    case WebSocketFrame.OPCODE_CONTINUATION -> {
                        if (fragmentBuffer != null) {
                            if (fragmentBuffer.size() + frame.payload().length > 16 * 1024 * 1024) {
                                throw new IOException("Fragmented WebSocket message exceeds 16 MiB limit");
                            }

                            fragmentBuffer.write(frame.payload());

                            if (frame.isFin()) {
                                dispatchDataFrame(fragmentOpcode, fragmentBuffer.toByteArray());
                                fragmentBuffer = null;
                                fragmentOpcode = -1;
                            }
                        }
                    }

                    case WebSocketFrame.OPCODE_PING -> {
                        final var handler = pingHandler;

                        if (handler != null) {
                            handler.accept(frame.payload());
                        }

                        // Auto-pong per RFC 6455
                        pong(frame.payload());
                    }

                    case WebSocketFrame.OPCODE_PONG -> {
                        final var handler = pongHandler;

                        if (handler != null) {
                            handler.accept(frame.payload());
                        }
                    }

                    case WebSocketFrame.OPCODE_CLOSE -> {
                        if (open.compareAndSet(true, false)) {
                            // Echo close frame back
                            try {
                                sendFrame(new WebSocketFrame(true, WebSocketFrame.OPCODE_CLOSE, frame.payload()));
                            } catch (final IOException ignored) {
                                // Best effort
                            }

                            final var handler = closeHandler;

                            if (handler != null) {
                                handler.run();
                            }
                        }

                        return;
                    }

                    default -> {
                        // Unknown opcode — ignore
                    }
                }
            }
        } catch (final IOException e) {
            // Connection lost
            if (open.compareAndSet(true, false)) {
                final var handler = closeHandler;

                if (handler != null) {
                    handler.run();
                }
            }
        }
    }

    /**
     * Dispatches a complete data frame to the appropriate handler.
     */
    private void dispatchDataFrame(final int opcode,
                                   final byte[] payload) {
        if (opcode == WebSocketFrame.OPCODE_TEXT) {
            final var handler = textHandler;

            if (handler != null) {
                handler.accept(new String(payload, StandardCharsets.UTF_8));
            }
        } else {
            final var handler = binaryHandler;

            if (handler != null) {
                handler.accept(payload);
            }
        }
    }

    /**
     * Sends a frame, synchronized for thread safety.
     */
    private synchronized void sendFrame(final WebSocketFrame frame) throws IOException {
        if (!open.get() && frame.opcode() != WebSocketFrame.OPCODE_CLOSE) {
            throw new IOException("WebSocket connection is closed");
        }

        frame.write(out);
    }
}
