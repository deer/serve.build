---
title: Middleware
description: How to compose and write middleware in serve.build, with examples of the built-in middleware.
ai-summary: "Explains middleware composition in serve.build — the Middleware functional interface, registration order (first = outermost), and the built-in middleware: logging, CORS, security headers, compression, JSON, health, and static files. Focuses on usage patterns rather than exhaustive configuration."
ai-keywords: [
  middleware,
  cors,
  security-headers,
  compression,
  logging,
  health,
  static,
  json,
  RequestLoggingMiddleware,
  CorsMiddleware,
  SecurityHeadersMiddleware,
  CompressionMiddleware,
  JsonMiddleware,
  HealthHandler,
]
---

# Middleware

Middleware wraps handlers. Register it with `.middleware()` on `RouterBuilder` —
first-registered is the outermost wrapper, so it runs first on the way in and
last on the way out.

A typical middleware stack looks like this:

```java
RouterBuilder.create()
    .middleware(RequestLoggingMiddleware.defaults())   // outermost
    .middleware(CorsMiddleware.allowAll())
    .middleware(SecurityHeadersMiddleware.defaults())
    .middleware(CompressionMiddleware.defaults())
    .middleware(new JsonMiddleware())                  // innermost
    .route("/", myRouter)
    .build();
```

## Writing your own

Middleware is a `@FunctionalInterface`: `Handler apply(Handler next)`. Wrap the
next handler however you like:

```java
Middleware timer = next -> exchange -> {
    long start = System.nanoTime();
    next.handle(exchange);
    long elapsed = (System.nanoTime() - start) / 1_000_000;
    System.out.println(exchange.request().path() + " took " + elapsed + "ms");
};
```

## Built-in middleware

### Logging

`RequestLoggingMiddleware` logs method, path, status, and timing for every
request. Call `.defaults()` for zero-config access logging, or build a custom
instance to set a slow-request threshold and exclude health-check paths from the
log.

```java
.middleware(RequestLoggingMiddleware.defaults())
```

### CORS

`CorsMiddleware` adds `Access-Control-*` headers and handles OPTIONS preflight.
`allowAll()` is fine for development. In production, restrict to your actual
frontend origin:

```java
.middleware(CorsMiddleware.builder()
    .allowOrigins("https://app.example.com")
    .allowMethods("GET", "POST", "PUT", "DELETE")
    .allowHeaders("Authorization", "Content-Type")
    .build())
```

### Security headers

`SecurityHeadersMiddleware.defaults()` sets a standard hardening suite on every
response: `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
`Strict-Transport-Security`, `Referrer-Policy`, `Cross-Origin-Opener-Policy`,
and `Cross-Origin-Resource-Policy`. Works out of the box for most applications.

```java
.middleware(SecurityHeadersMiddleware.defaults())
```

### Compression

`CompressionMiddleware` transparently compresses responses with gzip or deflate
based on `Accept-Encoding`. Buffers the response body before compressing, so
it's incompatible with streaming handlers that use `response.bodyAsStream()`.

```java
.middleware(CompressionMiddleware.defaults())
```

### JSON

`JsonMiddleware` injects Jackson body reading and writing into the exchange.
Without it, `exchange.bodyAs(T)` and `exchange.response().json(obj)` throw
`UnsupportedOperationException`.

```java
.middleware(new JsonMiddleware())
```

### Health endpoints

`HealthHandler` is mounted as a route rather than wrapped middleware. It exposes
`/live` (always 200) and `/ready` (runs named checks, returns 503 if any fail):

```java
.route("/health", HealthHandler.create()
    .liveness("/live")
    .readiness("/ready",
        HealthCheck.of("db", () -> db.isAlive()))
    .build())
```

### Static files

`StaticFileHandler` serves files from the classpath or filesystem with ETag/304
support. Mount it at any path prefix:

```java
// Classpath resources (bundled with the application)
.route("/static", StaticFileHandler.resources(MyApp.class, "/static"))

// Filesystem directory
.route("/static", StaticFileHandler.directory(Path.of("public")))
```

## See also

- [Routing](/docs/routing) — `RouterBuilder`, `Exchange`, and error handling
- [Protocols](/docs/protocols) — WebSocket, SSE, MCP, and GraphQL
