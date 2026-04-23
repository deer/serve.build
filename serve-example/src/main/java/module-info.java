/*-
 * #%L
 * Serve Example
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
 * Example application demonstrating routing, middleware, WebSocket, GraphQL, and template rendering.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@build.base.template.ProcessTemplates
module build.serve.example {
    requires build.serve.application;
    requires build.serve.transport.json;
    requires build.serve.websocket;
    requires build.serve.htmx;
    requires build.serve.cors;
    requires build.serve.security;
    requires build.serve.compression;
    requires build.serve.logging;
    requires build.serve.health;
    requires build.serve.graphql;
    requires build.serve.session;
    requires build.serve.auth;
    requires build.serve.ratelimit;
    requires build.serve.devtools;
    requires build.serve.form;
    requires build.base.network;
    requires build.base.template;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens build.serve.example to com.fasterxml.jackson.databind;
    opens build.serve.example.api to com.fasterxml.jackson.databind;
    opens build.serve.example.domain to com.fasterxml.jackson.databind, com.graphqljava;
    opens build.serve.example.graphql to com.fasterxml.jackson.databind;
}
