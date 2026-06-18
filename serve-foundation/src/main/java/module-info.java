/*-
 * #%L
 * Serve Foundation
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
 * Core abstractions — Exchange, Request, Response, Handler, Middleware, routing, and error handling.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
module build.serve.foundation {
    requires transitive build.base.json;
    requires transitive build.base.configuration;
    requires transitive build.base.telemetry;
    requires java.logging;

    exports build.serve.foundation;
    exports build.serve.foundation.http;
    exports build.serve.foundation.routing;
    exports build.serve.foundation.middleware;
    exports build.serve.foundation.option;
    exports build.serve.foundation.error;
    exports build.serve.foundation.context;
    exports build.serve.foundation.concurrent;
    exports build.serve.foundation.util;
    exports build.serve.foundation.validation;
}
