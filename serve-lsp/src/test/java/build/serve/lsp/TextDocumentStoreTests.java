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
import build.serve.lsp.types.Hover;
import build.serve.lsp.types.Range;
import build.serve.lsp.types.TextDocumentContentChangeEvent;
import build.serve.lsp.types.TextDocumentIdentifier;
import build.serve.lsp.types.TextDocumentItem;
import build.serve.lsp.types.VersionedTextDocumentIdentifier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link TextDocumentStore}.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
class TextDocumentStoreTests {

    private static final String URI = "file:///test.java";

    @Test
    void shouldTrackTextOnDidOpen() {
        final var store = new TextDocumentStore();

        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "hello")));

        assertThat(store.text(URI)).contains("hello");
        assertThat(store.get(URI)).hasValueSatisfying(snap -> {
            assertThat(snap.version()).isEqualTo(1);
            assertThat(snap.languageId()).isEqualTo("java");
        });
    }

    @Test
    void shouldReturnEmptyForUnknownDocument() {
        final var store = new TextDocumentStore();

        assertThat(store.text("file:///never-opened.java")).isEmpty();
    }

    @Test
    void shouldReplaceWholeDocumentWhenChangeHasNoRange() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "old content")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(null, "new content"))));

        assertThat(store.text(URI)).contains("new content");
        assertThat(store.get(URI)).hasValueSatisfying(snap -> assertThat(snap.version()).isEqualTo(2));
    }

    @Test
    void shouldApplySingleLineIncrementalEdit() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "hello world")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(Range.of(0, 6, 0, 11), "there"))));

        assertThat(store.text(URI)).contains("hello there");
    }

    @Test
    void shouldApplyMultiLineIncrementalEdit() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "line one\nline two\nline three")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(Range.of(1, 0, 2, 5), "REPLACED "))));

        assertThat(store.text(URI)).contains("line one\nREPLACED three");
    }

    @Test
    void shouldApplyMultipleSequentialEditsInOneChangeNotification() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "abc")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(
                new TextDocumentContentChangeEvent(Range.of(0, 0, 0, 1), "X"),
                new TextDocumentContentChangeEvent(Range.of(0, 0, 0, 1), "Y"))));

        assertThat(store.text(URI)).contains("Ybc");
    }

    @Test
    void shouldHandleCrlfLineEndings() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "line one\r\nline two")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(Range.of(1, 0, 1, 4), "LINE"))));

        assertThat(store.text(URI)).contains("line one\r\nLINE two");
    }

    @Test
    void shouldForgetDocumentOnDidClose() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "hello")));

        store.didClose(new DidCloseParams(new TextDocumentIdentifier(URI)));

        assertThat(store.text(URI)).isEmpty();
        assertThat(store.openUris()).doesNotContain(URI);
    }

    @Test
    void shouldIgnoreDidCloseForUnopenedDocument() {
        final var store = new TextDocumentStore();

        store.didClose(new DidCloseParams(new TextDocumentIdentifier(URI)));

        assertThat(store.text(URI)).isEmpty();
    }

    @Test
    void shouldOverwriteExistingDocumentOnReopen() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "first")));

        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 2, "second")));

        assertThat(store.text(URI)).contains("second");
        assertThat(store.get(URI)).hasValueSatisfying(snap -> assertThat(snap.version()).isEqualTo(2));
    }

    @Test
    void shouldThrowWhenDidChangeAppliedToUnopenedDocument() {
        final var store = new TextDocumentStore();

        assertThatThrownBy(() -> store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(null, "new content")))))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldInsertTextWithZeroLengthRange() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "hello world")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(Range.of(0, 5, 0, 5), " there"))));

        assertThat(store.text(URI)).contains("hello there world");
    }

    @Test
    void shouldDeleteTextWhenReplacementIsEmpty() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem(URI, "java", 1, "hello there world")));

        store.didChange(new DidChangeParams(
            new VersionedTextDocumentIdentifier(URI, 2),
            List.of(new TextDocumentContentChangeEvent(Range.of(0, 5, 0, 11), ""))));

        assertThat(store.text(URI)).contains("hello world");
    }

    @Test
    void shouldTrackMultipleDocumentsIndependently() {
        final var store = new TextDocumentStore();
        store.didOpen(new DidOpenParams(new TextDocumentItem("file:///a.java", "java", 1, "aaa")));
        store.didOpen(new DidOpenParams(new TextDocumentItem("file:///b.java", "java", 1, "bbb")));

        assertThat(store.openUris()).containsExactlyInAnyOrder("file:///a.java", "file:///b.java");
        assertThat(store.text("file:///a.java")).contains("aaa");
        assertThat(store.text("file:///b.java")).contains("bbb");
    }

    @Test
    void shouldExposeDocumentTextThroughLspServerHandlers() throws Exception {
        final var store = new TextDocumentStore();
        final var server = LspServer.builder()
            .onDidOpen((params, ctx) -> store.didOpen(params))
            .onDidChange((params, ctx) -> store.didChange(params))
            .onHover((params, ctx) -> Hover.of(
                store.text(params.textDocument().uri()).orElse("<not open>")))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            client.sendNotification("textDocument/didOpen",
                "{\"textDocument\":{\"uri\":\"" + URI + "\",\"languageId\":\"java\",\"version\":1,\"text\":\"synced text\"}}");

            final var response = client.sendRequest(1, "textDocument/hover",
                "{\"textDocument\":{\"uri\":\"" + URI + "\"},\"position\":{\"line\":0,\"character\":0}}");

            final var value = response.get("result").asObject().get("contents").asObject().get("value").asString().value();
            assertThat(value).isEqualTo("synced text");
        }
    }
}
