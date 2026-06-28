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
 * A single message in a prompt result, carrying a role and typed content.
 *
 * <p>The role is {@code "user"} or {@code "assistant"}. Content is either
 * text or a base64-encoded image.
 *
 * @author reed.vonredwitz
 * @since Jun-2026
 */
public sealed interface McpPromptMessage permits McpPromptMessage.Text, McpPromptMessage.Image {

    /**
     * Returns the role ({@code "user"} or {@code "assistant"}).
     */
    String role();

    /**
     * Creates a user-role text message.
     *
     * @param text the text content
     * @return the message
     */
    static Text userText(final String text) {
        return new Text("user", text);
    }

    /**
     * Creates an assistant-role text message.
     *
     * @param text the text content
     * @return the message
     */
    static Text assistantText(final String text) {
        return new Text("assistant", text);
    }

    /**
     * Creates a user-role image message.
     *
     * @param data     the base64-encoded image data
     * @param mimeType the MIME type (e.g. {@code "image/png"})
     * @return the message
     */
    static Image userImage(final String data, final String mimeType) {
        return new Image("user", data, mimeType);
    }

    /**
     * A text-content prompt message.
     *
     * @param role the message role
     * @param text the text content
     */
    record Text(String role, String text) implements McpPromptMessage {
    }

    /**
     * An image-content prompt message carrying base64-encoded data.
     *
     * @param role     the message role
     * @param data     the base64-encoded image data
     * @param mimeType the MIME type (e.g. {@code "image/png"})
     */
    record Image(String role, String data, String mimeType) implements McpPromptMessage {
    }
}
