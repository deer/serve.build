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
public sealed interface McpContent permits McpContent.Text, McpContent.Image, McpContent.Resource {

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
     * @param mimeType the MIME type of the image
     */
    record Image(String data, String mimeType) implements McpContent {
    }

    /**
     * Embedded resource content (MCP spec: type=resource with blob).
     *
     * @param uri      a synthetic URI identifying the resource (e.g. "composition.mid")
     * @param mimeType the MIME type (e.g. "audio/midi", "application/pdf")
     * @param blob     base64-encoded binary data
     */
    record Resource(String uri, String mimeType, String blob) implements McpContent {
    }
}
