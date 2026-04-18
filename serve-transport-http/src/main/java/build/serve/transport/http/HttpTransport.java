/*-
 * #%L
 * Serve Transport (HTTP)
 * %%
 * Copyright (C) 2026 Reed von Redwitz
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package build.serve.transport.http;

import build.base.logging.Logger;
import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.SimpleExchange;
import build.serve.foundation.context.RequestContext;
import build.serve.foundation.error.DefaultErrorHandler;
import build.serve.foundation.error.ErrorHandler;
import build.serve.foundation.option.MaxRequestSize;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import javax.net.ssl.SSLContext;

/**
 * An HTTP/HTTPS transport built on the JDK {@link HttpServer} with virtual thread execution.
 * <p>
 * Each incoming request is dispatched on its own virtual thread via
 * {@link Executors#newVirtualThreadPerTaskExecutor()}.
 * <p>
 * Use the constructors for plain HTTP, or the {@link #https} factory methods for TLS/HTTPS.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class HttpTransport {

    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.get(HttpTransport.class);

    /**
     * The underlying JDK {@link HttpServer}.
     */
    private final HttpServer httpServer;

    /**
     * The address this transport is bound to.
     */
    private final InetSocketAddress address;

    /**
     * The maximum allowed request body size.
     */
    private final MaxRequestSize maxRequestSize;

    /**
     * Constructs an {@link HttpTransport} with a default {@link DefaultErrorHandler}.
     *
     * @param address the address to bind to
     * @param backlog the maximum number of queued incoming connections (0 for system default)
     * @param handler the {@link Handler} to dispatch requests to
     * @throws IOException if the server cannot be created
     */
    public HttpTransport(final InetSocketAddress address,
                         final int backlog,
                         final Handler handler) throws IOException {
        this(address, backlog, handler, new DefaultErrorHandler(), MaxRequestSize.DEFAULT);
    }

    /**
     * Constructs an {@link HttpTransport}.
     *
     * @param address      the address to bind to
     * @param backlog      the maximum number of queued incoming connections (0 for system default)
     * @param handler      the {@link Handler} to dispatch requests to
     * @param errorHandler the {@link ErrorHandler} for unhandled exceptions
     * @throws IOException if the server cannot be created
     */
    public HttpTransport(final InetSocketAddress address,
                         final int backlog,
                         final Handler handler,
                         final ErrorHandler errorHandler) throws IOException {
        this(HttpServer.create(address, backlog), address, handler, errorHandler, MaxRequestSize.DEFAULT);
    }

    /**
     * Constructs an {@link HttpTransport} with a custom {@link MaxRequestSize}.
     *
     * @param address        the address to bind to
     * @param backlog        the maximum number of queued incoming connections (0 for system default)
     * @param handler        the {@link Handler} to dispatch requests to
     * @param errorHandler   the {@link ErrorHandler} for unhandled exceptions
     * @param maxRequestSize the maximum allowed request body size
     * @throws IOException if the server cannot be created
     */
    public HttpTransport(final InetSocketAddress address,
                         final int backlog,
                         final Handler handler,
                         final ErrorHandler errorHandler,
                         final MaxRequestSize maxRequestSize) throws IOException {
        this(HttpServer.create(address, backlog), address, handler, errorHandler, maxRequestSize);
    }

    /**
     * Creates an HTTPS {@link HttpTransport} with a default {@link DefaultErrorHandler}.
     *
     * @param address    the address to bind to
     * @param backlog    the maximum number of queued incoming connections (0 for system default)
     * @param handler    the {@link Handler} to dispatch requests to
     * @param sslContext the {@link SSLContext} used for TLS
     * @return a new HTTPS {@link HttpTransport}
     * @throws IOException if the server cannot be created
     */
    public static HttpTransport https(final InetSocketAddress address,
                                      final int backlog,
                                      final Handler handler,
                                      final SSLContext sslContext) throws IOException {
        return https(address, backlog, handler, new DefaultErrorHandler(), MaxRequestSize.DEFAULT, sslContext);
    }

    /**
     * Creates an HTTPS {@link HttpTransport}.
     *
     * @param address      the address to bind to
     * @param backlog      the maximum number of queued incoming connections (0 for system default)
     * @param handler      the {@link Handler} to dispatch requests to
     * @param errorHandler the {@link ErrorHandler} for unhandled exceptions
     * @param sslContext   the {@link SSLContext} used for TLS
     * @return a new HTTPS {@link HttpTransport}
     * @throws IOException if the server cannot be created
     */
    public static HttpTransport https(final InetSocketAddress address,
                                      final int backlog,
                                      final Handler handler,
                                      final ErrorHandler errorHandler,
                                      final SSLContext sslContext) throws IOException {
        return https(address, backlog, handler, errorHandler, MaxRequestSize.DEFAULT, sslContext);
    }

    /**
     * Creates an HTTPS {@link HttpTransport} with a custom {@link MaxRequestSize}.
     *
     * @param address        the address to bind to
     * @param backlog        the maximum number of queued incoming connections (0 for system default)
     * @param handler        the {@link Handler} to dispatch requests to
     * @param errorHandler   the {@link ErrorHandler} for unhandled exceptions
     * @param maxRequestSize the maximum allowed request body size
     * @param sslContext     the {@link SSLContext} used for TLS
     * @return a new HTTPS {@link HttpTransport}
     * @throws IOException if the server cannot be created
     */
    public static HttpTransport https(final InetSocketAddress address,
                                      final int backlog,
                                      final Handler handler,
                                      final ErrorHandler errorHandler,
                                      final MaxRequestSize maxRequestSize,
                                      final SSLContext sslContext) throws IOException {
        return https(address, backlog, handler, errorHandler, maxRequestSize, sslContext, TlsOptions.defaults());
    }

    /**
     * Creates an HTTPS {@link HttpTransport} with TLS hardening options.
     *
     * @param address        the address to bind to
     * @param backlog        the maximum number of queued incoming connections (0 for system default)
     * @param handler        the {@link Handler} to dispatch requests to
     * @param errorHandler   the {@link ErrorHandler} for unhandled exceptions
     * @param maxRequestSize the maximum allowed request body size
     * @param sslContext     the {@link SSLContext} used for TLS
     * @param tlsOptions     the {@link TlsOptions} controlling minimum TLS version and cipher filtering
     * @return a new HTTPS {@link HttpTransport}
     * @throws IOException if the server cannot be created
     */
    public static HttpTransport https(final InetSocketAddress address,
                                      final int backlog,
                                      final Handler handler,
                                      final ErrorHandler errorHandler,
                                      final MaxRequestSize maxRequestSize,
                                      final SSLContext sslContext,
                                      final TlsOptions tlsOptions) throws IOException {
        Objects.requireNonNull(sslContext, "sslContext must not be null");
        Objects.requireNonNull(tlsOptions, "tlsOptions must not be null");

        final var server = HttpsServer.create(address, backlog);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(final HttpsParameters params) {
                final var engine = getSSLContext().createSSLEngine();
                final var enabledProtocols = engine.getEnabledProtocols();
                final var enabledCiphers = engine.getEnabledCipherSuites();

                params.setNeedClientAuth(false);

                if (tlsOptions.minimumVersion() != null) {
                    final var minName = tlsOptions.minimumVersion().protocolName();
                    final var filteredProtocols = filterProtocols(enabledProtocols, minName);
                    final var filteredCiphers = filterCiphers(enabledCiphers, minName);
                    params.setProtocols(filteredProtocols);
                    params.setCipherSuites(filteredCiphers);
                } else {
                    params.setProtocols(enabledProtocols);
                    params.setCipherSuites(enabledCiphers);
                }

                params.setSSLParameters(getSSLContext().getDefaultSSLParameters());
            }
        });

        return new HttpTransport(server, address, handler, errorHandler, maxRequestSize);
    }

    static String[] filterProtocols(final String[] protocols, final String minProtocol) {
        final var order = List.of("TLSv1", "TLSv1.1", "TLSv1.2", "TLSv1.3");
        final int minIndex = order.indexOf(minProtocol);

        return Arrays.stream(protocols)
            .filter(p -> order.indexOf(p) >= minIndex)
            .toArray(String[]::new);
    }

    static String[] filterCiphers(final String[] ciphers, final String minProtocol) {
        if ("TLSv1.3".equals(minProtocol)) {
            return Arrays.stream(ciphers)
                .filter(c -> !c.contains("_CBC_") && !c.startsWith("TLS_RSA_"))
                .toArray(String[]::new);
        }

        return Arrays.stream(ciphers)
            .filter(c -> !c.contains("_NULL_") && !c.contains("_anon_") && !c.contains("EXPORT"))
            .toArray(String[]::new);
    }

    /**
     * Private constructor that accepts a pre-configured {@link HttpServer} (HTTP or HTTPS).
     */
    private HttpTransport(final HttpServer server,
                          final InetSocketAddress address,
                          final Handler handler,
                          final ErrorHandler errorHandler,
                          final MaxRequestSize maxRequestSize) {
        this.address = Objects.requireNonNull(address, "address must not be null");
        this.maxRequestSize = Objects.requireNonNull(maxRequestSize, "maxRequestSize must not be null");
        Objects.requireNonNull(handler, "handler must not be null");
        Objects.requireNonNull(errorHandler, "errorHandler must not be null");

        this.httpServer = server;
        this.httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.httpServer.createContext("/", httpExchange -> {
            final var request = new HttpRequest(httpExchange, this.maxRequestSize);
            final var response = new HttpResponse(httpExchange);
            final var exchange = new SimpleExchange(request, response);

            // Link request and response back to the exchange for path param and body reader/writer resolution
            request.setExchange(exchange);
            response.setExchange(exchange);

            // Expose the underlying HttpExchange for WebSocket upgrade support
            exchange.attribute(Exchange.HTTP_EXCHANGE_ATTRIBUTE, httpExchange);

            RequestContext.run(exchange, () -> {
                try {
                    handler.handle(exchange);
                } catch (final Exception e) {
                    try {
                        errorHandler.handle(exchange, e);
                    } catch (final Exception ignored) {
                        // response may already be committed
                    }
                } finally {
                    httpExchange.close();
                }
            });
        });
    }

    /**
     * Starts this {@link HttpTransport}.
     */
    public void start() {
        httpServer.start();

        LOGGER.info("HttpTransport started on " + address);
    }

    /**
     * Stops this {@link HttpTransport}.
     *
     * @param delay the maximum time in seconds to wait for active exchanges to finish
     */
    public void stop(final int delay) {
        httpServer.stop(delay);

        LOGGER.info("HttpTransport stopped");
    }

    /**
     * Obtains the {@link InetSocketAddress} this transport is bound to.
     *
     * @return the bound address
     */
    public InetSocketAddress address() {
        return httpServer.getAddress();
    }
}
