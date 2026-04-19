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
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidateTests {

    // --- notNull ---

    @Test
    void shouldReturnValueWhenNotNull() {
        assertThat(Validate.notNull("field", "value")).isEqualTo("value");
    }

    @Test
    void shouldThrow400WhenNull() {
        assertThatThrownBy(() -> Validate.notNull("field", null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("field");
    }

    // --- notBlank ---

    @Test
    void shouldReturnValueWhenNotBlank() {
        assertThat(Validate.notBlank("title", "hello")).isEqualTo("hello");
    }

    @Test
    void shouldThrow400WhenBlank() {
        assertThatThrownBy(() -> Validate.notBlank("title", "   "))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("title");
    }

    @Test
    void shouldThrow400WhenNullString() {
        assertThatThrownBy(() -> Validate.notBlank("title", null))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("title");
    }

    @Test
    void shouldThrow400WhenEmptyString() {
        assertThatThrownBy(() -> Validate.notBlank("title", ""))
            .isInstanceOf(BadRequestException.class);
    }

    // --- present ---

    @Test
    void shouldUnwrapPresentOptional() {
        assertThat(Validate.present("id", Optional.of("42"))).isEqualTo("42");
    }

    @Test
    void shouldThrow400WhenOptionalEmpty() {
        assertThatThrownBy(() -> Validate.present("id", Optional.empty()))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("id");
    }

    // --- that ---

    @Test
    void shouldReturnValueWhenPredicatePasses() {
        assertThat(Validate.that("count", 5, n -> n > 0, "must be positive")).isEqualTo(5);
    }

    @Test
    void shouldThrow400WhenPredicateFails() {
        assertThatThrownBy(() -> Validate.that("count", -1, n -> n > 0, "must be positive"))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("count")
            .hasMessageContaining("must be positive");
    }
}
