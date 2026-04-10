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

import build.serve.foundation.Exchange;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A default {@link ErrorHandler} that returns plain text error responses.
 * <p>
 * If the error is an {@link HttpException}, its status code and message are used.
 * Otherwise, a generic 500 response is returned and the error is logged.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class DefaultErrorHandler implements ErrorHandler {

    /**
     * The {@link Logger} for this class.
     */
    private static final Logger LOGGER = Logger.getLogger(DefaultErrorHandler.class.getName());

    @Override
    public void handle(final Exchange exchange, final Throwable error) {
        final int statusCode;
        final String message;

        if (error instanceof HttpException httpException) {
            statusCode = httpException.statusCode();
            message = httpException.getMessage();
        } else {
            statusCode = 500;
            message = "Internal Server Error";

            LOGGER.log(Level.SEVERE, "Unhandled exception", error);
        }

        exchange.response()
            .status(statusCode)
            .header("Content-Type", "text/plain")
            .send(message);
    }
}
