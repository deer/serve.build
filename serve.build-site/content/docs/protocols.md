---
title: Protocols
description: WebSocket, Server-Sent Events, MCP, and GraphQL support in serve.build.
ai-summary: Covers the protocol modules in serve.build — WebSocket (RFC 6455 over jdk.httpserver), Server-Sent Events (RFC 8895), Model Context Protocol (JSON-RPC 2.0 over HTTP), and GraphQL (SDL-first via graphql-java with GraphiQL IDE). Includes wiring patterns and key caveats for each.
ai-keywords: [
  websocket,
  sse,
  server-sent-events,
  mcp,
  model-context-protocol,
  graphql,
  graphiql,
  WebSocketUpgrade,
  SseUpgrade,
  McpServer,
  GraphQlHandler,
  GraphQlSchema,
]
---

# Protocols

serve.build includes optional modules for WebSocket, Server-Sent Events, MCP,
and GraphQL. Each is a separate Maven artifact. All require `HttpTransport` (not
stub transports) because they extract the raw socket from `jdk.httpserver`
internals.

## WebSocket (`serve-websocket`)

`WebSocketUpgrade.upgrade(handler)` produces a `Handler` that performs the HTTP
101 handshake and then hands off to a `WebSocketHandler`.

```java
.route("/ws/chat", WebSocketUpgrade.upgrade(ws -> {
    ws.onText(msg -> ws.sendText("echo: " + msg));
    ws.onClose(code -> System.out.println("closed: " + code));
    ws.onError(err -> err.printStackTrace());
}))
```

Register all callbacks inside the `WebSocketHandler` before it returns — the
read loop starts immediately after. The connection runs on its own virtual
thread.

**Caveat:** The upgrade uses reflection against `jdk.httpserver` internals to
extract the raw I/O streams. This is cached on first use; a JDK update that
changes these internals will throw a `RuntimeException` at startup.

## Server-Sent Events (`serve-sse`)

`SseUpgrade.sse(handler)` upgrades a request to a streaming SSE response and
calls your handler with an `SseEmitter`.

```java
.get("/events", SseUpgrade.sse(emitter -> {
    for (int i = 0; i < 10; i++) {
        emitter.send(SseEvent.of("tick " + i));
        Thread.sleep(1000);
    }
}))
```

`SseEvent` supports `data`, `id`, `event` type, and `retry` interval. The
emitter sets `X-Accel-Buffering: no` automatically for nginx compatibility.

## Model Context Protocol (`serve-mcp`)

`McpServer` exposes callable tools to AI clients over JSON-RPC 2.0.

```java
Handler mcpHandler = McpServer.builder("my-server", "1.0")
    .tool(new McpTool() {
        public String name() { return "greet"; }
        public String description() { return "Returns a greeting"; }
        public ObjectNode inputSchema() {
            return McpTools.object(Map.of("name", McpTools.string("Name to greet")));
        }
        public McpToolResult call(JsonNode input) {
            return McpToolResult.text("Hello, " + input.path("name").asText() + "!");
        }
    })
    .build()
    .handler();

RouterBuilder.create()
    .post("/mcp", mcpHandler)
    .build();
```

Implements `initialize`, `tools/list`, and `tools/call`. Notifications (requests
without an `id`) return HTTP 202 with no body. Protocol version: `2025-03-26`.

## GraphQL (`serve-graphql`)

SDL-first: define your schema as a string, wire in data fetchers, done.

```java
GraphQlSchema schema = GraphQlSchema.builder("""
    type Query {
        user(id: ID!): User
    }
    type User {
        id: ID!
        name: String!
    }
    """)
    .fetcher("Query", "user", env -> userService.find(env.getArgument("id")))
    .build();

RouterBuilder.create()
    .post("/graphql",  GraphQlHandler.graphql(schema))
    .get("/graphiql",  GraphiQlHandler.graphiql("/graphql"))
    .build();
```

GraphiQL is served from `unpkg.com` CDN — requires outbound internet.
Introspection is always enabled. There are no query depth or complexity limits.

## LSP (`serve-lsp`)

`serve-lsp` implements the Language Server Protocol over stdio or TCP. It is
stream-oriented rather than HTTP-oriented and does not produce a `Handler` —
wire it via `LspTransport.stdio(server)` or `LspTransport.tcp(server, port)`
instead of through `RouterBuilder`.

## See also

- [Routing](/docs/routing) — mounting handlers with `RouterBuilder`
- [Middleware](/docs/middleware) — CORS, logging, and compression for protocol
  endpoints
