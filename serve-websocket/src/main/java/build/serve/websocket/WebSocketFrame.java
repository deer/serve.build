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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Represents a WebSocket frame per RFC 6455.
 * <p>
 * Provides methods for reading frames from an {@link InputStream} and writing frames
 * to an {@link OutputStream}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class WebSocketFrame {

    /**
     * Opcode for a continuation frame.
     */
    static final int OPCODE_CONTINUATION = 0x0;

    /**
     * Opcode for a text frame.
     */
    static final int OPCODE_TEXT = 0x1;

    /**
     * Opcode for a binary frame.
     */
    static final int OPCODE_BINARY = 0x2;

    /**
     * Opcode for a connection close frame.
     */
    static final int OPCODE_CLOSE = 0x8;

    /**
     * Opcode for a ping frame.
     */
    static final int OPCODE_PING = 0x9;

    /**
     * Opcode for a pong frame.
     */
    static final int OPCODE_PONG = 0xA;

    /**
     * Whether this is the final fragment.
     */
    private final boolean fin;

    /**
     * The frame opcode.
     */
    private final int opcode;

    /**
     * The frame payload.
     */
    private final byte[] payload;

    /**
     * Constructs a {@link WebSocketFrame}.
     *
     * @param fin     whether this is the final fragment
     * @param opcode  the frame opcode
     * @param payload the frame payload
     */
    WebSocketFrame(final boolean fin,
                   final int opcode,
                   final byte[] payload) {
        this.fin = fin;
        this.opcode = opcode;
        this.payload = payload;
    }

    /**
     * Returns whether this is the final fragment.
     *
     * @return {@code true} if this is the final fragment
     */
    boolean isFin() {
        return fin;
    }

    /**
     * Returns the frame opcode.
     *
     * @return the opcode
     */
    int opcode() {
        return opcode;
    }

    /**
     * Returns the frame payload.
     *
     * @return the payload bytes
     */
    byte[] payload() {
        return payload;
    }

    /**
     * Returns the payload as a UTF-8 string.
     *
     * @return the payload string
     */
    String payloadAsText() {
        return new String(payload, StandardCharsets.UTF_8);
    }

    /**
     * Reads a single WebSocket frame from the specified {@link InputStream}.
     *
     * @param in the input stream to read from
     * @return the parsed frame, or {@code null} if the stream has ended
     * @throws IOException if an I/O error occurs or the frame is malformed
     */
    static WebSocketFrame read(final InputStream in) throws IOException {
        final int first = in.read();

        if (first == -1) {
            return null;
        }

        final boolean fin = (first & 0x80) != 0;
        final int opcode = first & 0x0F;

        final int second = readByte(in);
        final boolean masked = (second & 0x80) != 0;
        long payloadLength = second & 0x7F;

        if (payloadLength == 126) {
            payloadLength = ((long) readByte(in) << 8) | readByte(in);
        } else if (payloadLength == 127) {
            payloadLength = 0;

            for (int i = 0; i < 8; i++) {
                payloadLength = (payloadLength << 8) | readByte(in);
            }
        }

        if (payloadLength > 16 * 1024 * 1024) {
            throw new IOException("WebSocket frame too large: " + payloadLength);
        }

        final byte[] maskKey;

        if (masked) {
            maskKey = readBytes(in, 4);
        } else {
            maskKey = null;
        }

        final byte[] payload = readBytes(in, (int) payloadLength);

        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] ^= maskKey[i % 4];
            }
        }

        return new WebSocketFrame(fin, opcode, payload);
    }

    /**
     * Writes this frame to the specified {@link OutputStream}.
     * <p>
     * Server frames are not masked per RFC 6455.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    void write(final OutputStream out) throws IOException {
        int first = opcode;

        if (fin) {
            first |= 0x80;
        }

        out.write(first);

        if (payload.length < 126) {
            out.write(payload.length);
        } else if (payload.length <= 0xFFFF) {
            out.write(126);
            out.write((payload.length >> 8) & 0xFF);
            out.write(payload.length & 0xFF);
        } else {
            out.write(127);

            for (int i = 7; i >= 0; i--) {
                out.write((int) ((payload.length >> (8 * i)) & 0xFF));
            }
        }

        out.write(payload);
        out.flush();
    }

    /**
     * Reads a single byte, throwing if the stream has ended.
     */
    private static int readByte(final InputStream in) throws IOException {
        final int b = in.read();

        if (b == -1) {
            throw new IOException("Unexpected end of WebSocket stream");
        }

        return b;
    }

    /**
     * Reads exactly the specified number of bytes.
     */
    private static byte[] readBytes(final InputStream in,
                                    final int count) throws IOException {
        final var bytes = new byte[count];
        int offset = 0;

        while (offset < count) {
            final int read = in.read(bytes, offset, count - offset);

            if (read == -1) {
                throw new IOException("Unexpected end of WebSocket stream");
            }

            offset += read;
        }

        return bytes;
    }
}
