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
import build.serve.lsp.params.DidSaveParams;
import build.serve.lsp.params.ReferenceParams;
import build.serve.lsp.params.RenameParams;
import build.serve.lsp.params.TextDocumentPositionParams;
import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.Location;
import build.serve.lsp.types.Range;
import build.serve.lsp.types.ShowMessageParams;
import build.serve.lsp.types.TextEdit;
import build.serve.lsp.types.WorkspaceEdit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for LSP request/notification handlers not covered by {@link LspTransportTests}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
class LspHandlersTests {

    private static final String POSITION_PARAMS =
        "{\"textDocument\":{\"uri\":\"file:///test.java\"},\"position\":{\"line\":5,\"character\":10}}";

    @Test
    void shouldReturnDefinitionLocation() throws Exception {
        final var server = LspServer.builder()
            .onDefinition((params, ctx) -> new Location(
                "file:///other.java",
                Range.of(10, 0, 10, 15)))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);

            final var result = response.get("result");
            assertThat(result.path("uri").asText()).isEqualTo("file:///other.java");
            assertThat(result.path("range").path("start").path("line").asInt()).isEqualTo(10);
            assertThat(result.path("range").path("start").path("character").asInt()).isEqualTo(0);
            assertThat(result.path("range").path("end").path("character").asInt()).isEqualTo(15);
        }
    }

    @Test
    void shouldReturnMultipleReferences() throws Exception {
        final var server = LspServer.builder()
            .onReferences((params, ctx) -> List.of(
                new Location("file:///a.java", Range.of(1, 0, 1, 5)),
                new Location("file:///b.java", Range.of(3, 4, 3, 9)),
                new Location("file:///c.java", Range.of(7, 2, 7, 7))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/references",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"position\":{\"line\":0,\"character\":0},"
                    + "\"includeDeclaration\":true}");

            final var result = response.get("result");
            assertThat(result.isArray()).isTrue();
            assertThat(result.size()).isEqualTo(3);
            assertThat(result.get(0).path("uri").asText()).isEqualTo("file:///a.java");
            assertThat(result.get(1).path("uri").asText()).isEqualTo("file:///b.java");
            assertThat(result.get(2).path("range").path("start").path("line").asInt()).isEqualTo(7);
        }
    }

    @Test
    void shouldRenameSymbolAcrossFiles() throws Exception {
        final var server = LspServer.builder()
            .onRename((params, ctx) -> {
                assertThat(params.newName()).isEqualTo("newMethodName");
                final var edit = new TextEdit(Range.of(5, 10, 5, 20), params.newName());
                return new WorkspaceEdit(Map.of("file:///test.java", List.of(edit)));
            })
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/rename",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"position\":{\"line\":5,\"character\":10},"
                    + "\"newName\":\"newMethodName\"}");

            final var result = response.get("result");
            assertThat(result.path("changes").has("file:///test.java")).isTrue();
            final var edits = result.path("changes").path("file:///test.java");
            assertThat(edits.size()).isEqualTo(1);
            assertThat(edits.get(0).path("newText").asText()).isEqualTo("newMethodName");
        }
    }

    @Test
    void shouldReturnNullResultWhenNoHandlerRegistered() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);

            // No handler registered — result should be null, not an error
            assertThat(response.has("error")).isFalse();
            assertThat(response.path("result").isNull()).isTrue();
        }
    }

    @Test
    void shouldPublishDiagnosticsOnDidChange() throws Exception {
        final var server = LspServer.builder()
            .onDidChange((params, ctx) ->
                ctx.publishDiagnostics(params.textDocument().uri(), List.of(
                    Diagnostic.warning(Range.of(2, 0, 2, 10), "Unused variable"))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            client.sendNotification("textDocument/didChange",
                "{\"textDocument\":{\"uri\":\"file:///test.java\",\"version\":2},"
                    + "\"contentChanges\":[{\"text\":\"updated content\"}]}");

            final var notification = client.readMessage();
            assertThat(notification.path("method").asText()).isEqualTo("textDocument/publishDiagnostics");

            final var diag = notification.path("params").path("diagnostics").get(0);
            assertThat(diag.path("message").asText()).isEqualTo("Unused variable");
            assertThat(diag.path("severity").asInt()).isEqualTo(2); // WARNING = 2
        }
    }

    @Test
    void shouldClearDiagnosticsOnDidClose() throws Exception {
        final var server = LspServer.builder()
            .onDidClose((params, ctx) ->
                ctx.publishDiagnostics(params.textDocument().uri(), List.of()))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            client.sendNotification("textDocument/didClose",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"}}");

            final var notification = client.readMessage();
            assertThat(notification.path("method").asText()).isEqualTo("textDocument/publishDiagnostics");
            assertThat(notification.path("params").path("diagnostics").size()).isEqualTo(0);
        }
    }

    @Test
    void shouldSendShowMessageNotification() throws Exception {
        final var server = LspServer.builder()
            .onDidSave((params, ctx) ->
                ctx.showMessage(ShowMessageParams.info("File saved successfully")))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            client.sendNotification("textDocument/didSave",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"}}");

            final var notification = client.readMessage();
            assertThat(notification.path("method").asText()).isEqualTo("window/showMessage");
            assertThat(notification.path("params").path("message").asText())
                .isEqualTo("File saved successfully");
            assertThat(notification.path("params").path("type").asInt()).isEqualTo(3); // Info
        }
    }

    @Test
    void shouldHandleMultipleRequestsInSequence() throws Exception {
        final var server = LspServer.builder()
            .onDefinition((params, ctx) -> new Location("file:///def.java", Range.of(0, 0, 0, 5)))
            .onReferences((params, ctx) -> List.of(
                new Location("file:///ref.java", Range.of(1, 0, 1, 5))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var def = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);
            final var refs = client.sendRequest(2, "textDocument/references",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"position\":{\"line\":0,\"character\":0},"
                    + "\"includeDeclaration\":false}");

            assertThat(def.path("id").asInt()).isEqualTo(1);
            assertThat(def.path("result").path("uri").asText()).isEqualTo("file:///def.java");

            assertThat(refs.path("id").asInt()).isEqualTo(2);
            assertThat(refs.path("result").get(0).path("uri").asText()).isEqualTo("file:///ref.java");
        }
    }
}
