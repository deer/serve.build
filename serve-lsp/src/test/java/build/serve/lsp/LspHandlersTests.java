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

import build.base.json.JsonNull;
import build.serve.lsp.types.CallHierarchyIncomingCall;
import build.serve.lsp.types.CallHierarchyItem;
import build.serve.lsp.types.CallHierarchyOutgoingCall;
import build.serve.lsp.types.CodeAction;
import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.DiagnosticSeverity;
import build.serve.lsp.types.Location;
import build.serve.lsp.types.Range;
import build.serve.lsp.types.ShowMessageParams;
import build.serve.lsp.types.SymbolKind;
import build.serve.lsp.types.TextEdit;
import build.serve.lsp.types.TypeHierarchyItem;
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
            .onDefinition((params, ctx) -> List.of(new Location(
                "file:///other.java",
                Range.of(10, 0, 10, 15))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);

            final var result = response.get("result").asArray().values().get(0).asObject();
            assertThat(result.get("uri").asString().value()).isEqualTo("file:///other.java");
            assertThat(result.get("range").asObject().get("start").asObject().get("line").asNumber().toNumber().intValue()).isEqualTo(10);
            assertThat(result.get("range").asObject().get("start").asObject().get("character").asNumber().toNumber().intValue()).isEqualTo(0);
            assertThat(result.get("range").asObject().get("end").asObject().get("character").asNumber().toNumber().intValue()).isEqualTo(15);
        }
    }

    @Test
    void shouldReturnMultipleDefinitionLocations() throws Exception {
        final var server = LspServer.builder()
            .onDefinition((params, ctx) -> List.of(
                new Location("file:///header.java", Range.of(1, 0, 1, 5)),
                new Location("file:///impl.java", Range.of(9, 4, 9, 9))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(2);
            assertThat(result.values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///header.java");
            assertThat(result.values().get(1).asObject().get("uri").asString().value()).isEqualTo("file:///impl.java");
        }
    }

    @Test
    void shouldReturnMultipleDeclarationLocations() throws Exception {
        final var server = LspServer.builder()
            .onDeclaration((params, ctx) -> List.of(
                new Location("file:///a.java", Range.of(1, 0, 1, 5)),
                new Location("file:///b.java", Range.of(2, 0, 2, 5))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/declaration", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(2);
            assertThat(result.values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///a.java");
            assertThat(result.values().get(1).asObject().get("uri").asString().value()).isEqualTo("file:///b.java");
        }
    }

    @Test
    void shouldReturnMultipleTypeDefinitionLocations() throws Exception {
        final var server = LspServer.builder()
            .onTypeDefinition((params, ctx) -> List.of(
                new Location("file:///a.java", Range.of(1, 0, 1, 5)),
                new Location("file:///b.java", Range.of(2, 0, 2, 5))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/typeDefinition", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(2);
            assertThat(result.values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///a.java");
            assertThat(result.values().get(1).asObject().get("uri").asString().value()).isEqualTo("file:///b.java");
        }
    }

    @Test
    void shouldReturnMultipleImplementationLocations() throws Exception {
        final var server = LspServer.builder()
            .onImplementation((params, ctx) -> List.of(
                new Location("file:///a.java", Range.of(1, 0, 1, 5)),
                new Location("file:///b.java", Range.of(2, 0, 2, 5))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/implementation", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(2);
            assertThat(result.values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///a.java");
            assertThat(result.values().get(1).asObject().get("uri").asString().value()).isEqualTo("file:///b.java");
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

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(3);
            assertThat(result.values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///a.java");
            assertThat(result.values().get(1).asObject().get("uri").asString().value()).isEqualTo("file:///b.java");
            assertThat(result.values().get(2).asObject().get("range").asObject().get("start").asObject().get("line").asNumber().toNumber().intValue()).isEqualTo(7);
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

            final var result = response.get("result").asObject();
            assertThat(result.get("changes").asObject().has("file:///test.java")).isTrue();
            final var edits = result.get("changes").asObject().get("file:///test.java").asArray();
            assertThat(edits.values()).hasSize(1);
            assertThat(edits.values().get(0).asObject().get("newText").asString().value()).isEqualTo("newMethodName");
        }
    }

    @Test
    void shouldReturnNullResultWhenNoHandlerRegistered() throws Exception {
        final var server = LspServer.builder().build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);

            assertThat(response.has("error")).isFalse();
            assertThat(response.get("result")).isInstanceOf(JsonNull.class);
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
            assertThat(notification.get("method").asString().value()).isEqualTo("textDocument/publishDiagnostics");

            final var diag = notification.get("params").asObject().get("diagnostics").asArray().values().get(0).asObject();
            assertThat(diag.get("message").asString().value()).isEqualTo("Unused variable");
            assertThat(diag.get("severity").asNumber().toNumber().intValue()).isEqualTo(2);
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
            assertThat(notification.get("method").asString().value()).isEqualTo("textDocument/publishDiagnostics");
            assertThat(notification.get("params").asObject().get("diagnostics").asArray().values()).isEmpty();
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
            assertThat(notification.get("method").asString().value()).isEqualTo("window/showMessage");
            assertThat(notification.get("params").asObject().get("message").asString().value())
                .isEqualTo("File saved successfully");
            assertThat(notification.get("params").asObject().get("type").asNumber().toNumber().intValue()).isEqualTo(3);
        }
    }

    @Test
    void shouldParseCodeActionContextDiagnostics() throws Exception {
        final var server = LspServer.builder()
            .onCodeAction((params, ctx) -> {
                assertThat(params.context()).hasSize(1);
                assertThat(params.context().get(0).message()).isEqualTo("Unused import");
                assertThat(params.context().get(0).severity()).isEqualTo(DiagnosticSeverity.WARNING);
                return List.of(new CodeAction("Remove unused import", "quickfix", null, null, null));
            })
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/codeAction",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":0,\"character\":0}},"
                    + "\"context\":{\"diagnostics\":[{\"range\":{\"start\":{\"line\":0,\"character\":0},"
                    + "\"end\":{\"line\":0,\"character\":10}},\"severity\":2,\"message\":\"Unused import\"}]}}");

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            assertThat(result.values().get(0).asObject().getString("title")).isEqualTo("Remove unused import");
        }
    }

    @Test
    void shouldHandleCodeActionWithEmptyContext() throws Exception {
        final var server = LspServer.builder()
            .onCodeAction((params, ctx) -> {
                assertThat(params.context()).isEmpty();
                return List.of();
            })
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/codeAction",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":0,\"character\":0}},"
                    + "\"context\":{\"diagnostics\":[]}}");

            assertThat(response.has("error")).isFalse();
        }
    }

    @Test
    void shouldHandleMultipleRequestsInSequence() throws Exception {
        final var server = LspServer.builder()
            .onDefinition((params, ctx) -> List.of(new Location("file:///def.java", Range.of(0, 0, 0, 5))))
            .onReferences((params, ctx) -> List.of(
                new Location("file:///ref.java", Range.of(1, 0, 1, 5))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var def = client.sendRequest(1, "textDocument/definition", POSITION_PARAMS);
            final var refs = client.sendRequest(2, "textDocument/references",
                "{\"textDocument\":{\"uri\":\"file:///test.java\"},"
                    + "\"position\":{\"line\":0,\"character\":0},"
                    + "\"includeDeclaration\":false}");

            assertThat(def.get("id").asNumber().toNumber().intValue()).isEqualTo(1);
            assertThat(def.get("result").asArray().values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///def.java");

            assertThat(refs.get("id").asNumber().toNumber().intValue()).isEqualTo(2);
            assertThat(refs.get("result").asArray().values().get(0).asObject().get("uri").asString().value()).isEqualTo("file:///ref.java");
        }
    }

    @Test
    void shouldReturnPrepareCallHierarchyItems() throws Exception {
        final var server = LspServer.builder()
            .onPrepareCallHierarchy((params, ctx) -> List.of(
                new CallHierarchyItem("doWork", SymbolKind.METHOD, "void doWork()", "file:///a.java",
                    Range.of(4, 0, 4, 20), Range.of(4, 9, 4, 15), null)))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/prepareCallHierarchy", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            final var item = result.values().get(0).asObject();
            assertThat(item.get("name").asString().value()).isEqualTo("doWork");
            assertThat(item.get("kind").asNumber().toNumber().intValue()).isEqualTo(SymbolKind.METHOD.value());
            assertThat(item.get("detail").asString().value()).isEqualTo("void doWork()");
            assertThat(item.get("uri").asString().value()).isEqualTo("file:///a.java");
            assertThat(item.has("data")).isFalse();
        }
    }

    @Test
    void shouldRoundTripCallHierarchyItemDataThroughIncomingCalls() throws Exception {
        final var server = LspServer.builder()
            .onCallHierarchyIncomingCalls((params, ctx) -> {
                assertThat(params.item().name()).isEqualTo("doWork");
                assertThat(params.item().data().asString().value()).isEqualTo("opaque-id");
                return List.of(new CallHierarchyIncomingCall(
                    new CallHierarchyItem("caller", SymbolKind.METHOD, null, "file:///b.java",
                        Range.of(0, 0, 0, 10), Range.of(0, 0, 0, 6), null),
                    List.of(Range.of(1, 4, 1, 10))));
            })
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "callHierarchy/incomingCalls",
                "{\"item\":{\"name\":\"doWork\",\"kind\":6,\"uri\":\"file:///a.java\","
                    + "\"range\":{\"start\":{\"line\":4,\"character\":0},\"end\":{\"line\":4,\"character\":20}},"
                    + "\"selectionRange\":{\"start\":{\"line\":4,\"character\":9},\"end\":{\"line\":4,\"character\":15}},"
                    + "\"data\":\"opaque-id\"}}");

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            final var call = result.values().get(0).asObject();
            assertThat(call.get("from").asObject().get("name").asString().value()).isEqualTo("caller");
            assertThat(call.get("fromRanges").asArray().values()).hasSize(1);
        }
    }

    @Test
    void shouldReturnCallHierarchyOutgoingCalls() throws Exception {
        final var server = LspServer.builder()
            .onCallHierarchyOutgoingCalls((params, ctx) -> List.of(new CallHierarchyOutgoingCall(
                new CallHierarchyItem("callee", SymbolKind.METHOD, null, "file:///c.java",
                    Range.of(2, 0, 2, 10), Range.of(2, 0, 2, 6), null),
                List.of(Range.of(5, 4, 5, 10)))))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "callHierarchy/outgoingCalls",
                "{\"item\":{\"name\":\"doWork\",\"kind\":6,\"uri\":\"file:///a.java\","
                    + "\"range\":{\"start\":{\"line\":4,\"character\":0},\"end\":{\"line\":4,\"character\":20}},"
                    + "\"selectionRange\":{\"start\":{\"line\":4,\"character\":9},\"end\":{\"line\":4,\"character\":15}}}}");

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            assertThat(result.values().get(0).asObject().get("to").asObject().get("name").asString().value()).isEqualTo("callee");
        }
    }

    @Test
    void shouldReturnPrepareTypeHierarchyItems() throws Exception {
        final var server = LspServer.builder()
            .onPrepareTypeHierarchy((params, ctx) -> List.of(
                new TypeHierarchyItem("Widget", SymbolKind.CLASS, null, "file:///widget.java",
                    Range.of(0, 0, 10, 1), Range.of(0, 6, 0, 12), null)))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "textDocument/prepareTypeHierarchy", POSITION_PARAMS);

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            assertThat(result.values().get(0).asObject().get("name").asString().value()).isEqualTo("Widget");
        }
    }

    @Test
    void shouldReturnTypeHierarchySupertypes() throws Exception {
        final var server = LspServer.builder()
            .onTypeHierarchySupertypes((params, ctx) -> {
                assertThat(params.item().name()).isEqualTo("Widget");
                return List.of(new TypeHierarchyItem("Component", SymbolKind.INTERFACE, null, "file:///component.java",
                    Range.of(0, 0, 5, 1), Range.of(0, 6, 0, 15), null));
            })
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "typeHierarchy/supertypes",
                "{\"item\":{\"name\":\"Widget\",\"kind\":5,\"uri\":\"file:///widget.java\","
                    + "\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":10,\"character\":1}},"
                    + "\"selectionRange\":{\"start\":{\"line\":0,\"character\":6},\"end\":{\"line\":0,\"character\":12}}}}");

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            assertThat(result.values().get(0).asObject().get("name").asString().value()).isEqualTo("Component");
        }
    }

    @Test
    void shouldReturnTypeHierarchySubtypes() throws Exception {
        final var server = LspServer.builder()
            .onTypeHierarchySubtypes((params, ctx) -> List.of(
                new TypeHierarchyItem("Button", SymbolKind.CLASS, null, "file:///button.java",
                    Range.of(0, 0, 5, 1), Range.of(0, 6, 0, 12), null)))
            .build();

        try (final var client = new LspTransportTests.LspTestClient(server)) {
            final var response = client.sendRequest(1, "typeHierarchy/subtypes",
                "{\"item\":{\"name\":\"Widget\",\"kind\":5,\"uri\":\"file:///widget.java\","
                    + "\"range\":{\"start\":{\"line\":0,\"character\":0},\"end\":{\"line\":10,\"character\":1}},"
                    + "\"selectionRange\":{\"start\":{\"line\":0,\"character\":6},\"end\":{\"line\":0,\"character\":12}}}}");

            final var result = response.get("result").asArray();
            assertThat(result.values()).hasSize(1);
            assertThat(result.values().get(0).asObject().get("name").asString().value()).isEqualTo("Button");
        }
    }
}
