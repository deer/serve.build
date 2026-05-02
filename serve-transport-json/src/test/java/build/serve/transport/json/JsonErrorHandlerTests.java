package build.serve.transport.json;

import build.base.json.Json;
import build.serve.foundation.Exchange;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.error.BadRequestException;
import build.serve.foundation.error.NotFoundException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JsonErrorHandlerTests {

    private final JsonErrorHandler handler = new JsonErrorHandler();

    @Test
    void shouldReturnJsonResponseForHttpException() throws Exception {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new NotFoundException("User 42 not found"));

        assertThat(response.statusCode).isEqualTo(404);
        assertThat(response.headers.get("Content-Type")).isEqualTo("application/json");

        var json = Json.parse(response.body).asObject();
        assertThat(json.get("status").asNumber().toNumber().intValue()).isEqualTo(404);
        assertThat(json.getString("error")).isEqualTo("Not Found");
        assertThat(json.getString("message")).isEqualTo("User 42 not found");
    }

    @Test
    void shouldReturnBadRequestJson() throws Exception {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new BadRequestException("missing field"));

        assertThat(response.statusCode).isEqualTo(400);

        var json = Json.parse(response.body).asObject();
        assertThat(json.get("status").asNumber().toNumber().intValue()).isEqualTo(400);
        assertThat(json.getString("error")).isEqualTo("Bad Request");
    }

    @Test
    void shouldReturn500JsonForGenericException() throws Exception {
        var response = new StubResponse();
        var exchange = exchangeWith(response);

        handler.handle(exchange, new RuntimeException("oops"));

        assertThat(response.statusCode).isEqualTo(500);

        var json = Json.parse(response.body).asObject();
        assertThat(json.get("status").asNumber().toNumber().intValue()).isEqualTo(500);
        assertThat(json.getString("message")).isEqualTo("Internal Server Error");
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
        public build.base.json.JsonValue bodyAsJson() {
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
