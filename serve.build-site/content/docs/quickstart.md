---
title: Quickstart
description: Run your first serve.build HTTP server in five minutes.
ai-summary: Step-by-step guide to running a serve.build HTTP server. Shows a minimal working application first, then covers Maven setup, module-info, and how to run the included example app. Requires Java 25 with preview features.
ai-keywords: [
  quickstart,
  maven,
  module-info,
  java-25,
  preview,
  RouterBuilder,
  Launcher,
  Handler,
  Exchange,
  JsonMiddleware,
]
---

# Quickstart

serve.build requires Java 25 with preview features enabled.

## Minimal server

Extend `ServerApplication.Implementation`, override `configure()` to return a
router, and launch:

```java
public final class MyApp extends ServerApplication.Implementation {

    public static void main(String[] args) {
        Launcher.launch(new MyApp(), Port.of(8080));
    }

    @Override
    protected Router configure() {
        return RouterBuilder.create()
            .get("/hello", exchange -> exchange.response().send("Hello, world!"))
            .build();
    }
}
```

Visit `http://localhost:8080/hello`.

## Adding JSON

Wire in `JsonMiddleware` to enable `exchange.bodyAs(T)` and
`exchange.response().json(obj)`:

```java
record Greeting(String message) {}

RouterBuilder.create()
    .middleware(new JsonMiddleware())
    .get("/hello", exchange -> exchange.response().json(new Greeting("Hello!")))
    .post("/echo", exchange -> {
        var body = exchange.bodyAs(Greeting.class);
        exchange.response().json(body);
    })
    .build();
```

## Maven setup

Add the modules you need. The three below cover most applications:

```xml
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-foundation</artifactId>
  <version>0.1.1</version>
</dependency>
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-transport-http</artifactId>
  <version>0.1.1</version>
</dependency>
<dependency>
  <groupId>build.serve</groupId>
  <artifactId>serve-application</artifactId>
  <version>0.1.1</version>
</dependency>
```

Enable preview in the compiler plugin:

```xml
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <configuration>
    <release>25</release>
    <compilerArgs><arg>--enable-preview</arg></compilerArgs>
  </configuration>
</plugin>
```

Declare your module:

```java
module com.example.myapp {
    requires build.serve.foundation;
    requires build.serve.transport.http;
    requires build.serve.application;
}
```

## Run the example app

The repo ships a full demo covering REST, GraphQL, WebSocket, JTE templates,
HTMX, and health endpoints:

```bash
./mvnw -pl serve-example -am exec:exec
```

Visit `http://localhost:8080` to explore the running application.

## Next steps

- [Routing](/docs/routing) — path parameters, sub-routers, error handling
- [Middleware](/docs/middleware) — logging, CORS, security headers, compression
