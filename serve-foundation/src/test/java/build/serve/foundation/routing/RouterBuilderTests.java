package build.serve.foundation.routing;

import build.base.json.JsonValue;
import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.error.NotFoundException;
import build.serve.foundation.middleware.Middleware;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RouterBuilderTests {

    @Test
    void routesGetRequestToCorrectHandler() throws Exception {
        var called = new AtomicReference<String>();

        var router = RouterBuilder.create()
            .get("/hello", exchange -> called.set("hello"))
            .get("/world", exchange -> called.set("world"))
            .build();

        router.handle(exchangeFor("GET", "/hello"));

        assertThat(called.get()).isEqualTo("hello");
    }

    @Test
    void routesPostRequestToCorrectHandler() throws Exception {
        var called = new AtomicReference<String>();

        var router = RouterBuilder.create()
            .post("/submit", exchange -> called.set("submitted"))
            .build();

        router.handle(exchangeFor("POST", "/submit"));

        assertThat(called.get()).isEqualTo("submitted");
    }

    @Test
    void throwsNotFoundForUnmatchedPath() {
        var router = RouterBuilder.create()
            .get("/hello", exchange -> exchange.response().send("hi"))
            .build();

        assertThatThrownBy(() -> router.handle(exchangeFor("GET", "/nope")))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("/nope");
    }

    @Test
    void throwsNotFoundForWrongMethod() {
        var router = RouterBuilder.create()
            .get("/hello", exchange -> exchange.response().send("hi"))
            .build();

        assertThatThrownBy(() -> router.handle(exchangeFor("POST", "/hello")))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("POST");
    }

    @Test
    void middlewareIsApplied() throws Exception {
        var order = new ArrayList<String>();

        Middleware mw = next -> exchange -> {
            order.add("before");
            next.handle(exchange);
            order.add("after");
        };

        var router = RouterBuilder.create()
            .middleware(mw)
            .get("/test", exchange -> order.add("handler"))
            .build();

        router.handle(exchangeFor("GET", "/test"));

        assertThat(order).containsExactly("before", "handler", "after");
    }

    @Test
    void multipleMiddlewareAppliedInCorrectOrder() throws Exception {
        var order = new ArrayList<String>();

        Middleware first = next -> exchange -> {
            order.add("first-before");
            next.handle(exchange);
            order.add("first-after");
        };

        Middleware second = next -> exchange -> {
            order.add("second-before");
            next.handle(exchange);
            order.add("second-after");
        };

        var router = RouterBuilder.create()
            .middleware(first)
            .middleware(second)
            .get("/test", exchange -> order.add("handler"))
            .build();

        router.handle(exchangeFor("GET", "/test"));

        // first registered = outermost
        assertThat(order).containsExactly(
            "first-before", "second-before", "handler", "second-after", "first-after");
    }

    @Test
    void pathParamsAvailableViaExchange() throws Exception {
        var captured = new AtomicReference<Map<String, String>>();

        var router = RouterBuilder.create()
            .get("/users/{id}", exchange -> captured.set(exchange.pathParams()))
            .build();

        router.handle(exchangeFor("GET", "/users/42"));

        assertThat(captured.get()).containsEntry("id", "42");
    }

    @Test
    void nestedRouterMatchesSubPath() throws Exception {
        var called = new AtomicReference<String>();

        var subRouter = RouterBuilder.create()
            .get("/users", exchange -> called.set("listUsers"))
            .get("/users/{id}", exchange -> called.set("getUser-" + exchange.pathParams().get("id")))
            .build();

        var router = RouterBuilder.create()
            .get("/health", exchange -> called.set("health"))
            .route("/api/v1", subRouter)
            .build();

        router.handle(exchangeFor("GET", "/api/v1/users"));
        assertThat(called.get()).isEqualTo("listUsers");

        router.handle(exchangeFor("GET", "/api/v1/users/42"));
        assertThat(called.get()).isEqualTo("getUser-42");
    }

    @Test
    void nestedRouterWithPathParamsInPrefix() throws Exception {
        var captured = new AtomicReference<Map<String, String>>();

        var postsRouter = RouterBuilder.create()
            .get("/posts", exchange -> captured.set(exchange.pathParams()))
            .build();

        var router = RouterBuilder.create()
            .route("/users/{userId}", postsRouter)
            .build();

        router.handle(exchangeFor("GET", "/users/99/posts"));

        assertThat(captured.get())
            .containsEntry("userId", "99");
    }

    @Test
    void nestedRouterReturns404ForUnmatchedSubPath() {
        var subRouter = RouterBuilder.create()
            .get("/known", exchange -> exchange.response().send("ok"))
            .build();

        var router = RouterBuilder.create()
            .route("/api", subRouter)
            .build();

        assertThatThrownBy(() -> router.handle(exchangeFor("GET", "/api/unknown")))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void deeplyNestedRouters() throws Exception {
        var called = new AtomicReference<String>();

        var level3 = RouterBuilder.create()
            .get("/action", exchange -> called.set("deep"))
            .build();

        var level2 = RouterBuilder.create()
            .route("/c", level3)
            .build();

        var level1 = RouterBuilder.create()
            .route("/a/b", level2)
            .build();

        level1.handle(exchangeFor("GET", "/a/b/c/action"));

        assertThat(called.get()).isEqualTo("deep");
    }

    @Test
    void middlewareOnParentAppliestoNestedRoutes() throws Exception {
        var order = new ArrayList<String>();

        Middleware parentMw = next -> exchange -> {
            order.add("parent-before");
            next.handle(exchange);
            order.add("parent-after");
        };

        var subRouter = RouterBuilder.create()
            .get("/item", exchange -> order.add("handler"))
            .build();

        var router = RouterBuilder.create()
            .middleware(parentMw)
            .route("/sub", subRouter)
            .build();

        router.handle(exchangeFor("GET", "/sub/item"));

        assertThat(order).containsExactly("parent-before", "handler", "parent-after");
    }

    @Test
    void shouldRootPrefixRouterMatchAllSubPaths() throws Exception {
        var called = new AtomicReference<String>();

        var subRouter = RouterBuilder.create()
            .get("/tasks", exchange -> called.set("list"))
            .get("/tasks/{id}/toggle", exchange -> called.set("toggle-" + exchange.pathParams().get("id")))
            .build();

        var router = RouterBuilder.create()
            .route("/", subRouter)
            .build();

        router.handle(exchangeFor("GET", "/tasks"));
        assertThat(called.get()).isEqualTo("list");

        router.handle(exchangeFor("GET", "/tasks/42/toggle"));
        assertThat(called.get()).isEqualTo("toggle-42");
    }

    @Test
    void middlewareOnSubRouterOnlyAppliesToSubRoutes() throws Exception {
        var order = new ArrayList<String>();

        Middleware subMw = next -> exchange -> {
            order.add("sub-before");
            next.handle(exchange);
            order.add("sub-after");
        };

        var subRouter = RouterBuilder.create()
            .middleware(subMw)
            .get("/item", exchange -> order.add("sub-handler"))
            .build();

        var router = RouterBuilder.create()
            .get("/top", exchange -> order.add("top-handler"))
            .route("/nested", subRouter)
            .build();

        // Sub-route should have sub-middleware
        router.handle(exchangeFor("GET", "/nested/item"));
        assertThat(order).containsExactly("sub-before", "sub-handler", "sub-after");

        // Top-level should NOT have sub-middleware
        order.clear();
        router.handle(exchangeFor("GET", "/top"));
        assertThat(order).containsExactly("top-handler");
    }

    // --- group() ---

    @Test
    void shouldApplyGroupMiddlewareToRoutesInsideGroup() throws Exception {
        var order = new ArrayList<String>();

        var router = RouterBuilder.create()
            .get("/health", exchange -> order.add("health"))
            .group(g -> g
                .middleware(next -> exchange -> {
                    order.add("auth");
                    next.handle(exchange);
                })
                .get("/api/data", exchange -> order.add("data")))
            .build();

        router.handle(exchangeFor("GET", "/api/data"));
        assertThat(order).containsExactly("auth", "data");
    }

    @Test
    void shouldNotApplyGroupMiddlewareToRoutesOutsideGroup() throws Exception {
        var order = new ArrayList<String>();

        var router = RouterBuilder.create()
            .get("/health", exchange -> order.add("health"))
            .group(g -> g
                .middleware(next -> exchange -> {
                    order.add("auth");
                    next.handle(exchange);
                })
                .get("/api/data", exchange -> order.add("data")))
            .build();

        router.handle(exchangeFor("GET", "/health"));
        assertThat(order).containsExactly("health");
    }

    @Test
    void shouldPreserveFullPathsInsideGroup() throws Exception {
        var called = new AtomicReference<String>();

        var router = RouterBuilder.create()
            .group(g -> g
                .get("/mcp", exchange -> called.set("mcp"))
                .get("/sse", exchange -> called.set("sse")))
            .build();

        router.handle(exchangeFor("GET", "/mcp"));
        assertThat(called.get()).isEqualTo("mcp");

        router.handle(exchangeFor("GET", "/sse"));
        assertThat(called.get()).isEqualTo("sse");
    }

    @Test
    void shouldApplyMultipleGroupMiddlewareInOrder() throws Exception {
        var order = new ArrayList<String>();

        var router = RouterBuilder.create()
            .group(g -> g
                .middleware(next -> exchange -> {
                    order.add("first");
                    next.handle(exchange);
                })
                .middleware(next -> exchange -> {
                    order.add("second");
                    next.handle(exchange);
                })
                .get("/route", exchange -> order.add("handler")))
            .build();

        router.handle(exchangeFor("GET", "/route"));
        assertThat(order).containsExactly("first", "second", "handler");
    }

    @Test
    void shouldSupportMultipleGroupsWithDifferentMiddleware() throws Exception {
        var order = new ArrayList<String>();

        var router = RouterBuilder.create()
            .group(g -> g
                .middleware(next -> exchange -> {
                    order.add("auth");
                    next.handle(exchange);
                })
                .get("/protected", exchange -> order.add("protected")))
            .group(g -> g
                .middleware(next -> exchange -> {
                    order.add("log");
                    next.handle(exchange);
                })
                .get("/logged", exchange -> order.add("logged")))
            .build();

        router.handle(exchangeFor("GET", "/protected"));
        assertThat(order).containsExactly("auth", "protected");

        order.clear();
        router.handle(exchangeFor("GET", "/logged"));
        assertThat(order).containsExactly("log", "logged");
    }

    // --- Helpers ---

    private static Exchange exchangeFor(String method, String path) {
        return exchangeWith(stubRequest(method, path), new StubResponse());
    }

    private static Exchange exchangeWith(Request request, Response response) {
        return new SimpleExchange(request, response);
    }

    private static StubRequest stubRequest(String method, String path) {
        return new StubRequest(method, path);
    }

    /**
     * Minimal stub Request for unit testing the router without HTTP.
     */
    private static class StubRequest implements Request {
        private final String method;
        private final String path;

        StubRequest(String method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public String method() {
            return method;
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create(path);
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public java.util.Optional<String> pathParam(String name) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<String> queryParams(String name) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<String> queryParam(String name) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<String, java.util.List<String>> headers() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Optional<String> header(String name) {
            return java.util.Optional.empty();
        }

        @Override
        public java.io.InputStream bodyAsStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public JsonValue bodyAsJson() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Minimal stub Response for unit testing the router without HTTP.
     */
    private static class StubResponse implements Response {
        int statusCode = 200;
        String body;

        @Override
        public Response status(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        @Override
        public int status() {
            return statusCode;
        }

        @Override
        public Response header(String name, String value) {
            return this;
        }

        @Override
        public void send(String body) {
            this.body = body;
        }

        @Override
        public void send(byte[] body) {
            this.body = new String(body);
        }

        @Override
        public void json(Object body) {
            throw new UnsupportedOperationException();
        }

        @Override
        public java.io.OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
