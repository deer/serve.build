package build.serve.foundation;

import build.base.network.option.Port;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleExchangeTests {

    @Test
    void optionCanBeSetAndRetrieved() {
        var exchange = createExchange();

        exchange.option(Port.of(9090));

        assertThat(exchange.option(Port.class))
            .isPresent()
            .hasValueSatisfying(p -> assertThat(p.get()).isEqualTo(9090));
    }

    @Test
    void attributeCanBeSetAndRetrieved() {
        var exchange = createExchange();

        exchange.attribute("key", "value");

        assertThat(exchange.attribute("key", String.class))
            .isPresent()
            .hasValue("value");
    }

    @Test
    void missingOptionReturnsEmpty() {
        var exchange = createExchange();

        assertThat(exchange.option(Port.class)).isEmpty();
    }

    @Test
    void missingAttributeReturnsEmpty() {
        var exchange = createExchange();

        assertThat(exchange.attribute("nonexistent", String.class)).isEmpty();
    }

    private static SimpleExchange createExchange() {
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
        public java.util.Optional<String> pathParam(String n) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.List<String> queryParams(String n) {
            return java.util.List.of();
        }

        @Override
        public java.util.Optional<String> queryParam(String n) {
            return java.util.Optional.empty();
        }

        @Override
        public java.util.Map<String, java.util.List<String>> headers() {
            return java.util.Map.of();
        }

        @Override
        public java.util.Optional<String> header(String n) {
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
        public <T> T body(Class<T> t) {
            throw new UnsupportedOperationException();
        }
    }

    private static class StubResponse implements Response {
        @Override
        public Response status(int s) {
            return this;
        }

        @Override
        public int status() {
            return 200;
        }

        @Override
        public Response header(String n, String v) {
            return this;
        }

        @Override
        public void send(String b) {
        }

        @Override
        public void send(byte[] b) {
        }

        @Override
        public void json(Object b) {
        }

        @Override
        public java.io.OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
