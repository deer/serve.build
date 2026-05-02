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

import build.base.json.JsonObject;
import build.serve.foundation.Exchange;
import build.serve.foundation.error.DefaultErrorHandler;
import build.serve.foundation.error.ErrorHandler;
import build.serve.foundation.error.HttpException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An {@link ErrorHandler} that returns JSON error responses.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonErrorHandler implements ErrorHandler {

    private static final Logger LOGGER = Logger.getLogger(JsonErrorHandler.class.getName());

    private final ErrorHandler fallback = new DefaultErrorHandler();

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

        final var errorName = statusText(statusCode);

        try {
            final var json = JsonObject.builder()
                .put("status", statusCode)
                .put("error", errorName)
                .put("message", message)
                .build()
                .toJsonString();

            exchange.response()
                .status(statusCode)
                .header("Content-Type", "application/json")
                .send(json);
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Failed to serialize JSON error response", e);
            fallback.handle(exchange, error);
        }
    }

    private static String statusText(final int statusCode) {
        return switch (statusCode) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 500 -> "Internal Server Error";
            default -> "Error";
        };
    }
}
