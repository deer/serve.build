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
package build.serve.foundation.error;

/**
 * An {@link HttpException} for HTTP 413 Payload Too Large.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public class PayloadTooLargeException extends HttpException {

    /**
     * Constructs a {@link PayloadTooLargeException} with a message.
     *
     * @param message the detail message
     */
    public PayloadTooLargeException(final String message) {
        super(413, message);
    }
}
