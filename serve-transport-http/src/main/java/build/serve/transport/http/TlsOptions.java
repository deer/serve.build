/*-
 * #%L
 * Serve Transport (HTTP)
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
package build.serve.transport.http;

import java.util.Objects;

/**
 * TLS hardening options for HTTPS transports.
 * <p>
 * Use {@link #defaults()} for no restrictions (JDK defaults), or {@link #minimum(TlsVersion)}
 * to reject connections below a minimum protocol version.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class TlsOptions {

    private final TlsVersion minimumVersion;

    private TlsOptions(final TlsVersion minimumVersion) {
        this.minimumVersion = minimumVersion;
    }

    /**
     * Returns {@link TlsOptions} that apply no restrictions beyond JDK defaults.
     *
     * @return default {@link TlsOptions}
     */
    public static TlsOptions defaults() {
        return new TlsOptions(null);
    }

    /**
     * Returns {@link TlsOptions} that reject connections below the specified minimum TLS version.
     * Cipher suites associated with older versions are also filtered out.
     *
     * @param version the minimum {@link TlsVersion} to accept
     * @return {@link TlsOptions} with the given minimum version
     */
    public static TlsOptions minimum(final TlsVersion version) {
        return new TlsOptions(Objects.requireNonNull(version, "version"));
    }

    /**
     * Returns the minimum TLS version, or {@code null} if no restriction is applied.
     *
     * @return the minimum {@link TlsVersion}, or {@code null}
     */
    public TlsVersion minimumVersion() {
        return minimumVersion;
    }
}
