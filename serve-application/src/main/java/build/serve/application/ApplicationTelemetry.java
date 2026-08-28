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

import build.base.telemetry.TelemetryRecorder;
import build.base.telemetry.foundation.PrintStreamTelemetryRecorder;

import java.net.URI;

/**
 * Shared telemetry defaults for the {@code build.serve.application} module.
 *
 * @author reed.vonredwitz
 * @since Aug-2026
 */
final class ApplicationTelemetry {

    /**
     * The {@link TelemetryRecorder} used when a caller does not supply one — records to
     * {@code System.out}/{@code System.err}.
     */
    static final TelemetryRecorder DEFAULT_RECORDER =
        PrintStreamTelemetryRecorder.of(URI.create("serve://application"), System.out, System.err);

    /**
     * Prevent instantiation.
     */
    private ApplicationTelemetry() {
    }
}
