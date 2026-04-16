package build.serve.foundation.option;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;

import static org.assertj.core.api.Assertions.assertThat;

class TlsConfigTests {

    @Test
    void ofStoresSslContext() throws Exception {
        final var ctx = SSLContext.getDefault();
        final var config = TlsConfig.of(ctx);

        assertThat(config.sslContext()).isSameAs(ctx);
    }

    @Test
    void recordAccessorReturnsSslContext() throws Exception {
        final var ctx = SSLContext.getInstance("TLS");
        ctx.init(null, null, null);
        final var config = new TlsConfig(ctx);

        assertThat(config.sslContext()).isSameAs(ctx);
    }
}
