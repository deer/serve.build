package build.serve.foundation.error;

import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultErrorHandlerTests {

    private final DefaultErrorHandler handler = new DefaultErrorHandler();

    @Test
    void httpExceptionUsesStatusCodeAndMessage() {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new NotFoundException("User not found"));

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.body).isEqualTo("User not found");
        assertThat(response.headers.get("Content-Type")).isEqualTo("text/plain");
    }

    @Test
    void genericExceptionReturns500() {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new RuntimeException("oops"));

        assertThat(response.statusCode).isEqualTo(500);
        assertThat(response.body).isEqualTo("Internal Server Error");
    }

    @Test
    void doesNotLeakStackTrace() {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new RuntimeException("secret details"));

        assertThat(response.body).doesNotContain("secret details");
        assertThat(response.body).doesNotContain("RuntimeException");
    }

    private static Exchange exchangeWith(final Response response) {
        return new SimpleExchange(new StubRequest(), response);
    }

    private static class StubRequest implements Request {
        @Override
        public String method() {
            return "GET";
        }

        @Override
        public URI uri() {
            return URI.create("/");
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public Optional<String> pathParam(String name) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(String name) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(String name) {
            return Optional.empty();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Map.of();
        }

        @Override
        public Optional<String> header(String name) {
            return Optional.empty();
        }

        @Override
        public InputStream bodyAsStream() {
            return InputStream.nullInputStream();
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public <T> T body(Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }

    private static class StubResponse implements Response {
        int statusCode = 200;
        String body;
        final Map<String, String> headers = new LinkedHashMap<>();

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
            headers.put(name, value);
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
        public OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
