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
package build.serve.foundation.validation;

import build.serve.foundation.error.BadRequestException;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Utility for declarative request validation that throws {@link BadRequestException} (400) on
 * failure. Each method returns the validated value, enabling inline use:
 * <pre>{@code
 * var title  = Validate.notBlank("title", body.title());
 * var id     = Validate.present("id", exchange.pathParam("id"));
 * var count  = Validate.that("count", body.count(), n -> n > 0, "must be positive");
 * }</pre>
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class Validate {

    private Validate() {
    }

    /**
     * Asserts that {@code value} is not {@code null}.
     *
     * @param field the field name used in the error message
     * @param value the value to check
     * @param <T>   the value type
     * @return {@code value} if validation passes
     * @throws BadRequestException if {@code value} is null
     */
    public static <T> T notNull(final String field,
                                final T value) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        return value;
    }

    /**
     * Asserts that {@code value} is not {@code null} and not blank.
     *
     * @param field the field name used in the error message
     * @param value the string to check
     * @return {@code value} if validation passes
     * @throws BadRequestException if {@code value} is null or blank
     */
    public static String notBlank(final String field,
                                  final String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(field + " must not be blank");
        }
        return value;
    }

    /**
     * Asserts that the {@link Optional} is present and returns its value.
     *
     * @param field the field name used in the error message
     * @param value the optional to check
     * @param <T>   the value type
     * @return the unwrapped value if present
     * @throws BadRequestException if {@code value} is empty
     */
    public static <T> T present(final String field,
                                final Optional<T> value) {
        return value.orElseThrow(() -> new BadRequestException(field + " is required"));
    }

    /**
     * Asserts that {@code value} satisfies the given {@code predicate}.
     *
     * @param field     the field name used in the error message
     * @param value     the value to check
     * @param predicate the condition that must hold
     * @param message   the error message suffix appended after the field name
     * @param <T>       the value type
     * @return {@code value} if validation passes
     * @throws BadRequestException if the predicate returns {@code false}
     */
    public static <T> T that(final String field, final T value,
                             final Predicate<T> predicate,
                             final String message) {
        if (!predicate.test(value)) {
            throw new BadRequestException(field + ": " + message);
        }
        return value;
    }
}
