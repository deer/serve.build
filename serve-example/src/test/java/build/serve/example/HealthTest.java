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
package build.serve.example;

import build.serve.foundation.routing.RouterBuilder;
import build.serve.health.HealthCheck;
import build.serve.health.HealthHandler;
import build.serve.testing.TestServer;
import org.junit.jupiter.api.Test;

class HealthTest {

    private static TestServer server() {
        return TestServer.of(RouterBuilder.create()
            .route("/health", HealthHandler.create()
                .liveness("/live")
                .readiness("/ready", HealthCheck.of("tasks", () -> true))
                .build())
            .build());
    }

    @Test
    void shouldReturnLivenessUp() {
        try (final var s = server()) {
            s.get("/health/live")
                .send()
                .assertStatus(200)
                .assertBodyContains("UP");
        }
    }

    @Test
    void shouldReturnReadinessUp() {
        try (final var s = server()) {
            s.get("/health/ready")
                .send()
                .assertStatus(200)
                .assertBodyContains("UP")
                .assertBodyContains("tasks");
        }
    }

    @Test
    void shouldReturnReadinessDownWhenCheckFails() {
        try (final var s = TestServer.of(RouterBuilder.create()
                .route("/health", HealthHandler.create()
                    .readiness("/ready", HealthCheck.of("tasks", () -> false))
                    .build())
                .build())) {
            s.get("/health/ready")
                .send()
                .assertStatus(503)
                .assertBodyContains("DOWN");
        }
    }
}
