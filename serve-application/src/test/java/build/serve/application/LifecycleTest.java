package build.serve.application;

import build.base.network.option.Port;
import build.serve.foundation.option.ListenAddress;
import build.serve.foundation.option.ShutdownTimeout;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LifecycleTest {

    private ServerApplication.Implementation server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void serverStartCompletesOnStartFuture() throws Exception {
        server = createServer();
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));

        var result = server.onStart().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(result).isSameAs(server);
    }

    @Test
    void serverCloseCompletesOnExitFuture() throws Exception {
        server = createServer();
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));

        server.close();

        var result = server.onExit().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(result).isSameAs(server);
        server = null; // already closed
    }

    @Test
    void boundAddressIsNonNullAfterStart() throws Exception {
        server = createServer();
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));

        assertThat(server.boundAddress()).isNotNull();
        assertThat(server.boundAddress().getPort()).isGreaterThan(0);
    }

    @Test
    void stopIsIdempotent() throws Exception {
        server = createServer();
        server.start(ListenAddress.of("127.0.0.1"), Port.of(0));

        server.stop();
        server.stop(); // second call should not throw

        var result = server.onExit().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(result).isSameAs(server);
        server = null; // already stopped
    }

    @Test
    void customShutdownTimeoutIsUsed() throws Exception {
        server = createServer();
        var timeout = ShutdownTimeout.ofSeconds(1);

        server.start(ListenAddress.of("127.0.0.1"), Port.of(0), timeout);

        assertThat(server.boundAddress()).isNotNull();

        server.stop();

        var result = server.onExit().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertThat(result).isSameAs(server);
        server = null; // already stopped
    }

    private ServerApplication.Implementation createServer() {
        return new ServerApplication.Implementation() {
            @Override
            protected Router configure() {
                return RouterBuilder.create()
                    .get("/ping", exchange -> exchange.response().send("pong"))
                    .build();
            }
        };
    }
}
