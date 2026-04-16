---
title: Introduction
description: serve.build is a lightweight, JPMS-native HTTP server framework for Java 25+ built directly on jdk.httpserver.
ai-summary: Introduces serve.build — a lightweight Java 25 HTTP server framework built on jdk.httpserver, virtual threads, and structured concurrency. Covers the design philosophy (routes are code, explicit DI, JPMS-native), the module layers (core, protocol, middleware, template), and the key abstractions (Exchange, Handler, Router, Middleware).
ai-keywords: [
  serve.build,
  java,
  http,
  jdk.httpserver,
  virtual-threads,
  jpms,
  modules,
  exchange,
  handler,
  router,
  middleware,
]
---

# Introduction

serve.build is a lightweight HTTP server framework for Java 25+. It sits
directly on top of `jdk.httpserver` — the HTTP server that ships inside the JDK
— and adds only what's missing: a routing DSL, middleware composition,
virtual-thread dispatch, and a clean request/response abstraction.

Routes are code. Dependency injection is handled by
[codemodel.build](https://github.com/Workday/codemodel.build). The result is a
framework you can read top-to-bottom in an afternoon.

## Design principles

**Code over convention.** Routes are registered with `RouterBuilder`, middleware
is composed with `.middleware()`, and the application wires itself in a single
`configure()` method.

**JPMS-native.** Every module has explicit `requires` and `exports`. You know
exactly what depends on what.

**Virtual threads throughout.** Each request runs on its own virtual thread. No
thread pools to tune, no reactive programming model to learn.

**Null-free public API.** Public methods return `Optional<T>` instead of `null`.

## Module layers

serve.build is organized into four layers:

| Layer          | Modules                                                                                                   |
| -------------- | --------------------------------------------------------------------------------------------------------- |
| **Core**       | `serve-foundation`, `serve-transport-http`, `serve-transport-json`, `serve-application`, `serve-testing`  |
| **Protocol**   | `serve-websocket`, `serve-sse`, `serve-mcp`, `serve-lsp`, `serve-graphql`                                 |
| **Middleware** | `serve-cors`, `serve-security`, `serve-compression`, `serve-logging`, `serve-health`, `serve-staticfiles` |
| **Template**   | `serve-template`, `serve-jte`, `serve-htmx`                                                               |

Most applications only need core + a handful of middleware. Protocol and
template modules are opt-in.

## Key abstractions

`Exchange` is the pairing of a request and response for a single HTTP
interaction. It carries a typed attribute bag, which is how middleware injects
capabilities (JSON reading/writing, telemetry, etc.) into the request lifecycle
without coupling modules together.

`Handler` is a `@FunctionalInterface`: `void handle(Exchange) throws Exception`.
Everything that processes an HTTP request is a `Handler` — route callbacks,
middleware-wrapped chains, and routers themselves.

`Router` extends `Handler` and dispatches by path and method. Build one with
`RouterBuilder`.

`Middleware` is `Handler apply(Handler next)` — a function that wraps a handler.
Compose middleware with `.middleware()` on `RouterBuilder`. The first-registered
middleware is the outermost wrapper.
