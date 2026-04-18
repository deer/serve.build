/*-
 * #%L
 * Serve Logging
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
package build.serve.logging;

/**
 * Controls the output format used by {@link RequestLoggingMiddleware}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public enum LogFormat {

    /**
     * Human-readable plain-text: {@code GET /api -> 200 (42ms)}.
     */
    TEXT,

    /**
     * Structured JSON parseable by Stackdriver, CloudWatch, and ELK:
     * {@code {"method":"GET","path":"/api","status":200,"duration_ms":42,"request_id":"..."}}
     */
    JSON
}
