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
/**
 * Application lifecycle management: startup, shutdown, DI integration, and JVM shutdown hook.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
module build.serve.application {
    requires transitive build.serve.foundation;
    requires build.serve.transport.http;
    requires transitive build.serve.security;
    requires transitive build.spawn.application;
    requires transitive build.codemodel.injection;
    requires transitive jakarta.inject;

    requires build.base.logging;
    requires build.base.telemetry;
    requires build.base.telemetry.foundation;

    exports build.serve.application;
    opens build.serve.application to build.codemodel.injection;
}
