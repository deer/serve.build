/*-
 * #%L
 * Serve Application
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
package build.serve.application;

import build.base.configuration.Configuration;
import build.base.network.option.Port;
import build.base.telemetry.TelemetryRecorder;
import build.codemodel.dependency.injection.ConfigurationResolver;
import build.codemodel.dependency.injection.Context;
import build.codemodel.dependency.injection.InjectionFramework;
import build.serve.foundation.option.ListenAddress;

import java.io.IOException;
import java.util.Objects;

/**
 * A simple launcher for {@link ServerApplication} instances.
 * <p>
 * This provides a standalone entry point for launching a server without the full spawn.build platform.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class Launcher {

    /**
     * Prevent instantiation.
     */
    private Launcher() {
    }

    /**
     * Launches a {@link ServerApplication.Implementation} with the specified options.
     *
     * @param server     the {@link ServerApplication.Implementation} to launch
     * @param listenPort the {@link Port} to bind to
     * @return the started server
     */
    public static ServerApplication.Implementation launch(final ServerApplication.Implementation server,
                                                          final Port listenPort) {
        return launch(server, ListenAddress.DEFAULT, listenPort);
    }

    /**
     * Launches a {@link ServerApplication.Implementation} with the specified options.
     *
     * @param server            the {@link ServerApplication.Implementation} to launch
     * @param listenPort        the {@link Port} to bind to
     * @param telemetryRecorder the {@link TelemetryRecorder} to bind into the injection context and record
     *                          server lifecycle events with
     * @return the started server
     */
    public static ServerApplication.Implementation launch(final ServerApplication.Implementation server,
                                                          final Port listenPort,
                                                          final TelemetryRecorder telemetryRecorder) {
        return launch(server, ListenAddress.DEFAULT, listenPort, telemetryRecorder);
    }

    /**
     * Launches a {@link ServerApplication.Implementation} with the specified options.
     *
     * @param server        the {@link ServerApplication.Implementation} to launch
     * @param listenAddress the {@link ListenAddress} to bind to
     * @param listenPort    the {@link Port} to bind to
     * @return the started server
     */
    public static ServerApplication.Implementation launch(final ServerApplication.Implementation server,
                                                          final ListenAddress listenAddress,
                                                          final Port listenPort) {
        return launch(server, listenAddress, listenPort, ApplicationTelemetry.DEFAULT_RECORDER);
    }

    /**
     * Launches a {@link ServerApplication.Implementation} with the specified options.
     *
     * @param server            the {@link ServerApplication.Implementation} to launch
     * @param listenAddress     the {@link ListenAddress} to bind to
     * @param listenPort        the {@link Port} to bind to
     * @param telemetryRecorder the {@link TelemetryRecorder} to bind into the injection context and record
     *                          server lifecycle events with
     * @return the started server
     */
    public static ServerApplication.Implementation launch(final ServerApplication.Implementation server,
                                                          final ListenAddress listenAddress,
                                                          final Port listenPort,
                                                          final TelemetryRecorder telemetryRecorder) {
        Objects.requireNonNull(telemetryRecorder, "telemetryRecorder");

        try {
            // Build configuration from the provided options
            final var configuration = Configuration.of(listenAddress, listenPort);

            // Create the injection framework and context
            final var framework = InjectionFramework.create();
            final var context = framework.newContext();

            // Bind standard objects into the context
            context.bind(Context.class).to(context);
            context.bind(InjectionFramework.class).to(framework);
            context.bind(Configuration.class).to(configuration);
            context.bind(ServerApplication.class).to(server);

            // Bind the TelemetryRecorder and use it for server lifecycle events
            context.bind(TelemetryRecorder.class).to(telemetryRecorder);
            server.recorder(telemetryRecorder);

            // Add the ConfigurationResolver so Options are injectable
            context.addResolver(ConfigurationResolver.of(configuration));

            // Set the context on the server so subclasses can access it
            server.context(context);

            // Inject @Inject fields on the server implementation
            context.inject(server);

            server.start(listenAddress, listenPort);

            return server;
        } catch (final IOException e) {
            telemetryRecorder.fatal(e, "Failed to launch ServerApplication");

            throw new RuntimeException("Failed to launch ServerApplication", e);
        }
    }
}
