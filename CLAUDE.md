# serve.build

## Codebase Overview

A lightweight, JPMS-native HTTP server framework for Java 25+ built directly on `jdk.httpserver`, virtual threads,
scoped values, and structured concurrency. No Spring, no annotation scanning — routes are code, DI is explicit. Part of
the `*.build` family.

**Stack**: Java 25 (preview enabled), Maven multi-module, Jackson, graphql-java, JTE, JUnit 5 + AssertJ  
**Structure**: 19 JPMS modules — core layer (foundation, transports, application, testing), protocol layer (WebSocket,
SSE, MCP, LSP, GraphQL), middleware layer (CORS, security, compression, logging, health, static), template layer (
template SPI, JTE, HTMX)

For detailed architecture, see [docs/CODEBASE_MAP.md](docs/CODEBASE_MAP.md).

## Key Conventions

- **4-space indentation** everywhere (accessibility standard)
- **Null-free**: use `Optional<T>` instead of null in public APIs
- **Test method names** must start with `should` or `shouldNot`
- **No mocks**: use stub implementations (`StubRequest`, `StubResponse`) in tests
- **Middleware order**: first-registered = outermost (applied in reverse)
- All modules are `open module` with explicit `requires`/`exports`

## Build

```bash
./mvnw clean verify          # build + test all modules
./mvnw -pl serve-foundation test   # test a single module
```

Requires Java 25. Dependencies are resolved from Maven Central.
