package build.serve.websocket;

import build.serve.foundation.routing.RouterBuilder;
import build.serve.transport.http.HttpTransport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WebSocketTests {

    private HttpTransport transport;

    @AfterEach
    void tearDown() {
        if (transport != null) {
            transport.stop(0);
        }
    }

    @Test
    void echoTextMessage() throws Exception {
        var received = new CompletableFuture<String>();

        startWith(ws -> ws.onText(message -> {
            try {
                ws.sendText("Echo: " + message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        var clientReceived = new CompletableFuture<String>();
        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(java.net.http.WebSocket webSocket,
                                                 CharSequence data,
                                                 boolean last) {
                    clientReceived.complete(data.toString());
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendText("hello", true);

        assertThat(clientReceived.get(5, TimeUnit.SECONDS)).isEqualTo("Echo: hello");

        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "done");
    }

    @Test
    void binaryMessage() throws Exception {
        var clientReceived = new CompletableFuture<byte[]>();

        startWith(ws -> ws.onBinary(data -> {
            try {
                ws.sendBinary(data);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onBinary(java.net.http.WebSocket webSocket,
                                                   ByteBuffer data,
                                                   boolean last) {
                    var bytes = new byte[data.remaining()];
                    data.get(bytes);
                    clientReceived.complete(bytes);
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendBinary(ByteBuffer.wrap(new byte[]{1, 2, 3}), true);

        assertThat(clientReceived.get(5, TimeUnit.SECONDS)).isEqualTo(new byte[]{1, 2, 3});

        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "done");
    }

    @Test
    void pingPong() throws Exception {
        var pongReceived = new CompletableFuture<String>();

        startWith(ws -> {
            // Default ping handler auto-pongs
        });

        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onPong(java.net.http.WebSocket webSocket,
                                                 ByteBuffer message) {
                    var bytes = new byte[message.remaining()];
                    message.get(bytes);
                    pongReceived.complete(new String(bytes));
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendPing(ByteBuffer.wrap("ping".getBytes()));

        assertThat(pongReceived.get(5, TimeUnit.SECONDS)).isEqualTo("ping");

        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "done");
    }

    @Test
    void cleanClose() throws Exception {
        var serverClosed = new CountDownLatch(1);

        startWith(ws -> ws.onClose(serverClosed::countDown));

        var clientClosed = new CompletableFuture<Integer>();
        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onClose(java.net.http.WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    clientClosed.complete(statusCode);
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "bye");

        assertThat(serverClosed.await(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void multipleMessages() throws Exception {
        var messages = new CopyOnWriteArrayList<String>();
        var allReceived = new CountDownLatch(3);

        startWith(ws -> ws.onText(message -> {
            try {
                ws.sendText("Got: " + message);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));

        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(java.net.http.WebSocket webSocket,
                                                 CharSequence data,
                                                 boolean last) {
                    messages.add(data.toString());
                    allReceived.countDown();
                    webSocket.request(1);
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendText("one", true);
        Thread.sleep(100);
        ws.sendText("two", true);
        Thread.sleep(100);
        ws.sendText("three", true);

        assertThat(allReceived.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(messages).containsExactly("Got: one", "Got: two", "Got: three");

        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "done");
    }

    @Test
    void shouldRejectUpgradeFromDisallowedOrigin() throws Exception {
        var router = RouterBuilder.create()
            .route("/ws", WebSocketUpgrade.upgrade(ws -> {}, Set.of("https://allowed.example.com")))
            .build();
        transport = new HttpTransport(new InetSocketAddress("127.0.0.1", 0), 0, router);
        transport.start();

        // HttpURLConnection silently drops Upgrade/Connection headers; use a raw socket instead
        var statusLine = sendRawUpgradeRequest(transport.address().getPort(), "https://evil.example.com");

        assertThat(statusLine).contains("403");
    }

    @Test
    void shouldAllowUpgradeFromAllowedOrigin() throws Exception {
        startWithOrigins(ws -> {}, Set.of("https://allowed.example.com"));

        var clientReceived = new CompletableFuture<String>();
        var client = HttpClient.newHttpClient();
        var ws = client.newWebSocketBuilder()
            .header("Origin", "https://allowed.example.com")
            .buildAsync(wsUri("/ws"), new java.net.http.WebSocket.Listener() {
                @Override
                public CompletionStage<?> onText(java.net.http.WebSocket webSocket,
                                                 CharSequence data, boolean last) {
                    clientReceived.complete(data.toString());
                    return null;
                }
            })
            .get(5, TimeUnit.SECONDS);

        ws.sendText("hi", true);
        ws.sendClose(java.net.http.WebSocket.NORMAL_CLOSURE, "done");
    }

    // --- Helpers ---

    private void startWith(WebSocketHandler wsHandler) throws Exception {
        var router = RouterBuilder.create()
            .route("/ws", WebSocketUpgrade.upgrade(wsHandler))
            .build();

        transport = new HttpTransport(new InetSocketAddress("127.0.0.1", 0), 0, router);
        transport.start();
    }

    private void startWithOrigins(WebSocketHandler wsHandler, Set<String> allowedOrigins) throws Exception {
        var router = RouterBuilder.create()
            .route("/ws", WebSocketUpgrade.upgrade(wsHandler, allowedOrigins))
            .build();

        transport = new HttpTransport(new InetSocketAddress("127.0.0.1", 0), 0, router);
        transport.start();
    }

    private String sendRawUpgradeRequest(int port, String origin) throws Exception {
        try (var socket = new Socket("127.0.0.1", port);
             var out = new PrintWriter(socket.getOutputStream(), true);
             var in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            var key = Base64.getEncoder().encodeToString(new byte[16]);
            out.print("GET /ws HTTP/1.1\r\n");
            out.print("Host: 127.0.0.1:" + port + "\r\n");
            out.print("Upgrade: websocket\r\n");
            out.print("Connection: Upgrade\r\n");
            out.print("Sec-WebSocket-Key: " + key + "\r\n");
            out.print("Sec-WebSocket-Version: 13\r\n");
            out.print("Origin: " + origin + "\r\n");
            out.print("\r\n");
            out.flush();
            return in.readLine(); // HTTP/1.1 403 Forbidden (or 101, etc.)
        }
    }

    private URI wsUri(String path) {
        var port = transport.address().getPort();
        return URI.create("ws://127.0.0.1:" + port + path);
    }
}
