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

import build.base.json.Json;
import build.base.json.JsonArray;
import build.base.json.JsonBoolean;
import build.base.json.JsonNull;
import build.base.json.JsonNumber;
import build.base.json.JsonObject;
import build.base.json.JsonString;
import build.base.json.JsonValue;
import build.serve.lsp.params.CallHierarchyIncomingCallsParams;
import build.serve.lsp.params.CallHierarchyOutgoingCallsParams;
import build.serve.lsp.params.CodeActionParams;
import build.serve.lsp.params.DidChangeParams;
import build.serve.lsp.params.DidCloseParams;
import build.serve.lsp.params.DidOpenParams;
import build.serve.lsp.params.DidSaveParams;
import build.serve.lsp.params.DocumentSymbolParams;
import build.serve.lsp.params.ExecuteCommandParams;
import build.serve.lsp.params.FoldingRangeParams;
import build.serve.lsp.params.FormattingParams;
import build.serve.lsp.params.InitializeParams;
import build.serve.lsp.params.InlayHintParams;
import build.serve.lsp.params.RangeFormattingParams;
import build.serve.lsp.params.ReferenceParams;
import build.serve.lsp.params.RenameParams;
import build.serve.lsp.params.SelectionRangeParams;
import build.serve.lsp.params.TextDocumentPositionParams;
import build.serve.lsp.params.TypeHierarchySubtypesParams;
import build.serve.lsp.params.TypeHierarchySupertypesParams;
import build.serve.lsp.params.WorkspaceSymbolParams;
import build.serve.lsp.types.ApplyWorkspaceEditResult;
import build.serve.lsp.types.CallHierarchyIncomingCall;
import build.serve.lsp.types.CallHierarchyItem;
import build.serve.lsp.types.CallHierarchyOutgoingCall;
import build.serve.lsp.types.ClientCapabilities;
import build.serve.lsp.types.CodeAction;
import build.serve.lsp.types.Command;
import build.serve.lsp.types.CompletionItem;
import build.serve.lsp.types.ConfigurationItem;
import build.serve.lsp.types.Diagnostic;
import build.serve.lsp.types.DiagnosticSeverity;
import build.serve.lsp.types.DocumentHighlight;
import build.serve.lsp.types.DocumentSymbol;
import build.serve.lsp.types.FoldingRange;
import build.serve.lsp.types.Hover;
import build.serve.lsp.types.InlayHint;
import build.serve.lsp.types.Location;
import build.serve.lsp.types.LspType;
import build.serve.lsp.types.MarkupContent;
import build.serve.lsp.types.ParameterInformation;
import build.serve.lsp.types.Position;
import build.serve.lsp.types.Range;
import build.serve.lsp.types.SelectionRange;
import build.serve.lsp.types.ServerCapabilities;
import build.serve.lsp.types.ShowMessageParams;
import build.serve.lsp.types.SignatureHelp;
import build.serve.lsp.types.SignatureInformation;
import build.serve.lsp.types.SymbolInformation;
import build.serve.lsp.types.SymbolKind;
import build.serve.lsp.types.TextDocumentContentChangeEvent;
import build.serve.lsp.types.TextDocumentIdentifier;
import build.serve.lsp.types.TextDocumentItem;
import build.serve.lsp.types.TextEdit;
import build.serve.lsp.types.TypeHierarchyItem;
import build.serve.lsp.types.VersionedTextDocumentIdentifier;
import build.serve.lsp.types.WorkspaceEdit;
import build.serve.lsp.types.WorkspaceFolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Central JSON converter for LSP types. Serialises via sealed pattern matching; deserialises via
 * explicit field extraction.
 */
final class LspJson {

    private LspJson() {
    }

    // ── Serialisation ────────────────────────────────────────────────────────

    static JsonValue toJson(final Object value) {
        return switch (value) {
            case null -> JsonNull.INSTANCE;
            case LspType t -> typeToJson(t);
            case List<?> l -> listToJson(l);
            case JsonValue v -> v;
            default -> Json.of(value);
        };
    }

    private static JsonArray listToJson(final List<?> list) {
        final var b = JsonArray.builder();
        for (final var item : list) {
            b.add(toJson(item));
        }
        return b.build();
    }

    private static JsonValue typeToJson(final LspType type) {
        return switch (type) {
            case Position p -> JsonObject.builder()
                .put("line", p.line())
                .put("character", p.character())
                .build();
            case Range r -> JsonObject.builder()
                .put("start", toJson(r.start()))
                .put("end", toJson(r.end()))
                .build();
            case Location l -> JsonObject.builder()
                .put("uri", l.uri())
                .put("range", toJson(l.range()))
                .build();
            case TextDocumentIdentifier t -> JsonObject.builder()
                .put("uri", t.uri())
                .build();
            case VersionedTextDocumentIdentifier v -> JsonObject.builder()
                .put("uri", v.uri())
                .put("version", v.version())
                .build();
            case TextDocumentItem t -> JsonObject.builder()
                .put("uri", t.uri())
                .put("languageId", t.languageId())
                .put("version", t.version())
                .put("text", t.text())
                .build();
            case TextDocumentContentChangeEvent e -> {
                final var b = JsonObject.builder();
                if (e.range() != null) {
                    b.put("range", toJson(e.range()));
                }
                yield b.put("text", e.text()).build();
            }
            case TextEdit e -> JsonObject.builder()
                .put("range", toJson(e.range()))
                .put("newText", e.newText())
                .build();
            case WorkspaceEdit w -> {
                final var changes = JsonObject.builder();
                w.changes().forEach((uri, edits) -> changes.put(uri, listToJson(edits)));
                yield JsonObject.builder().put("changes", changes.build()).build();
            }
            case MarkupContent m -> JsonObject.builder()
                .put("kind", m.kind())
                .put("value", m.value())
                .build();
            case Hover h -> {
                final var b = JsonObject.builder().put("contents", toJson(h.contents()));
                if (h.range() != null) {
                    b.put("range", toJson(h.range()));
                }
                yield b.build();
            }
            case CompletionItem c -> {
                final var b = JsonObject.builder().put("label", c.label());
                if (c.kind() != null) {
                    b.put("kind", c.kind().value());
                }
                if (c.detail() != null) {
                    b.put("detail", c.detail());
                }
                if (c.documentation() != null) {
                    b.put("documentation", toJson(c.documentation()));
                }
                if (c.insertText() != null) {
                    b.put("insertText", c.insertText());
                }
                yield b.build();
            }
            case Diagnostic d -> {
                final var b = JsonObject.builder().put("range", toJson(d.range()));
                if (d.severity() != null) {
                    b.put("severity", d.severity().value());
                }
                if (d.code() != null) {
                    b.put("code", d.code());
                }
                if (d.source() != null) {
                    b.put("source", d.source());
                }
                yield b.put("message", d.message()).build();
            }
            case DocumentHighlight dh -> JsonObject.builder()
                .put("range", toJson(dh.range()))
                .put("kind", dh.kind())
                .build();
            case DocumentSymbol ds -> {
                final var b = JsonObject.builder()
                    .put("name", ds.name())
                    .put("kind", ds.kind().value())
                    .put("range", toJson(ds.range()))
                    .put("selectionRange", toJson(ds.selectionRange()));
                if (ds.children() != null && !ds.children().isEmpty()) {
                    b.put("children", listToJson(ds.children()));
                }
                yield b.build();
            }
            case FoldingRange f -> {
                final var b = JsonObject.builder()
                    .put("startLine", f.startLine())
                    .put("endLine", f.endLine());
                if (f.kind() != null) {
                    b.put("kind", f.kind());
                }
                yield b.build();
            }
            case InlayHint i -> JsonObject.builder()
                .put("position", toJson(i.position()))
                .put("label", i.label())
                .put("kind", i.kind())
                .build();
            case SelectionRange s -> {
                final var b = JsonObject.builder().put("range", toJson(s.range()));
                if (s.parent() != null) {
                    b.put("parent", toJson(s.parent()));
                }
                yield b.build();
            }
            case ParameterInformation pi -> {
                final var b = JsonObject.builder().put("label", pi.label());
                if (pi.documentation() != null) {
                    b.put("documentation", toJson(pi.documentation()));
                }
                yield b.build();
            }
            case SignatureInformation si -> {
                final var b = JsonObject.builder().put("label", si.label());
                if (si.documentation() != null) {
                    b.put("documentation", toJson(si.documentation()));
                }
                if (si.parameters() != null && !si.parameters().isEmpty()) {
                    b.put("parameters", listToJson(si.parameters()));
                }
                yield b.build();
            }
            case SignatureHelp sh -> JsonObject.builder()
                .put("signatures", listToJson(sh.signatures()))
                .put("activeSignature", sh.activeSignature())
                .put("activeParameter", sh.activeParameter())
                .build();
            case SymbolInformation si -> {
                final var b = JsonObject.builder()
                    .put("name", si.name())
                    .put("kind", si.kind().value())
                    .put("location", toJson(si.location()));
                if (si.containerName() != null) {
                    b.put("containerName", si.containerName());
                }
                yield b.build();
            }
            case CodeAction ca -> {
                final var b = JsonObject.builder().put("title", ca.title());
                if (ca.kind() != null) {
                    b.put("kind", ca.kind());
                }
                if (ca.diagnostics() != null && !ca.diagnostics().isEmpty()) {
                    b.put("diagnostics", listToJson(ca.diagnostics()));
                }
                if (ca.edit() != null) {
                    b.put("edit", toJson(ca.edit()));
                }
                if (ca.command() != null) {
                    b.put("command", toJson(ca.command()));
                }
                yield b.build();
            }
            case Command c -> {
                final var b = JsonObject.builder()
                    .put("title", c.title())
                    .put("command", c.command());
                if (c.arguments() != null && !c.arguments().isEmpty()) {
                    b.put("arguments", listToJson(c.arguments()));
                }
                yield b.build();
            }
            case ServerCapabilities sc -> {
                final var b = JsonObject.builder();
                if (sc.textDocumentSync() != null) {
                    b.put("textDocumentSync", Json.of(sc.textDocumentSync()));
                }
                for (final var cap : sc.providers()) {
                    b.put(cap.fieldName, true);
                }
                yield b.build();
            }
            case CallHierarchyItem chi -> {
                final var b = JsonObject.builder()
                    .put("name", chi.name())
                    .put("kind", chi.kind().value())
                    .put("uri", chi.uri())
                    .put("range", toJson(chi.range()))
                    .put("selectionRange", toJson(chi.selectionRange()));
                if (chi.detail() != null) {
                    b.put("detail", chi.detail());
                }
                if (chi.data() != null) {
                    b.put("data", chi.data());
                }
                yield b.build();
            }
            case CallHierarchyIncomingCall c -> JsonObject.builder()
                .put("from", toJson(c.from()))
                .put("fromRanges", listToJson(c.fromRanges()))
                .build();
            case CallHierarchyOutgoingCall c -> JsonObject.builder()
                .put("to", toJson(c.to()))
                .put("fromRanges", listToJson(c.fromRanges()))
                .build();
            case TypeHierarchyItem thi -> {
                final var b = JsonObject.builder()
                    .put("name", thi.name())
                    .put("kind", thi.kind().value())
                    .put("uri", thi.uri())
                    .put("range", toJson(thi.range()))
                    .put("selectionRange", toJson(thi.selectionRange()));
                if (thi.detail() != null) {
                    b.put("detail", thi.detail());
                }
                if (thi.data() != null) {
                    b.put("data", thi.data());
                }
                yield b.build();
            }
            case ShowMessageParams smp -> JsonObject.builder()
                .put("type", smp.type())
                .put("message", smp.message())
                .build();
            case ConfigurationItem ci -> {
                final var b = JsonObject.builder();
                if (ci.scopeUri() != null) {
                    b.put("scopeUri", ci.scopeUri());
                }
                if (ci.section() != null) {
                    b.put("section", ci.section());
                }
                yield b.build();
            }
            case ClientCapabilities __ -> JsonObject.builder().build();
        };
    }

    // ── Deserialisation helpers ───────────────────────────────────────────────

    static Position parsePosition(final JsonObject o) {
        return new Position(
            o.get("line").asNumber().toNumber().intValue(),
            o.get("character").asNumber().toNumber().intValue()
        );
    }

    static Range parseRange(final JsonObject o) {
        return new Range(
            parsePosition(o.get("start").asObject()),
            parsePosition(o.get("end").asObject())
        );
    }

    private static TextDocumentIdentifier parseTDI(final JsonObject o) {
        return new TextDocumentIdentifier(o.get("uri").asString().value());
    }

    private static VersionedTextDocumentIdentifier parseVTDI(final JsonObject o) {
        return new VersionedTextDocumentIdentifier(
            o.get("uri").asString().value(),
            o.get("version").asNumber().toNumber().intValue()
        );
    }

    private static TextDocumentItem parseTDItem(final JsonObject o) {
        return new TextDocumentItem(
            o.get("uri").asString().value(),
            o.get("languageId").asString().value(),
            o.get("version").asNumber().toNumber().intValue(),
            o.get("text").asString().value()
        );
    }

    private static TextDocumentContentChangeEvent parseTDCE(final JsonObject o) {
        final var range = o.has("range") ? parseRange(o.get("range").asObject()) : null;
        return new TextDocumentContentChangeEvent(range, o.get("text").asString().value());
    }

    private static Diagnostic parseDiagnostic(final JsonObject o) {
        final var severity = o.has("severity")
            ? DiagnosticSeverity.fromValue(o.get("severity").asNumber().toNumber().intValue())
            : null;
        final var code = o.has("code") ? o.get("code").asString().value() : null;
        final var source = o.has("source") ? o.get("source").asString().value() : null;
        return new Diagnostic(
            parseRange(o.get("range").asObject()),
            severity, code, source,
            o.get("message").asString().value()
        );
    }

    private static CallHierarchyItem parseCallHierarchyItem(final JsonObject o) {
        return new CallHierarchyItem(
            o.get("name").asString().value(),
            SymbolKind.fromValue(o.get("kind").asNumber().toNumber().intValue()),
            o.has("detail") ? o.get("detail").asString().value() : null,
            o.get("uri").asString().value(),
            parseRange(o.get("range").asObject()),
            parseRange(o.get("selectionRange").asObject()),
            o.has("data") ? o.get("data") : null
        );
    }

    private static TypeHierarchyItem parseTypeHierarchyItem(final JsonObject o) {
        return new TypeHierarchyItem(
            o.get("name").asString().value(),
            SymbolKind.fromValue(o.get("kind").asNumber().toNumber().intValue()),
            o.has("detail") ? o.get("detail").asString().value() : null,
            o.get("uri").asString().value(),
            parseRange(o.get("range").asObject()),
            parseRange(o.get("selectionRange").asObject()),
            o.has("data") ? o.get("data") : null
        );
    }

    static ApplyWorkspaceEditResult parseApplyWorkspaceEditResult(final JsonValue value) {
        final var o = value.asObject();
        return new ApplyWorkspaceEditResult(
            o.has("applied") && o.get("applied").asBoolean().value(),
            o.has("failureReason") ? o.get("failureReason").asString().value() : null
        );
    }

    static List<JsonValue> parseConfigurationResult(final JsonValue value) {
        final var result = new ArrayList<JsonValue>();
        if (value instanceof JsonArray a) {
            result.addAll(a.values());
        }
        return result;
    }

    static List<WorkspaceFolder> parseWorkspaceFolders(final JsonValue value) {
        final var result = new ArrayList<WorkspaceFolder>();
        if (value instanceof JsonArray a) {
            for (final var item : a.values()) {
                final var o = item.asObject();
                result.add(new WorkspaceFolder(o.get("uri").asString().value(), o.get("name").asString().value()));
            }
        }
        return result;
    }

    // ── Params parsers ────────────────────────────────────────────────────────

    static InitializeParams parseInitialize(final JsonObject o) {
        return new InitializeParams(
            o.has("rootUri") ? o.get("rootUri").asString().value() : null,
            new ClientCapabilities()
        );
    }

    static TextDocumentPositionParams parseTDPosition(final JsonObject o) {
        return new TextDocumentPositionParams(
            parseTDI(o.get("textDocument").asObject()),
            parsePosition(o.get("position").asObject())
        );
    }

    static CallHierarchyIncomingCallsParams parseCallHierarchyIncomingCalls(final JsonObject o) {
        return new CallHierarchyIncomingCallsParams(parseCallHierarchyItem(o.get("item").asObject()));
    }

    static CallHierarchyOutgoingCallsParams parseCallHierarchyOutgoingCalls(final JsonObject o) {
        return new CallHierarchyOutgoingCallsParams(parseCallHierarchyItem(o.get("item").asObject()));
    }

    static TypeHierarchySupertypesParams parseTypeHierarchySupertypes(final JsonObject o) {
        return new TypeHierarchySupertypesParams(parseTypeHierarchyItem(o.get("item").asObject()));
    }

    static TypeHierarchySubtypesParams parseTypeHierarchySubtypes(final JsonObject o) {
        return new TypeHierarchySubtypesParams(parseTypeHierarchyItem(o.get("item").asObject()));
    }

    static CodeActionParams parseCodeAction(final JsonObject o) {
        final var diagnostics = new ArrayList<Diagnostic>();
        if (o.has("context")) {
            final var context = o.get("context").asObject();
            if (context.has("diagnostics")) {
                for (final var item : context.get("diagnostics").asArray().values()) {
                    diagnostics.add(parseDiagnostic(item.asObject()));
                }
            }
        }
        return new CodeActionParams(
            parseTDI(o.get("textDocument").asObject()),
            parseRange(o.get("range").asObject()),
            diagnostics
        );
    }

    static DocumentSymbolParams parseDocumentSymbol(final JsonObject o) {
        return new DocumentSymbolParams(parseTDI(o.get("textDocument").asObject()));
    }

    static ExecuteCommandParams parseExecuteCommand(final JsonObject o) {
        final List<Object> args;
        if (o.has("arguments")) {
            final var list = new ArrayList<Object>();
            for (final var item : o.get("arguments").asArray().values()) {
                list.add(switch (item) {
                    case JsonString s -> s.value();
                    case JsonNumber n -> n.toNumber();
                    case JsonBoolean b -> b.value();
                    case JsonNull __ -> null;
                    case JsonObject obj -> obj;
                    case JsonArray arr -> arr;
                });
            }
            args = list;
        } else {
            args = List.of();
        }
        return new ExecuteCommandParams(o.get("command").asString().value(), args);
    }

    static FoldingRangeParams parseFoldingRange(final JsonObject o) {
        return new FoldingRangeParams(parseTDI(o.get("textDocument").asObject()));
    }

    static FormattingParams parseFormatting(final JsonObject o) {
        return new FormattingParams(parseTDI(o.get("textDocument").asObject()));
    }

    static InlayHintParams parseInlayHint(final JsonObject o) {
        return new InlayHintParams(
            parseTDI(o.get("textDocument").asObject()),
            parseRange(o.get("range").asObject())
        );
    }

    static RangeFormattingParams parseRangeFormatting(final JsonObject o) {
        return new RangeFormattingParams(
            parseTDI(o.get("textDocument").asObject()),
            parseRange(o.get("range").asObject())
        );
    }

    static ReferenceParams parseReferences(final JsonObject o) {
        return new ReferenceParams(
            parseTDI(o.get("textDocument").asObject()),
            parsePosition(o.get("position").asObject()),
            o.has("includeDeclaration") && o.get("includeDeclaration").asBoolean().value()
        );
    }

    static RenameParams parseRename(final JsonObject o) {
        return new RenameParams(
            parseTDI(o.get("textDocument").asObject()),
            parsePosition(o.get("position").asObject()),
            o.get("newName").asString().value()
        );
    }

    static SelectionRangeParams parseSelectionRange(final JsonObject o) {
        final var positions = new ArrayList<Position>();
        for (final var item : o.get("positions").asArray().values()) {
            positions.add(parsePosition(item.asObject()));
        }
        return new SelectionRangeParams(parseTDI(o.get("textDocument").asObject()), positions);
    }

    static WorkspaceSymbolParams parseWorkspaceSymbol(final JsonObject o) {
        return new WorkspaceSymbolParams(o.get("query").asString().value());
    }

    static DidChangeParams parseDidChange(final JsonObject o) {
        final var changes = new ArrayList<TextDocumentContentChangeEvent>();
        for (final var item : o.get("contentChanges").asArray().values()) {
            changes.add(parseTDCE(item.asObject()));
        }
        return new DidChangeParams(parseVTDI(o.get("textDocument").asObject()), changes);
    }

    static DidCloseParams parseDidClose(final JsonObject o) {
        return new DidCloseParams(parseTDI(o.get("textDocument").asObject()));
    }

    static DidOpenParams parseDidOpen(final JsonObject o) {
        return new DidOpenParams(parseTDItem(o.get("textDocument").asObject()));
    }

    static DidSaveParams parseDidSave(final JsonObject o) {
        return new DidSaveParams(
            parseTDI(o.get("textDocument").asObject()),
            o.has("text") ? o.get("text").asString().value() : null
        );
    }
}
