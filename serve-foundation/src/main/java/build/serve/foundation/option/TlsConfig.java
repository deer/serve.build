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
package build.serve.foundation.option;

import build.base.configuration.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

/**
 * An {@link Option} that carries a configured {@link SSLContext} for TLS/HTTPS support.
 *
 * @param sslContext the configured {@link SSLContext}
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record TlsConfig(SSLContext sslContext)
    implements Option {

    /**
     * Creates a {@link TlsConfig} by loading a PKCS12 keystore from a file.
     *
     * @param keyStorePath the path to the PKCS12 keystore file
     * @param password     the keystore and key password
     * @return a new {@link TlsConfig}
     * @throws Exception if the keystore cannot be loaded or the SSL context cannot be initialised
     */
    public static TlsConfig fromKeyStore(final Path keyStorePath,
                                         final char[] password) throws Exception {
        final var ks = KeyStore.getInstance("PKCS12");
        try (var is = Files.newInputStream(keyStorePath)) {
            ks.load(is, password);
        }
        final var kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(ks, password);
        final var ctx = SSLContext.getInstance("TLS");
        ctx.init(kmf.getKeyManagers(), null, null);
        return new TlsConfig(ctx);
    }

    /**
     * Creates a {@link TlsConfig} from an existing {@link SSLContext}.
     *
     * @param sslContext the {@link SSLContext} to use
     * @return a new {@link TlsConfig}
     */
    public static TlsConfig of(final SSLContext sslContext) {
        return new TlsConfig(sslContext);
    }
}
