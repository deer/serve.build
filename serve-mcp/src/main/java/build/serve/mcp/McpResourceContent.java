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
 * The content returned by reading an MCP resource.
 *
 * @author reed.vonredwitz
 * @since May-2026
 */
public sealed interface McpResourceContent permits McpResourceContent.Text, McpResourceContent.Blob {

    /**
     * Returns the URI of the resource that was read.
     *
     * @return the resource URI
     */
    String uri();

    /**
     * Returns the MIME type of the content.
     *
     * @return the MIME type
     */
    String mimeType();

    /**
     * Plain-text resource content.
     *
     * @param uri      the resource URI
     * @param mimeType the MIME type (e.g. {@code "text/plain"})
     * @param text     the text content
     */
    record Text(String uri, String mimeType, String text) implements McpResourceContent {
    }

    /**
     * Binary resource content, base64-encoded.
     *
     * @param uri      the resource URI
     * @param mimeType the MIME type (e.g. {@code "image/png"})
     * @param blob     the base64-encoded binary content
     */
    record Blob(String uri, String mimeType, String blob) implements McpResourceContent {
    }
}
