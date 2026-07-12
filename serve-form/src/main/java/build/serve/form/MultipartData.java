/*-
 * #%L
 * Serve Form
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
package build.serve.form;

import build.serve.foundation.Request;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Parsed {@code multipart/form-data} request body.
 * <p>
 * Obtain an instance from an inbound {@link Request}:
 * <pre>{@code
 * var multipart = MultipartData.from(exchange.request());
 * var file = multipart.get("avatar");
 * var text = multipart.get("description").map(FormPart::value).orElse("");
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class MultipartData {

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("boundary=([^;]+)");
    private static final Pattern NAME_PATTERN = Pattern.compile("name=\"([^\"]+)\"");
    private static final Pattern FILENAME_PATTERN = Pattern.compile("filename=\"([^\"]*)\"");

    private final List<FormPart> parts;

    private MultipartData(final List<FormPart> parts) {
        this.parts = Collections.unmodifiableList(parts);
    }

    /**
     * Parses multipart form data from the given {@link Request}.
     * <p>
     * Reads the {@code Content-Type} header to extract the boundary.
     *
     * @param request the inbound request
     * @return the parsed {@link MultipartData}
     * @throws IllegalArgumentException if the Content-Type header is missing or has no boundary
     */
    public static MultipartData from(final Request request) {
        final var contentType = request.header("Content-Type")
            .orElseThrow(() -> new IllegalArgumentException("Missing Content-Type header"));

        final var boundaryMatcher = BOUNDARY_PATTERN.matcher(contentType);
        if (!boundaryMatcher.find()) {
            throw new IllegalArgumentException("No boundary found in Content-Type: " + contentType);
        }

        final var boundary = boundaryMatcher.group(1).strip();
        try {
            return parse(request.bodyAsStream().readAllBytes(), boundary);
        } catch (final java.io.IOException e) {
            throw new RuntimeException("Failed to read multipart body", e);
        }
    }

    /**
     * Parses multipart form data from raw bytes with the given boundary.
     *
     * @param body     the raw multipart body
     * @param boundary the multipart boundary string
     * @return the parsed {@link MultipartData}
     */
    public static MultipartData parse(final byte[] body, final String boundary) {
        final var parts = new ArrayList<FormPart>();
        final var delimiter = ("--" + boundary).getBytes(StandardCharsets.ISO_8859_1);
        final var bodyStr = new String(body, StandardCharsets.ISO_8859_1);
        final var delimStr = "--" + boundary;

        var pos = 0;

        while (pos < bodyStr.length()) {
            final var delimIdx = bodyStr.indexOf(delimStr, pos);
            if (delimIdx < 0) {
                break;
            }

            pos = delimIdx + delimiter.length;

            // End boundary
            if (pos < bodyStr.length() && bodyStr.startsWith("--", pos)) {
                break;
            }

            // Skip CRLF after boundary
            if (pos < bodyStr.length() && bodyStr.startsWith("\r\n", pos)) {
                pos += 2;
            } else if (pos < bodyStr.length() && bodyStr.charAt(pos) == '\n') {
                pos += 1;
            } else {
                break;
            }

            // Find end of headers (blank line)
            final var headerEnd = bodyStr.indexOf("\r\n\r\n", pos);
            if (headerEnd < 0) {
                break;
            }

            final var headerSection = bodyStr.substring(pos, headerEnd);
            pos = headerEnd + 4;

            // Find start of next boundary
            final var nextDelim = bodyStr.indexOf("\r\n" + delimStr, pos);
            final var partEnd = nextDelim >= 0 ? nextDelim : bodyStr.indexOf(delimStr, pos);
            if (partEnd < 0) {
                break;
            }

            final var partBytes = bodyStr.substring(pos, partEnd).getBytes(StandardCharsets.ISO_8859_1);
            pos = partEnd;

            // Parse headers
            String name = null;
            Optional<String> filename = Optional.empty();
            Optional<String> contentType = Optional.empty();

            for (final var headerLine : headerSection.split("\r\n")) {
                final var lower = headerLine.toLowerCase();
                if (lower.startsWith("content-disposition:")) {
                    final var nameMatcher = NAME_PATTERN.matcher(headerLine);
                    if (nameMatcher.find()) {
                        name = nameMatcher.group(1);
                    }
                    final var filenameMatcher = FILENAME_PATTERN.matcher(headerLine);
                    if (filenameMatcher.find()) {
                        filename = Optional.of(sanitizeFilename(filenameMatcher.group(1)));
                    }
                } else if (lower.startsWith("content-type:")) {
                    contentType = Optional.of(headerLine.substring("content-type:".length()).strip());
                }
            }

            if (name != null) {
                parts.add(new FormPart(name, filename, contentType, partBytes));
            }
        }

        return new MultipartData(parts);
    }

    /**
     * Strips any path components from a client-supplied filename, keeping only the final segment.
     * <p>
     * The {@code Content-Disposition} filename is attacker-controlled and, unsanitized, is a
     * classic path-traversal footgun for any consumer that saves the upload using that filename
     * directly (e.g. {@code Files.write(uploadDir.resolve(part.filename()), part.bytes())}).
     * Handles both {@code /} and {@code \} separators, and falls back to a safe placeholder if
     * nothing but path components or dots remain.
     *
     * @param rawFilename the filename as supplied by the client
     * @return the sanitized filename, containing no path separators
     */
    private static String sanitizeFilename(final String rawFilename) {
        final var normalized = rawFilename.replace('\\', '/');
        final var lastSlash = normalized.lastIndexOf('/');
        final var basename = lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;

        if (basename.isBlank() || basename.equals(".") || basename.equals("..")) {
            return "unnamed";
        }

        return basename;
    }

    /**
     * Returns the first part with the given name, or empty if absent.
     *
     * @param name the field name
     * @return the first matching {@link FormPart}, or empty
     */
    public Optional<FormPart> get(final String name) {
        return parts.stream().filter(p -> p.name().equals(name)).findFirst();
    }

    /**
     * Returns all parts with the given name.
     *
     * @param name the field name
     * @return all matching parts
     */
    public List<FormPart> getAll(final String name) {
        return parts.stream().filter(p -> p.name().equals(name)).toList();
    }

    /**
     * Returns all parts in this multipart body.
     *
     * @return all {@link FormPart}s
     */
    public List<FormPart> parts() {
        return parts;
    }
}
