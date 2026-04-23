/*-
 * #%L
 * Serve DevTools
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
package build.serve.devtools;

import build.serve.foundation.Handler;
import build.serve.foundation.util.JsonStrings;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;
import java.util.UUID;

/**
 * A {@link Handler} that responds to Chrome DevTools' workspace-discovery probe at
 * {@value #PATH}.
 * <p>
 * When DevTools is opened against a local dev server, it fetches this endpoint looking for a
 * workspace JSON document. If found, DevTools offers to map the page's source-mapped files back
 * to the filesystem so edits made in DevTools flow to the real project files.
 * <p>
 * Register it at {@link #PATH}:
 * <pre>{@code
 * RouterBuilder.create()
 *     .get(ChromeDevToolsHandler.PATH, ChromeDevToolsHandler.forWorkspace(Path.of(".")))
 *     ...
 * }</pre>
 * <p>
 * If no {@link UUID} is supplied, one is derived deterministically from the workspace's absolute
 * path so the same project keeps the same workspace identity across restarts.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class ChromeDevToolsHandler {

    /**
     * The path at which Chrome DevTools probes for the workspace document.
     */
    public static final String PATH = "/.well-known/appspecific/com.chrome.devtools.json";

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private ChromeDevToolsHandler() {
    }

    /**
     * Creates a handler that advertises the given {@code root} as the DevTools workspace with a
     * UUID derived from the root's absolute path.
     *
     * @param root the workspace root directory
     * @return a new {@link Handler}
     */
    public static Handler forWorkspace(final Path root) {
        Objects.requireNonNull(root, "root");
        return forWorkspace(root, stableUuid(root));
    }

    /**
     * Creates a handler that advertises the given {@code root} as the DevTools workspace with the
     * specified {@code uuid}.
     *
     * @param root the workspace root directory
     * @param uuid the stable workspace identifier (DevTools uses this to remember user choices)
     * @return a new {@link Handler}
     */
    public static Handler forWorkspace(final Path root,
                                       final UUID uuid) {
        Objects.requireNonNull(root, "root");
        Objects.requireNonNull(uuid, "uuid");
        final var json = buildJson(root.toAbsolutePath().normalize().toString(), uuid);
        return exchange -> exchange.response()
            .status(200)
            .header(CONTENT_TYPE, APPLICATION_JSON)
            .send(json);
    }

    private static UUID stableUuid(final Path root) {
        final var bytes = root.toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8);
        return UUID.nameUUIDFromBytes(bytes);
    }

    private static String buildJson(final String absolutePath,
                                    final UUID uuid) {
        return "{\"workspace\":{\"root\":\""
            + JsonStrings.escape(absolutePath)
            + "\",\"uuid\":\""
            + uuid
            + "\"}}";
    }
}
