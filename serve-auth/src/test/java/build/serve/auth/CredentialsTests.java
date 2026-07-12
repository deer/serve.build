/*-
 * #%L
 * Serve Auth
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
package build.serve.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CredentialsTests {

    @Test
    void shouldReturnTrueForEqualValues() {
        assertThat(Credentials.equal("secret-token", "secret-token")).isTrue();
    }

    @Test
    void shouldReturnFalseForDifferentValues() {
        assertThat(Credentials.equal("secret-token", "wrong-token")).isFalse();
    }

    @Test
    void shouldReturnFalseForDifferentLengths() {
        assertThat(Credentials.equal("short", "a-much-longer-value")).isFalse();
    }

    @Test
    void shouldTreatBothNullAsEqual() {
        assertThat(Credentials.equal(null, null)).isTrue();
    }

    @Test
    void shouldTreatOneNullAsNotEqual() {
        assertThat(Credentials.equal(null, "value")).isFalse();
        assertThat(Credentials.equal("value", null)).isFalse();
    }
}
