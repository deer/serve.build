package build.serve.application;

import build.base.network.option.Port;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.option.ListenAddress;
import build.serve.foundation.routing.Router;
import build.serve.foundation.routing.RouterBuilder;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyInjectionTest {

    private ServerApplication.Implementation server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.close();
        }
    }

    @Test
    void serverImplementationWithInjectFieldGetsInjected() throws Exception {
        server = new ServerWithInjectedPort();

        Launcher.launch(server, ListenAddress.of("127.0.0.1"), Port.of(0));

        var response = get("/port");

        // The handler returns the injected Port value
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("0");
    }

    @Test
    void diResolvedHandlerClassGetsInjected() throws Exception {
        server = new ServerWithDIHandler();

        Launcher.launch(server, ListenAddress.of("127.0.0.1"), Port.of(0));

        var response = get("/greeting");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("Hello from GreetingService");
    }

    // --- Test classes ---

    static class ServerWithInjectedPort extends ServerApplication.Implementation {
        @Inject
        Port listenPort;

        @Override
        protected Router configure() {
            return RouterBuilder.create()
                .get("/port", exchange ->
                    exchange.response().send(String.valueOf(listenPort.get())))
                .build();
        }
    }

    public static class GreetingService {
        public String greet() {
            return "Hello from GreetingService";
        }
    }

    public static class GreetingHandler implements Handler {
        @Inject
        GreetingService service;

        @Override
        public void handle(Exchange exchange) throws Exception {
            exchange.response().send(service.greet());
        }
    }

    static class ServerWithDIHandler extends ServerApplication.Implementation {
        @Override
        protected Router configure() {
            // Bind GreetingService into the DI context
            context().bind(GreetingService.class).to(GreetingService.class);

            return DependencyInjectionRouterBuilder.create(context())
                .get("/greeting", GreetingHandler.class)
                .build();
        }
    }

    // --- Helpers ---

    private HttpResponse<String> get(String path) throws Exception {
        var port = server.boundAddress().getPort();
        try (var client = HttpClient.newHttpClient()) {
            var req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .GET().build();
            return client.send(req, HttpResponse.BodyHandlers.ofString());
        }
    }
}
