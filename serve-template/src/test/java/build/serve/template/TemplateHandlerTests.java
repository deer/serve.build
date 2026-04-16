package build.serve.template;

import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.SimpleExchange;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateHandlerTests {

    @Test
    void templateOutputOfWriterWritesContent() {
        var writer = new StringWriter();
        var output = TemplateOutput.of(writer);

        output.writeContent("Hello, World!");

        assertThat(writer.toString()).isEqualTo("Hello, World!");
    }

    @Test
    void templateOutputOfWriterWritesCharArray() {
        var writer = new StringWriter();
        var output = TemplateOutput.of(writer);
        var chars = "Hello, World!".toCharArray();

        output.writeContent(chars, 7, 5);

        assertThat(writer.toString()).isEqualTo("World");
    }

    @Test
    void captureAccumulatesContent() {
        var output = TemplateOutput.capture();

        output.writeContent("Hello");
        output.writeContent(", ");
        output.writeContent("World!");

        assertThat(output.get()).isEqualTo("Hello, World!");
    }

    @Test
    void captureCharArraySlice() {
        var output = TemplateOutput.capture();
        var chars = "Hello, World!".toCharArray();

        output.writeContent(chars, 0, 5);

        assertThat(output.get()).isEqualTo("Hello");
    }

    @Test
    void templateHandlerRendersHtmlResponse() throws Exception {
        TemplateEngine engine = name -> (templateOutput, params) ->
            templateOutput.writeContent("<h1>" + params.get("title") + "</h1>");

        var params = Map.<String, Object>of("title", "Test Page");
        var response = new StubResponse();
        var exchange = new SimpleExchange(new StubRequest(), response);

        TemplateHandler.render(engine, "test.html", params).handle(exchange);

        assertThat(response.body).isEqualTo("<h1>Test Page</h1>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.headers).containsEntry("Content-Type", "text/html; charset=utf-8");
    }

    @Test
    void templateHandlerWithParamsBuilderPassesExchange() throws Exception {
        TemplateEngine engine = name -> (templateOutput, params) ->
            templateOutput.writeContent("<p>" + params.get("value") + "</p>");

        var response = new StubResponse();
        var exchange = new SimpleExchange(new StubRequest(), response);

        TemplateHandler.render(engine, "test.html", ex -> Map.of("value", "dynamic")).handle(exchange);

        assertThat(response.body).isEqualTo("<p>dynamic</p>".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.headers).containsEntry("Content-Type", "text/html; charset=utf-8");
    }

    @Test
    void templateOutputOfOutputStreamWritesUtf8Content() throws Exception {
        var baos = new ByteArrayOutputStream();
        var output = TemplateOutput.of(baos);

        output.writeContent("Héllo");

        assertThat(baos.toByteArray()).isEqualTo("Héllo".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void templateEngineRenderShorthandWorks() throws Exception {
        TemplateEngine engine = name -> (templateOutput, params) ->
            templateOutput.writeContent("rendered:" + name);

        var output = TemplateOutput.capture();
        engine.render("page.html", output, Map.of());

        assertThat(output.get()).isEqualTo("rendered:page.html");
    }

    private static final class StubRequest implements Request {

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
        public Optional<String> pathParam(final String name) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(final String name) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(final String name) {
            return Optional.empty();
        }

        @Override
        public Map<String, List<String>> headers() {
            return Map.of();
        }

        @Override
        public Optional<String> header(final String name) {
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
        public <T> T body(final Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StubResponse implements Response {

        final Map<String, String> headers = new LinkedHashMap<>();
        byte[] body;

        @Override
        public Response status(final int statusCode) {
            return this;
        }

        @Override
        public int status() {
            return 200;
        }

        @Override
        public Response header(final String name, final String value) {
            headers.put(name, value);
            return this;
        }

        @Override
        public void send(final String body) {
            this.body = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }

        @Override
        public void send(final byte[] body) {
            this.body = body;
        }

        @Override
        public void json(final Object body) {
        }

        @Override
        public OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
