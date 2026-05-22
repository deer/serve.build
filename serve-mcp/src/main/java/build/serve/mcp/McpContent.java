/*-
 * #%L
 * Serve MCP
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
package build.serve.mcp;

/**
 * Represents content returned from an MCP tool invocation.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public sealed interface McpContent permits McpContent.Text, McpContent.Image, McpContent.Audio, McpContent.Resource {

    /**
     * Text content.
     *
     * @param text the text
     */
    record Text(String text) implements McpContent {
    }

    /**
     * Base64-encoded image content.
     *
     * @param data     the base64-encoded image data
     * @param mimeType the MIME type of the image (e.g. {@code "image/png"})
     */
    record Image(String data, String mimeType) implements McpContent {
    }

    /**
     * Base64-encoded audio content.
     *
     * @param data     the base64-encoded audio data
     * @param mimeType the MIME type of the audio (e.g. {@code "audio/wav"})
     */
    record Audio(String data, String mimeType) implements McpContent {
    }

    /**
     * An embedded resource in a tool result, carrying either text or binary content.
     *
     * @param content the resource content (text or blob)
     */
    record Resource(McpResourceContent content) implements McpContent {

        /**
         * Creates an embedded text resource.
         *
         * @param uri      a URI identifying the resource
         * @param mimeType the MIME type (e.g. {@code "text/plain"})
         * @param text     the text content
         * @return the embedded resource
         */
        public static Resource text(final String uri, final String mimeType, final String text) {
            return new Resource(new McpResourceContent.Text(uri, mimeType, text));
        }

        /**
         * Creates an embedded binary resource.
         *
         * @param uri      a URI identifying the resource
         * @param mimeType the MIME type (e.g. {@code "audio/midi"})
         * @param blob     base64-encoded binary data
         * @return the embedded resource
         */
        public static Resource blob(final String uri, final String mimeType, final String blob) {
            return new Resource(new McpResourceContent.Blob(uri, mimeType, blob));
        }
    }
}
