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
 * Base exception that maps to an HTTP status code.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class HttpException extends RuntimeException {

    /**
     * The HTTP status code.
     */
    private final int statusCode;

    /**
     * Constructs an {@link HttpException}.
     *
     * @param statusCode the HTTP status code
     * @param message    the detail message
     */
    public HttpException(final int statusCode, final String message) {
        super(message);
        this.statusCode = statusCode;
    }

    /**
     * Constructs an {@link HttpException} with a cause.
     *
     * @param statusCode the HTTP status code
     * @param message    the detail message
     * @param cause      the cause
     */
    public HttpException(final int statusCode, final String message, final Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /**
     * Obtains the HTTP status code.
     *
     * @return the status code
     */
    public int statusCode() {
        return statusCode;
    }
}
