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

import java.util.Optional;

/**
 * A resource exposed by an MCP server that clients can list and read.
 *
 * @author reed.vonredwitz
 * @since May-2026
 */
public interface McpResource {

    /**
     * Returns the URI that uniquely identifies this resource.
     *
     * @return the resource URI
     */
    String uri();

    /**
     * Returns the human-readable name of this resource.
     *
     * @return the name
     */
    String name();

    /**
     * Returns an optional description of this resource.
     *
     * @return the description
     */
    Optional<String> description();

    /**
     * Returns the MIME type of this resource's content, if known.
     *
     * @return the MIME type
     */
    Optional<String> mimeType();

    /**
     * Reads and returns the content of this resource.
     *
     * @return the resource content
     * @throws Exception if reading fails
     */
    McpResourceContent read() throws Exception;
}
