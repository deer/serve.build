package build.serve.transport.http;

import build.serve.foundation.Handler;
import build.serve.foundation.option.MaxRequestSize;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;

class HttpTransportTests {

    private HttpTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.stop(0);
        }
    }

    @Test
    void getRequestReturnsBodyAndStatus() throws Exception {
        startWith(exchange -> exchange.response().status(200).send("OK"));

        var conn = get("/");

        assertThat(conn.getResponseCode()).isEqualTo(200);
        assertThat(readBody(conn)).isEqualTo("OK");
    }

    @Test
    void contentTypeDefaultsToTextPlain() throws Exception {
        startWith(exchange -> exchange.response().send("text"));

        var conn = get("/");

        assertThat(conn.getContentType()).startsWith("text/plain");
    }

    @Test
    void customHeadersAreSent() throws Exception {
        startWith(exchange ->
            exchange.response()
                .header("X-Custom", "hello")
                .send("ok"));

        var conn = get("/");

        assertThat(conn.getHeaderField("X-Custom")).isEqualTo("hello");
    }

    @Test
    void postWithBodyIsReadable() throws Exception {
        startWith(exchange -> {
            var body = exchange.request().bodyAsString();
            exchange.response().send("echo:" + body);
        });

        var conn = openConnection("/");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write("hello".getBytes(StandardCharsets.UTF_8));
        }

        assertThat(readBody(conn)).isEqualTo("echo:hello");
    }

    @Test
    void multipleConcurrentRequests() throws Exception {
        startWith(exchange -> exchange.response().send("ok"));

        var futures = new ArrayList<Future<Integer>>();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < 50; i++) {
                futures.add(executor.submit(() -> get("/").getResponseCode()));
            }

            for (var future : futures) {
                assertThat(future.get()).isEqualTo(200);
            }
        }
    }

    @Test
    void handlerThrowingReturns500() throws Exception {
        startWith(exchange -> {
            throw new RuntimeException("boom");
        });

        var conn = get("/");

        assertThat(conn.getResponseCode()).isEqualTo(500);
    }

    @Test
    void shouldCapQueryParamsAt100() throws Exception {
        startWith(exchange -> {
            var values = exchange.request().queryParams("x");
            exchange.response().send(String.valueOf(values.size()));
        });

        var query = "x=1&".repeat(101).stripTrailing().replaceAll("&$", "");
        var conn = openConnection("/?" + query);
        conn.connect();

        // 101 params sent — server should see at most 100
        assertThat(Integer.parseInt(readBody(conn))).isLessThanOrEqualTo(100);
    }

    @Test
    void shouldCapCookiesAt50() throws Exception {
        startWith(exchange -> {
            var cookies = exchange.request().cookies();
            exchange.response().send(String.valueOf(cookies.size()));
        });

        var cookieHeader = new StringBuilder();
        for (int i = 0; i < 51; i++) {
            if (i > 0) cookieHeader.append("; ");
            cookieHeader.append("c").append(i).append("=v");
        }

        var conn = openConnection("/");
        conn.setRequestProperty("Cookie", cookieHeader.toString());
        conn.connect();

        assertThat(Integer.parseInt(readBody(conn))).isLessThanOrEqualTo(50);
    }

    @Test
    void shouldReturn413WhenBodyExceedsMaxRequestSize() throws Exception {
        var address = new InetSocketAddress("127.0.0.1", 0);
        transport = new HttpTransport(address, 0,
            exchange -> {
                exchange.request().bodyAsString();
                exchange.response().send("ok");
            },
            (ex, e) -> ex.response().status(e instanceof build.serve.foundation.error.HttpException he
                ? he.statusCode() : 500).send(e.getMessage()),
            MaxRequestSize.of(5));
        transport.start();

        var conn = openConnection("/");
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write("this body is longer than 5 bytes".getBytes(StandardCharsets.UTF_8));
        }

        assertThat(conn.getResponseCode()).isEqualTo(413);
    }

    @Test
    void httpsFactoryBindsToPort() throws Exception {
        // SSLContext.getDefault() has no key material but is sufficient to verify that
        // HttpTransport.https() creates and starts the server — no TLS connection is made.
        var ctx = SSLContext.getDefault();
        transport = HttpTransport.https(new InetSocketAddress("127.0.0.1", 0), 0,
            exchange -> exchange.response().status(200).send("OK"),
            ctx);
        transport.start();

        assertThat(transport.address().getPort()).isGreaterThan(0);
    }

    @Test
    void httpsFactoryWithErrorHandlerBindsToPort() throws Exception {
        var ctx = SSLContext.getDefault();
        transport = HttpTransport.https(new InetSocketAddress("127.0.0.1", 0), 0,
            exchange -> exchange.response().status(200).send("OK"),
            (exchange, ex) -> exchange.response().status(500).send("error"),
            ctx);
        transport.start();

        assertThat(transport.address().getPort()).isGreaterThan(0);
    }

    // --- Helpers ---

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

    private String readBody(HttpURLConnection conn) throws Exception {
        try (var is = conn.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
