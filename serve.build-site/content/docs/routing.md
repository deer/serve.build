---
title: Routing
description: How to define routes and handlers with RouterBuilder and Exchange.
ai-summary: Covers routing in serve.build — RouterBuilder DSL, route parameter patterns, sub-routers, the Exchange abstraction (request/response/attributes), path parameters, and error handling with HttpException subclasses.
ai-keywords: [
  routing,
  RouterBuilder,
  Handler,
  Exchange,
  route-params,
  sub-router,
  HttpException,
  request,
  response,
  attributes,
]
---

# Routing

All routing flows through `RouterBuilder`. Build a `Router`, pass it to your
transport, and it dispatches incoming requests by method and path.

## Defining routes

```java
RouterBuilder.create()
    .get("/users",        exchange -> { /* list users */ })
    .post("/users",       exchange -> { /* create user */ })
    .get("/users/{id}",   exchange -> { /* get one user */ })
    .delete("/users/{id}", exchange -> { /* delete user */ })
    .build();
```

The supported method helpers are `.get()`, `.post()`, `.put()`, `.delete()`,
`.patch()`, `.head()`, `.options()`. Use `.route()` to mount a sub-router or
handler at a path prefix.

## Path parameters

Curly-brace segments are captured as named parameters:

```java
.get("/users/{id}", exchange -> {
    String id = exchange.pathParam("id").orElseThrow();
    exchange.response().json(userService.find(id));
})
```

Routes with a trailing `{param}` automatically accept sub-paths. Everything
under `/files/{path}` is matched and the full suffix is available as
`pathParam("path")`.

## Sub-routers

Use `.route()` to mount a whole router under a prefix:

```java
RouterBuilder.create()
    .route("/api/v1", apiRouter())
    .route("/health",  healthHandler)
    .build();
```

This is how the example app separates its REST, WebSocket, and web routes.

## Exchange

`Exchange` is the central object passed to every handler. It holds:

- `exchange.request()` — method, URI, headers, query params, cookies, body
  stream
- `exchange.response()` — fluent response builder
- `exchange.attribute(key, type)` — typed attribute bag (populated by
  middleware)

Common response methods:

```java
exchange.response().send("plain text");           // text/plain
exchange.response().json(myObject);               // application/json (requires JsonMiddleware)
exchange.response().status(204).send();           // no body
exchange.response().redirect("/new-path");        // 302
exchange.response().bodyAsStream();               // streaming — commits headers immediately
```

## Error handling

Throw any `HttpException` subclass from a handler to produce a structured error
response:

```java
throw new NotFoundException("User not found");   // → 404
throw new BadRequestException("Invalid input");  // → 400
throw new UnauthorizedException();               // → 401
```

The `DefaultErrorHandler` catches unrecognized exceptions and returns 500,
logging the stack trace at SEVERE. Wire in `JsonErrorHandler` (from
`serve-transport-json`) to get JSON-shaped error bodies.

## Request context

`RequestContext` exposes scoped values that are bound per-request by
`HttpTransport`:

```java
Exchange exchange = RequestContext.EXCHANGE.get();
String requestId  = RequestContext.REQUEST_ID.get();
Instant startTime = RequestContext.START_TIME.get();
```

These propagate automatically to virtual-thread subtasks via `ScopedValue`, so
structured-concurrency fan-outs inherit the full request context.

## See also

- [Middleware](/docs/middleware) — wrap routes with logging, CORS, and JSON
- [Protocols](/docs/protocols) — WebSocket, SSE, MCP, and GraphQL handlers
