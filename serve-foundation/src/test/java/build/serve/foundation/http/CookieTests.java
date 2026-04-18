package build.serve.foundation.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CookieTests {

    // --- Cookie.of / basics ---

    @Test
    void ofCreatesCookieWithNameAndValue() {
        var cookie = Cookie.of("session", "abc123");

        assertThat(cookie.name()).isEqualTo("session");
        assertThat(cookie.value()).isEqualTo("abc123");
    }

    @Test
    void simpleSetCookieHeaderContainsOnlyNameValue() {
        var header = Cookie.of("token", "xyz").toSetCookieHeader();

        assertThat(header).isEqualTo("token=xyz");
    }

    // --- Builder / Set-Cookie attributes ---

    @Test
    void builderSetsPath() {
        var header = Cookie.of("id", "1").toBuilder()
            .path("/api")
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; Path=/api");
    }

    @Test
    void builderSetsDomain() {
        var header = Cookie.of("id", "1").toBuilder()
            .domain("example.com")
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; Domain=example.com");
    }

    @Test
    void builderSetsMaxAge() {
        var header = Cookie.of("id", "1").toBuilder()
            .maxAge(Duration.ofHours(1))
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; Max-Age=3600");
    }

    @Test
    void builderSetsSecureFlag() {
        var header = Cookie.of("id", "1").toBuilder()
            .secure(true)
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; Secure");
    }

    @Test
    void builderSetsHttpOnlyFlag() {
        var header = Cookie.of("id", "1").toBuilder()
            .httpOnly(true)
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; HttpOnly");
    }

    @Test
    void builderSetsSameSite() {
        var header = Cookie.of("id", "1").toBuilder()
            .sameSite("Strict")
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo("id=1; SameSite=Strict");
    }

    @Test
    void builderSetsAllAttributes() {
        var header = Cookie.of("session", "tok").toBuilder()
            .path("/")
            .domain("example.com")
            .maxAge(Duration.ofDays(7))
            .secure(true)
            .httpOnly(true)
            .sameSite("Lax")
            .build()
            .toSetCookieHeader();

        assertThat(header).isEqualTo(
            "session=tok; Path=/; Domain=example.com; Max-Age=604800; Secure; HttpOnly; SameSite=Lax");
    }

    @Test
    void secureAndHttpOnlyTogetherRenderBothFlags() {
        var header = Cookie.of("id", "1").toBuilder()
            .secure(true)
            .httpOnly(true)
            .build()
            .toSetCookieHeader();

        assertThat(header).contains("Secure");
        assertThat(header).contains("HttpOnly");
    }

    @Test
    void sameSiteNoneRendersCorrectly() {
        var header = Cookie.of("id", "1").toBuilder()
            .sameSite("None")
            .secure(true)
            .build()
            .toSetCookieHeader();

        assertThat(header).contains("SameSite=None");
        assertThat(header).contains("Secure");
    }

    // --- Cookie parsing (simulated via stub request) ---

    @Test
    void parseSingleCookieFromHeader() {
        var request = new StubRequest("session=abc123");

        var cookies = request.cookies();

        assertThat(cookies).hasSize(1);
        assertThat(cookies.getFirst().name()).isEqualTo("session");
        assertThat(cookies.getFirst().value()).isEqualTo("abc123");
    }

    @Test
    void parseMultipleCookiesFromHeader() {
        var request = new StubRequest("a=1; b=2; c=3");

        var cookies = request.cookies();

        assertThat(cookies).hasSize(3);
        assertThat(cookies.get(0).name()).isEqualTo("a");
        assertThat(cookies.get(0).value()).isEqualTo("1");
        assertThat(cookies.get(1).name()).isEqualTo("b");
        assertThat(cookies.get(1).value()).isEqualTo("2");
        assertThat(cookies.get(2).name()).isEqualTo("c");
        assertThat(cookies.get(2).value()).isEqualTo("3");
    }

    @Test
    void cookieByNameReturnsCorrectCookie() {
        var request = new StubRequest("x=10; y=20");

        assertThat(request.cookie("y"))
            .isPresent()
            .hasValueSatisfying(c -> assertThat(c.value()).isEqualTo("20"));
    }

    @Test
    void cookieByNameReturnsEmptyForMissing() {
        var request = new StubRequest("x=10");

        assertThat(request.cookie("z")).isEmpty();
    }

    @Test
    void noCookieHeaderReturnsEmptyList() {
        var request = new StubRequest(null);

        assertThat(request.cookies()).isEmpty();
    }

    // --- deleteCookie ---

    @Test
    void deleteCookieSetsMaxAgeZero() {
        var header = Cookie.of("session", "").toBuilder()
            .maxAge(Duration.ZERO)
            .build()
            .toSetCookieHeader();

        assertThat(header).contains("Max-Age=0");
    }

    @Test
    void deleteCookieViaDefaultMethodSetsMaxAgeZero() {
        var response = new StubResponse();

        response.deleteCookie("session");

        assertThat(response.setCookieHeader).contains("session=");
        assertThat(response.setCookieHeader).contains("Max-Age=0");
    }

    // --- CRLF injection prevention ---

    @Test
    void shouldRejectCrlfInName() {
        assertThatThrownBy(() -> Cookie.of("bad\r\nname", "value"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name");
    }

    @Test
    void shouldRejectCrlfInValue() {
        assertThatThrownBy(() -> Cookie.of("name", "bad\r\nInjected: header"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("value");
    }

    @Test
    void shouldRejectCrlfInPath() {
        assertThatThrownBy(() -> Cookie.of("n", "v").toBuilder().path("/\r\nX-Inject: bad").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("path");
    }

    @Test
    void shouldRejectCrlfInDomain() {
        assertThatThrownBy(() -> Cookie.of("n", "v").toBuilder().domain("evil\r\ndomain").build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("domain");
    }

    // --- Stubs ---

    /**
     * A stub {@link build.serve.foundation.Request} that parses cookies from a given raw cookie header value.
     */
    private static final class StubRequest implements build.serve.foundation.Request {

        private final String rawCookieHeader;

        StubRequest(final String rawCookieHeader) {
            this.rawCookieHeader = rawCookieHeader;
        }

        @Override
        public List<Cookie> cookies() {
            if (rawCookieHeader == null || rawCookieHeader.isBlank()) {
                return List.of();
            }

            final var result = new java.util.ArrayList<Cookie>();

            for (final var pair : rawCookieHeader.split(";")) {
                final var trimmed = pair.trim();
                final var eq = trimmed.indexOf('=');

                if (eq > 0) {
                    final var name = trimmed.substring(0, eq).trim();
                    final var value = trimmed.substring(eq + 1).trim();

                    result.add(Cookie.of(name, value));
                }
            }

            return List.copyOf(result);
        }

        @Override
        public String method() {
            return "GET";
        }

        @Override
        public java.net.URI uri() {
            return java.net.URI.create("/");
        }

        @Override
        public String path() {
            return "/";
        }

        @Override
        public Optional<String> pathParam(final String name) {
            return Optional.empty();
        }

        @Override
        public List<String> queryParams(final String name) {
            return List.of();
        }

        @Override
        public Optional<String> queryParam(final String name) {
            return Optional.empty();
        }

        @Override
        public java.util.Map<String, List<String>> headers() {
            return java.util.Map.of();
        }

        @Override
        public Optional<String> header(final String name) {
            return Optional.empty();
        }

        @Override
        public java.io.InputStream bodyAsStream() {
            return java.io.InputStream.nullInputStream();
        }

        @Override
        public String bodyAsString() {
            return "";
        }

        @Override
        public <T> T body(final Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A stub {@link build.serve.foundation.Response} that captures the last Set-Cookie header value.
     */
    private static final class StubResponse implements build.serve.foundation.Response {

        String setCookieHeader;

        @Override
        public void cookie(final Cookie cookie) {
            this.setCookieHeader = cookie.toSetCookieHeader();
        }

        @Override
        public build.serve.foundation.Response status(final int s) {
            return this;
        }

        @Override
        public int status() {
            return 200;
        }

        @Override
        public build.serve.foundation.Response header(final String n, final String v) {
            return this;
        }

        @Override
        public void send(final String b) {
        }

        @Override
        public void send(final byte[] b) {
        }

        @Override
        public void json(final Object b) {
        }

        @Override
        public java.io.OutputStream bodyAsStream() {
            throw new UnsupportedOperationException();
        }
    }
}
