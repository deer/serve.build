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
/**
 * jdk.httpserver adapter and virtual thread dispatcher for serve-foundation.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
module build.serve.transport.http {
    requires transitive build.serve.foundation;
    requires build.base.logging;
    requires jdk.httpserver;

    exports build.serve.transport.http;
}
