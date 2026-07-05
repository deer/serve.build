/*-
 * #%L
 * Serve LSP
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
package build.serve.lsp;

import build.serve.lsp.params.DidChangeParams;
import build.serve.lsp.params.DidCloseParams;
import build.serve.lsp.params.DidOpenParams;
import build.serve.lsp.types.Position;
import build.serve.lsp.types.Range;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the current text of every open document by replaying {@code textDocument/didOpen},
 * {@code didChange}, and {@code didClose} against an in-memory map. Every non-trivial LSP handler
 * (hover, completion, definition, semantic tokens, ...) needs the current document text to answer a
 * request — without this, every {@code serve-lsp} server has to reimplement incremental text sync
 * itself, including correctly applying range-based {@code TextDocumentContentChangeEvent}s.
 *
 * <p>Wire it into the notification handlers a server already registers:
 * <pre>{@code
 * var documents = new TextDocumentStore();
 * LspServer.builder()
 *     .onDidOpen((params, ctx) -> documents.didOpen(params))
 *     .onDidChange((params, ctx) -> documents.didChange(params))
 *     .onDidClose((params, ctx) -> documents.didClose(params))
 *     .onHover((params, ctx) -> {
 *         var text = documents.text(params.textDocument().uri()).orElse("");
 *         ...
 *     })
 *     .build();
 * }</pre>
 *
 * <p>Thread-safe: notifications and requests can arrive concurrently on separate virtual threads.
 *
 * <p>Line endings: positions are resolved assuming {@code \n} or {@code \r\n} line breaks, per the
 * character offsets LSP clients send by default (UTF-16 code units). Bare {@code \r} line endings
 * are not recognised as line breaks.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public final class TextDocumentStore {

    private final ConcurrentHashMap<String, TextDocumentSnapshot> documents = new ConcurrentHashMap<>();

    /**
     * Records a newly opened document.
     *
     * @param params the didOpen parameters
     */
    public void didOpen(final DidOpenParams params) {
        final var item = params.textDocument();
        documents.put(item.uri(), new TextDocumentSnapshot(item.uri(), item.languageId(), item.version(), item.text()));
    }

    /**
     * Applies one or more content changes to a tracked document, in order. Each change is either a
     * full-document replacement (no range) or an incremental edit (range present), per the LSP spec.
     *
     * @param params the didChange parameters
     */
    public void didChange(final DidChangeParams params) {
        final var uri = params.textDocument().uri();
        documents.compute(uri, (__, existing) -> {
            if (existing == null) {
                throw new IllegalStateException("textDocument/didChange for a document that was never opened: " + uri);
            }
            var text = existing.text();
            final var languageId = existing.languageId();
            for (final var change : params.contentChanges()) {
                text = change.range() != null ? applyIncremental(text, change.range(), change.text()) : change.text();
            }
            return new TextDocumentSnapshot(uri, languageId, params.textDocument().version(), text);
        });
    }

    /**
     * Discards a closed document.
     *
     * @param params the didClose parameters
     */
    public void didClose(final DidCloseParams params) {
        documents.remove(params.textDocument().uri());
    }

    /**
     * Returns the current snapshot (text, version, languageId) for an open document.
     *
     * @param uri the document URI
     * @return the snapshot, or empty if the document isn't currently open
     */
    public Optional<TextDocumentSnapshot> get(final String uri) {
        return Optional.ofNullable(documents.get(uri));
    }

    /**
     * Returns the current text of an open document.
     *
     * @param uri the document URI
     * @return the text, or empty if the document isn't currently open
     */
    public Optional<String> text(final String uri) {
        return get(uri).map(TextDocumentSnapshot::text);
    }

    /**
     * Returns the URIs of all currently open documents.
     *
     * @return an immutable snapshot of the open URI set
     */
    public Set<String> openUris() {
        return Set.copyOf(documents.keySet());
    }

    private static String applyIncremental(final String text, final Range range, final String newText) {
        final var startOffset = offsetOf(text, range.start());
        final var endOffset = offsetOf(text, range.end());
        return text.substring(0, startOffset) + newText + text.substring(endOffset);
    }

    private static int offsetOf(final String text, final Position position) {
        var line = 0;
        var i = 0;
        while (line < position.line() && i < text.length()) {
            if (text.charAt(i) == '\n') {
                line++;
            }
            i++;
        }
        return Math.min(i + position.character(), text.length());
    }
}
