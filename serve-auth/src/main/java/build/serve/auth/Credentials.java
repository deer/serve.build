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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Utility for comparing credentials (tokens, API keys, passwords) without leaking timing
 * information.
 * <p>
 * {@link String#equals(Object)} short-circuits on the first mismatched character, so comparing a
 * secret with {@code .equals()} lets an attacker recover it byte-by-byte via timing analysis.
 * {@link AuthStrategy} validators supplied to {@link BearerTokenStrategy}, {@link BasicAuthStrategy},
 * and {@link ApiKeyStrategy} should use {@link #equal(String, String)} instead.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public final class Credentials {

    private Credentials() {
    }

    /**
     * Compares two credential strings in constant time, regardless of where they first differ.
     *
     * @param a the first value (e.g. the client-supplied token)
     * @param b the second value (e.g. the expected secret)
     * @return {@code true} if the values are equal
     */
    public static boolean equal(final String a, final String b) {
        if (a == null || b == null) {
            return a == b;
        }

        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
    }
}
