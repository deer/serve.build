package build.serve.foundation.context;

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestContextTests {

    @Test
    void exchangeIsAvailableInsideRun() {
        var exchange = createExchange();

        RequestContext.run(exchange, () -> {
            assertThat(RequestContext.EXCHANGE.get()).isSameAs(exchange);
        });
    }

    @Test
    void requestIdIsNonNullUuidInsideRun() {
        RequestContext.run(createExchange(), () -> {
            var id = RequestContext.REQUEST_ID.get();
            assertThat(id).isNotNull();
            assertThat(java.util.UUID.fromString(id)).isNotNull();
        });
    }

    @Test
    void startTimeIsSetInsideRun() {
        RequestContext.run(createExchange(), () -> {
            assertThat(RequestContext.START_TIME.get()).isNotNull();
        });
    }

    @Test
    void valuesAreNotAvailableOutsideRun() {
        assertThatThrownBy(() -> RequestContext.EXCHANGE.get())
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> RequestContext.REQUEST_ID.get())
            .isInstanceOf(NoSuchElementException.class);
        assertThatThrownBy(() -> RequestContext.START_TIME.get())
            .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void callReturnsResultWithContextBound() throws Throwable {
        var exchange = createExchange();

        var id = RequestContext.call(exchange, () -> RequestContext.REQUEST_ID.get());

        assertThat(id).isNotNull();
        assertThat(java.util.UUID.fromString(id)).isNotNull();
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
        public <T> T body(final Class<T> t) {
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
