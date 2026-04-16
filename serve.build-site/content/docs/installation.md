---
title: Installation
description: Add serve.build to a Java 25 Maven project.
ai-summary: How to add serve.build to a Java 25 Maven project. Covers prerequisites (Java 25 + preview features), module selection by layer, Maven dependency declarations, compiler plugin configuration, and module-info.java setup.
ai-keywords: [
  installation,
  maven,
  java-25,
  preview,
  jpms,
  module-info,
  dependencies,
  serve-foundation,
  serve-application,
  serve-transport-http,
]
---

# Installation

## Prerequisites

serve.build requires **Java 25** with preview features enabled. It has no
runtime dependencies beyond the JDK and the modules you choose to add.

## Choosing modules

serve.build is split into focused modules. Most applications only need the core
layer:

| Module                 | What it provides                                                              |
| ---------------------- | ----------------------------------------------------------------------------- |
| `serve-foundation`     | Core abstractions: `Exchange`, `Handler`, `RouterBuilder`, middleware SPI     |
| `serve-transport-http` | HTTP transport backed by `jdk.httpserver`, virtual-thread dispatch            |
| `serve-transport-json` | Jackson-based JSON reading and writing (`JsonMiddleware`, `JsonErrorHandler`) |
| `serve-application`    | `ServerApplication` lifecycle, `Launcher` for standalone use                  |
| `serve-testing`        | In-process test utilities (`StubRequest`, `StubResponse`)                     |

Additional modules are available for specific needs: `serve-websocket`,
`serve-sse`, `serve-mcp`, `serve-graphql`, `serve-lsp`, `serve-cors`,
`serve-security`, `serve-compression`, `serve-logging`, `serve-health`,
`serve-staticfiles`, `serve-template`, `serve-jte`, `serve-htmx`.

## Maven

Add the modules you need. These three cover most applications:

```xml
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-foundation</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-transport-http</artifactId>
  <version>0.1.0</version>
</dependency>
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-application</artifactId>
  <version>0.1.0</version>
</dependency>
```

Enable preview features in the compiler plugin:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>25</release>
    <compilerArgs><arg>--enable-preview</arg></compilerArgs>
  </configuration>
</plugin>
```

## JPMS

Declare the modules your application uses in `module-info.java`:

```java
module com.example.myapp {
    requires build.serve.foundation;
    requires build.serve.transport.http;
    requires build.serve.application;
}
```

Add a `requires` for each serve.build module you depend on. The module name
mirrors the artifact ID with hyphens replaced by dots and prefixed with
`build.serve` — for example, `serve-transport-json` →
`build.serve.transport.json`.
