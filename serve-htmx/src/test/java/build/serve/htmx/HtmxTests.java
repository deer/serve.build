package build.serve.htmx;

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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class HtmxTests {

    // --- HtmxRequest tests ---

    @Test
    void isHtmxRequestReturnsTrueWhenHeaderIsTrue() {
        var request = new StubRequest(Map.of(HtmxHeaders.HX_REQUEST, "true"));
        assertThat(HtmxRequest.of(request).isHtmxRequest()).isTrue();
    }

    @Test
    void targetReturnsCorrectValue() {
        var request = new StubRequest(Map.of(
            HtmxHeaders.HX_REQUEST, "true",
            HtmxHeaders.HX_TARGET, "my-element"
        ));
        assertThat(HtmxRequest.of(request).target()).contains("my-element");
    }

    @Test
    void nonHtmxRequestReturnsFalseAndEmpty() {
        var request = new StubRequest(Map.of());
        var htmxRequest = HtmxRequest.of(request);

        assertThat(htmxRequest.isHtmxRequest()).isFalse();
        assertThat(htmxRequest.isBoosted()).isFalse();
        assertThat(htmxRequest.isHistoryRestoreRequest()).isFalse();
        assertThat(htmxRequest.currentUrl()).isEmpty();
        assertThat(htmxRequest.prompt()).isEmpty();
        assertThat(htmxRequest.target()).isEmpty();
        assertThat(htmxRequest.triggerId()).isEmpty();
        assertThat(htmxRequest.triggerName()).isEmpty();
    }

    // --- HtmxResponse tests ---

    @Test
    void pushUrlSetsCorrectHeader() {
        var response = new StubResponse();
        HtmxResponse.of(response).pushUrl("/new-url");

        assertThat(response.headers).containsEntry(HtmxHeaders.HX_PUSH_URL, "/new-url");
    }

    @Test
    void triggerSetsCorrectHeader() {
        var response = new StubResponse();
        HtmxResponse.of(response).trigger("myEvent");

        assertThat(response.headers).containsEntry(HtmxHeaders.HX_TRIGGER, "myEvent");
    }

    @Test
    void fluentChainingWorks() {
        var response = new StubResponse();
        HtmxResponse.of(response)
            .pushUrl("/page")
            .retarget("#content")
            .reswap("outerHTML")
            .trigger("contentLoaded");

        assertThat(response.headers).containsEntry(HtmxHeaders.HX_PUSH_URL, "/page");
        assertThat(response.headers).containsEntry(HtmxHeaders.HX_RETARGET, "#content");
        assertThat(response.headers).containsEntry(HtmxHeaders.HX_RESWAP, "outerHTML");
        assertThat(response.headers).containsEntry(HtmxHeaders.HX_TRIGGER, "contentLoaded");
    }

    // --- HtmxMiddleware tests ---

    @Test
    void htmxOnlyBlocksNonHtmxRequests() throws Exception {
        var middleware = HtmxMiddleware.htmxOnly();
        var response = new StubResponse();
        var exchange = new SimpleExchange(new StubRequest(Map.of()), response);
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isFalse();
        assertThat(response.statusCode).isEqualTo(400);
    }

    @Test
    void htmxOnlyPassesThroughHtmxRequests() throws Exception {
        var middleware = HtmxMiddleware.htmxOnly();
        var response = new StubResponse();
        var exchange = new SimpleExchange(
            new StubRequest(Map.of(HtmxHeaders.HX_REQUEST, "true")),
            response
        );
        var nextCalled = new AtomicBoolean(false);

        middleware.apply(ex -> nextCalled.set(true)).handle(exchange);

        assertThat(nextCalled).isTrue();
        assertThat(response.statusCode).isEqualTo(200);
    }

    // --- Stubs ---

    private static final class StubRequest implements Request {

        private final Map<String, String> headers;

        StubRequest(final Map<String, String> headers) {
            this.headers = headers;
        }

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
            return Optional.ofNullable(headers.get(name));
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
        int statusCode = 200;

        @Override
        public Response status(final int statusCode) {
            this.statusCode = statusCode;

            return this;
        }

        @Override
        public int status() {
            return statusCode;
        }

        @Override
        public Response header(final String name, final String value) {
            headers.put(name, value);

            return this;
        }

        @Override
        public void send(final String body) {
        }

        @Override
        public void send(final byte[] body) {
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
