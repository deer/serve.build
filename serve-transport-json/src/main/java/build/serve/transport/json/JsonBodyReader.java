/*-
 * #%L
 * Serve Transport (JSON)
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
package build.serve.transport.json;

import build.base.json.Json;
import build.base.json.JsonValue;
import build.serve.foundation.Request;

/**
 * Reads and parses JSON request bodies using {@link Json}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonBodyReader {

    /**
     * Reads the request body and parses it as a {@link JsonValue}.
     *
     * @param request the {@link Request}
     * @param type    ignored; always returns {@link JsonValue}
     * @return the parsed {@link JsonValue}
     */
    public JsonValue read(final Request request) {
        return Json.parse(request.bodyAsString());
    }
}
