package build.serve.foundation.middleware;

import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.foundation.PrintStreamTelemetryRecorder;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TelemetryMiddlewareTest {

    @Test
    void completesActivityOnSuccess() throws Exception {
        var output = new ByteArrayOutputStream();
        var recorder = createRecorder(output);
        var middleware = new TelemetryMiddleware(recorder);

        var called = new AtomicBoolean(false);
        Handler inner = exchange -> called.set(true);

        var handler = middleware.apply(inner);
        handler.handle(createExchange("GET", "/hello"));

        assertThat(called).isTrue();
        assertThat(output.toString()).contains("GET /hello");
    }

    @Test
    void completesExceptionallyOnFailure() {
        var output = new ByteArrayOutputStream();
        var errOutput = new ByteArrayOutputStream();
        var recorder = PrintStreamTelemetryRecorder.of(
            URI.create("test://telemetry"),
            new PrintStream(output),
            new PrintStream(errOutput));
        var middleware = new TelemetryMiddleware(recorder);

        Handler inner = exchange -> {
            throw new RuntimeException("boom");
        };

        var handler = middleware.apply(inner);

        assertThatThrownBy(() -> handler.handle(createExchange("POST", "/fail")))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("boom");

        var allOutput = output.toString() + errOutput.toString();
        assertThat(allOutput).contains("POST /fail");
    }

    @Test
    void rejectsNullRecorder() {
        assertThatThrownBy(() -> new TelemetryMiddleware(null))
            .isInstanceOf(NullPointerException.class);
    }

    private static TelemetryRecorder createRecorder(ByteArrayOutputStream output) {
        return PrintStreamTelemetryRecorder.of(
            URI.create("test://telemetry"),
            new PrintStream(output),
            new PrintStream(output));
    }

    private static Exchange createExchange(String method, String path) {
        return new SimpleExchange(new StubRequest(method, path), new StubResponse());
    }

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
        public URI uri() {
            return URI.create(path);
        }

        @Override
        public String path() {
            return path;
        }

        @Override
        public Optional<String> pathParam(String n) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(String n) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(String n) {
            return Optional.empty();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Map.of();
        }

        @Override
        public Optional<String> header(String n) {
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
            return java.io.OutputStream.nullOutputStream();
        }
    }
}
