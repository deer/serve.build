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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

/**
 * Serves {@code /favicon.ico} so the browser stops emitting a 404 on every new page load.
 * <p>
 * Register with {@code .get("/favicon.ico", FaviconHandler.empty())} for a silent 204,
 * or {@code FaviconHandler.of(bytes, "image/png")} / {@link #fromClasspath(String)} for
 * a real icon.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class FaviconHandler {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CACHE_CONTROL = "Cache-Control";
    private static final String CACHE_FOR_A_DAY = "public, max-age=86400";

    private FaviconHandler() {
    }

    /**
     * A {@link Handler} that returns {@code 204 No Content}. Silences browser probes without
     * committing to any visual.
     *
     * @return a new {@link Handler}
     */
    public static Handler empty() {
        return exchange -> exchange.response().status(204).send(new byte[0]);
    }

    /**
     * A {@link Handler} that serves the specified icon bytes with the given content type.
     *
     * @param bytes       the icon bytes (defensive copy is made)
     * @param contentType the MIME type (e.g. {@code "image/png"}, {@code "image/x-icon"})
     * @return a new {@link Handler}
     */
    public static Handler of(final byte[] bytes,
                             final String contentType) {
        Objects.requireNonNull(bytes, "bytes");
        Objects.requireNonNull(contentType, "contentType");
        final byte[] copy = bytes.clone();
        return exchange -> exchange.response()
            .status(200)
            .header(CONTENT_TYPE, contentType)
            .header(CACHE_CONTROL, CACHE_FOR_A_DAY)
            .send(copy);
    }

    /**
     * A {@link Handler} that loads an icon from the classpath at startup. The content type is
     * inferred from the resource's file extension.
     *
     * @param resource the classpath resource path (e.g. {@code "/favicon.png"})
     * @return a new {@link Handler}
     * @throws IllegalArgumentException if the resource cannot be found
     */
    public static Handler fromClasspath(final String resource) {
        Objects.requireNonNull(resource, "resource");
        final byte[] bytes;
        try (var in = FaviconHandler.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalArgumentException("favicon resource not found on classpath: " + resource);
            }
            bytes = in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
        return of(bytes, contentTypeFor(resource));
    }

    private static String contentTypeFor(final String path) {
        final String lower = path.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".ico")) {
            return "image/x-icon";
        }
        if (lower.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
