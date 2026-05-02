package build.serve.foundation.concurrent;

import build.base.json.JsonValue;
import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.context.RequestContext;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcurrentTests {

    @Test
    void forkTwoTasksReturnsBothResults() throws InterruptedException {
        var result = Concurrent.fork(() -> "hello", () -> 42);

        assertThat(result.first()).isEqualTo("hello");
        assertThat(result.second()).isEqualTo(42);
    }

    @Test
    void forkThreeTasksReturnsAllResults() throws InterruptedException {
        var result = Concurrent.fork(() -> "a", () -> "b", () -> "c");

        assertThat(result.first()).isEqualTo("a");
        assertThat(result.second()).isEqualTo("b");
        assertThat(result.third()).isEqualTo("c");
    }

    @Test
    void forkAllReturnsTenResults() throws InterruptedException {
        List<Callable<Integer>> tasks = IntStream.range(0, 10)
            .<Callable<Integer>>mapToObj(i -> () -> i * i)
            .toList();

        var results = Concurrent.forkAll(tasks);

        assertThat(results).hasSize(10);
        assertThat(results.get(3)).isEqualTo(9);
    }

    @Test
    void failedSubtaskPropagatesException() {
        assertThatThrownBy(() -> Concurrent.fork(
            () -> "ok",
            () -> {
                throw new IllegalStateException("boom");
            }
        )).hasCauseInstanceOf(IllegalStateException.class)
            .hasRootCauseMessage("boom");
    }

    @Test
    void scopedValuesPropagateIntoForkedSubtasks() throws InterruptedException {
        var exchange = createExchange();

        RequestContext.run(exchange, () -> {
            try {
                final var outerRequestId = RequestContext.REQUEST_ID.get();
                final var outerExchange = RequestContext.EXCHANGE.get();

                var result = Concurrent.fork(
                    () -> RequestContext.REQUEST_ID.get(),
                    () -> RequestContext.EXCHANGE.get()
                );

                assertThat(result.first()).isEqualTo(outerRequestId);
                assertThat(result.second()).isSameAs(outerExchange);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    private static Exchange createExchange() {
        return new SimpleExchange(new StubRequest(), new StubResponse());
    }

    private static class StubRequest implements Request {
        @Override
        public String method() {
            return "GET";
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create("/");
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public java.util.Optional<String> pathParam(final String n) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<String> queryParams(final String n) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<String> queryParam(final String n) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<String, java.util.List<String>> headers() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Optional<String> header(final String n) {
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

    private static class StubResponse implements Response {
        @Override
        public Response status(final int s) {
            return this;
        }

        @Override
        public int status() {
            return 200;
        }

        @Override
        public Response header(final String n, final String v) {
            return this;
        }

        @Override
        public void send(final String b) {
        }

        @Override
        public void send(final byte[] b) {
        }

        @Override
        public void json(final Object b) {
        }

        @Override
        public java.io.OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
