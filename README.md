# serve.build

A server framework for Java developers who'd rather write code than configure it.

## What Is This

A JPMS-native server framework for Java 25+. Virtual threads as the only execution model, real Java modules, zero
annotation magic. Part of the [*.build](https://github.com/deer) open-source ecosystem.

## Why

Modern Java has virtual threads, scoped values, structured concurrency, and a built-in HTTP server. serve.build
surfaces all of them as first-class citizens.

- **JPMS-first** — real `module-info.java` with proper `requires`/`exports`, not classpath
- **Virtual threads as architecture** — not a config flag, the only execution model
- **Routing is code** — `RouterBuilder` wires routes explicitly; DI is `@Inject` resolved at startup
- **JDK only for HTTP** — `jdk.httpserver` under the hood, Jackson only if you want JSON

## Quick Start

```java
import build.base.network.option.Port;
import build.serve.application.Launcher;
import build.serve.application.ServerApplication;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;

public class HelloServer extends ServerApplication.Implementation {

    @Override
    protected Router configure() {
        return RouterBuilder.create()
            .get("/hello", exchange ->
                exchange.response().status(200).send("Hello, World!"))
            .build();
    }

    public static void main(String[] args) {
        Launcher.launch(new HelloServer(), Port.of(8080));
    }
}
```

That's it. Routes are code, wired at startup. One virtual thread per request, started in milliseconds.

## Features

- **Virtual threads** — every request runs on its own virtual thread. No thread pools to tune.
- **19 JPMS modules** — real `module-info.java` with explicit `requires`/`exports`; take only what you need
- **Type-safe routing** — path parameters (`/users/{id}`), HTTP method matching, nested routers
- **Middleware pipeline** — `Middleware` wraps `Handler`, applied in registration order
- **Dependency injection** — Jakarta `@Inject` via [codemodel.build](https://github.com/Workday/codemodel.build), resolved at startup
- **JSON support** — Jackson-based body reading/writing via `JsonMiddleware`
- **Typed error handling** — `HttpException` hierarchy (`NotFoundException`, `BadRequestException`, etc.)
- **Scoped Values** — request context (`RequestContext.EXCHANGE`, `REQUEST_ID`, `START_TIME`) without ThreadLocal
- **Structured Concurrency** — `Concurrent.fork()` for type-safe parallel fan-out within a request
- **WebSocket** — RFC 6455, raw frame implementation over `jdk.httpserver`
- **SSE** — Server-Sent Events (RFC 8895) with connection-drop detection
- **MCP** — Model Context Protocol (2025-03-26, Streamable HTTP transport)
- **LSP** — Language Server Protocol 3.17 with full typed handler support
- **GraphQL** — GraphQL over HTTP backed by graphql-java, with GraphiQL IDE
- **Middleware** — CORS, security headers, gzip/deflate compression, access logging, health checks, static files
- **Templates** — engine-agnostic SPI with JTE implementation and HTMX helpers
- **Test harness** — `TestServer` with fluent assertions, ephemeral port binding, zero boilerplate

## Modules

### Core

| Module                 | Java Module                  | Purpose                   | Key Types                                                                                                                    |
|------------------------|------------------------------|---------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `serve-foundation`     | `build.serve.foundation`     | Core abstractions         | `Exchange`, `Request`, `Response`, `Handler`, `Middleware`, `RouterBuilder`, `HttpException`, `RequestContext`, `Concurrent` |
| `serve-transport-http` | `build.serve.transport.http` | HTTP via `jdk.httpserver` | `HttpTransport`                                                                                                              |
| `serve-transport-json` | `build.serve.transport.json` | JSON body read/write      | `JsonMiddleware`, `JsonBodyReader`, `JsonBodyWriter`, `JsonErrorHandler`                                                     |
| `serve-application`    | `build.serve.application`    | Server lifecycle + DI     | `ServerApplication`, `Launcher`                                                                                              |
| `serve-testing`        | `build.serve.testing`        | Test harness              | `TestServer`, `TestRequest`, `TestResponse`                                                                                  |

### Protocols

| Module            | Java Module              | Purpose                            | Key Types                                                    |
|-------------------|--------------------------|------------------------------------|--------------------------------------------------------------|
| `serve-websocket` | `build.serve.websocket`  | RFC 6455 WebSocket                 | `WebSocketUpgrade`, `WebSocketHandler`, `WebSocket`          |
| `serve-sse`       | `build.serve.sse`        | Server-Sent Events (RFC 8895)      | `SseUpgrade`, `SseHandler`, `SseEmitter`, `SseEvent`         |
| `serve-mcp`       | `build.serve.mcp`        | Model Context Protocol             | `McpServer`, `McpTool`, `McpToolResult`                      |
| `serve-lsp`       | `build.serve.lsp`        | Language Server Protocol 3.17      | `LspTransport`, `LspServer`, `LspContext`                    |
| `serve-graphql`   | `build.serve.graphql`    | GraphQL over HTTP                  | `GraphQlHandler`, `GraphQlSchema`, `GraphiQlHandler`         |

### Middleware

| Module               | Java Module                   | Purpose                   | Key Types                     |
|----------------------|-------------------------------|---------------------------|-------------------------------|
| `serve-cors`         | `build.serve.cors`            | CORS                      | `CorsMiddleware`              |
| `serve-security`     | `build.serve.security`        | Security headers          | `SecurityHeadersMiddleware`   |
| `serve-compression`  | `build.serve.compression`     | Gzip/deflate compression  | `CompressionMiddleware`       |
| `serve-logging`      | `build.serve.logging`         | Access logging            | `RequestLoggingMiddleware`    |
| `serve-health`       | `build.serve.health`          | Health/readiness checks   | `HealthHandler`, `HealthCheck` |
| `serve-staticfiles`  | `build.serve.staticfiles`     | Static file serving       | `StaticFileHandler`           |

### Templates

| Module            | Java Module              | Purpose                         | Key Types                                        |
|-------------------|--------------------------|---------------------------------|--------------------------------------------------|
| `serve-template`  | `build.serve.template`   | Engine-agnostic template SPI    | `Template`, `TemplateEngine`, `TemplateHandler`  |
| `serve-jte`       | `build.serve.jte`        | JTE template engine             | `JteTemplateEngine`                              |
| `serve-htmx`      | `build.serve.htmx`       | HTMX request/response helpers   | `HtmxRequest`, `HtmxResponse`, `HtmxMiddleware` |

## Modern Java

serve.build targets Java 25+ and designs around features that older frameworks can't adopt without breaking backward
compatibility.

### Scoped Values

Request context propagated via `ScopedValue` instead of `ThreadLocal`. Automatically inherited by child virtual threads.

```java
import build.serve.foundation.context.RequestContext;

// In any handler or service — no parameter passing needed
var requestId = RequestContext.REQUEST_ID.get();
var startTime = RequestContext.START_TIME.get();
var exchange  = RequestContext.EXCHANGE.get();
```

### Structured Concurrency

Fan out within a request with automatic lifecycle management. If one subtask fails, the others are cancelled.

```java
import build.serve.foundation.concurrent.Concurrent;

// Fork two calls in parallel, get both results
var result = Concurrent.fork(
    () -> userService.findById(id),
    () -> orderService.findByUserId(id)
);
var user   = result.first();
var orders = result.second();

// Fork N tasks of the same type
var pages = Concurrent.forkAll(List.of(
    () -> fetch("/page/1"),
    () -> fetch("/page/2"),
    () -> fetch("/page/3")
));
```

Scoped values from the parent request thread are automatically inherited by forked subtasks.

## JSON API Example

```java
import build.serve.foundation.error.NotFoundException;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.transport.json.JsonMiddleware;
import jakarta.inject.Inject;

public class UserServer extends ServerApplication.Implementation {

    @Inject
    UserRepository users;

    @Override
    protected Router configure() {
        return RouterBuilder.create()
            .get("/users/{id}", exchange -> {
                var id = exchange.pathParam("id").orElseThrow();
                var user = users.find(id).orElseThrow(() ->
                    new NotFoundException("User not found: " + id));
                exchange.response().json(user);
            })
            .post("/users", exchange -> {
                var user = exchange.bodyAs(User.class);
                users.save(user);
                exchange.response().status(201).json(user);
            })
            .middleware(new JsonMiddleware())
            .build();
    }
}
```

## Testing

```java
import build.serve.foundation.routing.RouterBuilder;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.Test;

class HelloWorldTest {

    @Test
    void helloWorldReturns200() {
        try (var server = TestServer.of(RouterBuilder.create()
            .get("/hello", exchange ->
                exchange.response().status(200).send("Hello, World!"))
        )) {

            server.get("/hello")
                .send()
                .assertStatus(200)
                .assertBody("Hello, World!");
        }
    }
}
```

`TestServer.of()` accepts a `RouterBuilder`, `Router`, or `Handler`. Binds to an ephemeral port. Closes automatically.

## Building

**Prerequisites:**

- Java 25 with preview features
- Maven 3.9.14 (included via wrapper)

```bash
./mvnw clean verify
```

## Running the Example

```bash
# Build everything
./mvnw install

# Start the example on port 8080
./mvnw -pl serve-example exec:exec
```

The example is a simple task manager: create tasks, toggle them done, delete them. It demonstrates
the full feature set in one app — the same task list is served three ways simultaneously:

- **HTMX UI** at http://localhost:8080 — server-rendered HTML with live WebSocket push (open two
  tabs and watch updates appear in real time)
- **REST API** at `/api/tasks` — `GET`, `POST`, `PUT /{id}` (toggle), `DELETE /{id}`
- **GraphQL** at http://localhost:8080/graphiql — interactive GraphiQL IDE
- **Health** at http://localhost:8080/health/live

## Ecosystem

serve.build builds on the `*.build` open-source ecosystem:

- [**base.build**](https://github.com/Workday/base.build) — Foundation: configuration, options, flow, transport
- [**codemodel.build**](https://github.com/Workday/codemodel.build) — Code generation + dependency injection
- [**spawn.build**](https://github.com/Workday/spawn.build) — Application lifecycle, platform, process management

## Status

**Experimental / pre-release.** 200+ tests passing across all 19 modules.

- ✅ Core abstractions (Exchange, Handler, Middleware, Router)
- ✅ HTTP transport with virtual threads
- ✅ Application lifecycle with DI
- ✅ JSON body reading/writing
- ✅ WebSocket (RFC 6455)
- ✅ Server-Sent Events (RFC 8895)
- ✅ MCP (Model Context Protocol 2025-03-26)
- ✅ LSP (Language Server Protocol 3.17)
- ✅ GraphQL over HTTP with GraphiQL
- ✅ CORS, security headers, compression, logging, health, static files
- ✅ Template SPI with JTE + HTMX helpers
- ✅ TLS/HTTPS, cookies, graceful shutdown
- ✅ Test harness

**Planned:**

- gRPC

API will change.

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
