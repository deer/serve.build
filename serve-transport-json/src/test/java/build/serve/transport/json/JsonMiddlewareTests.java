package build.serve.transport.json;

import build.serve.foundation.Handler;
import build.serve.foundation.routing.RouterBuilder;
import build.serve.transport.http.HttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class JsonMiddlewareTests {

    private HttpTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.stop(0);
        }
    }

    @Test
    void postJsonBodyAndRespondWithJson() throws Exception {
        var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .post("/echo", exchange -> {
                var input = exchange.bodyAs(TestMessage.class);
                exchange.sendBody(new TestMessage(input.text().toUpperCase()));
            })
            .build();

        startWith(router);

        var conn = post("/echo", """
            {"text":"hello"}""");

        assertThat(conn.getResponseCode()).isEqualTo(200);
        var body = readBody(conn);
        assertThat(body).contains("\"text\"");
        assertThat(body).contains("HELLO");
    }

    @Test
    void contentTypeSetToApplicationJson() throws Exception {
        var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .get("/json", exchange -> exchange.sendBody(new TestMessage("hi")))
            .build();

        startWith(router);

        var conn = get("/json");

        assertThat(conn.getContentType()).startsWith("application/json");
    }

    @Test
    void invalidJsonReturnsError() throws Exception {
        var router = RouterBuilder.create()
            .middleware(new JsonMiddleware())
            .post("/parse", exchange -> {
                try {
                    exchange.bodyAs(TestMessage.class);
                    exchange.response().send("ok");
                } catch (RuntimeException e) {
                    exchange.response().status(400).send("Bad JSON");
                }
            })
            .build();

        startWith(router);

        var conn = post("/parse", "not-json{{{");

        assertThat(conn.getResponseCode()).isEqualTo(400);
        assertThat(readErrorBody(conn)).isEqualTo("Bad JSON");
    }

    // --- Helpers ---

    public static class TestMessage {
        private String text;

        public TestMessage() {
        }

        public TestMessage(String text) {
            this.text = text;
        }

        public String text() {
            return text;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }

    private void startWith(Handler handler) throws Exception {
        transport = new HttpTransport(new InetSocketAddress("127.0.0.1", 0), 0, handler);
        transport.start();
    }

    private HttpURLConnection openConnection(String path) throws Exception {
        var port = transport.address().getPort();
        var url = URI.create("http://127.0.0.1:" + port + path).toURL();
        return (HttpURLConnection) url.openConnection();
    }

    private HttpURLConnection get(String path) throws Exception {
        var conn = openConnection(path);
        conn.setRequestMethod("GET");
        conn.connect();
        return conn;
    }

    private HttpURLConnection post(String path, String body) throws Exception {
        var conn = openConnection(path);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        return conn;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        try (var is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String readErrorBody(HttpURLConnection conn) throws Exception {
        try (var is = conn.getErrorStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
