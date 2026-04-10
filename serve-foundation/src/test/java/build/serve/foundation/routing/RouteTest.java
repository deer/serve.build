package build.serve.foundation.routing;

import build.serve.foundation.Handler;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteTest {

    private static final Handler NOOP = exchange -> {
    };

    @Test
    void matchesExactPath() {
        var route = new Route("GET", "/hello", NOOP);

        assertThat(route.match("GET", "/hello")).isPresent();
    }

    @Test
    void matchesPathWithSingleParam() {
        var route = new Route("GET", "/users/{id}", NOOP);

        var match = route.match("GET", "/users/42");

        assertThat(match).isPresent();
        assertThat(match.get().params()).containsEntry("id", "42");
    }

    @Test
    void matchesPathWithMultipleParams() {
        var route = new Route("GET", "/users/{userId}/posts/{postId}", NOOP);

        var match = route.match("GET", "/users/7/posts/99");

        assertThat(match).isPresent();
        assertThat(match.get().params())
            .containsEntry("userId", "7")
            .containsEntry("postId", "99");
    }

    @Test
    void rejectsWrongHttpMethod() {
        var route = new Route("GET", "/hello", NOOP);

        assertThat(route.match("POST", "/hello")).isEmpty();
    }

    @Test
    void nullMethodMatchesAnyMethod() {
        var route = new Route(null, "/hello", NOOP);

        assertThat(route.match("GET", "/hello")).isPresent();
        assertThat(route.match("POST", "/hello")).isPresent();
        assertThat(route.match("DELETE", "/hello")).isPresent();
    }

    @Test
    void rejectsNonMatchingPath() {
        var route = new Route("GET", "/hello", NOOP);

        assertThat(route.match("GET", "/goodbye")).isEmpty();
    }

    @Test
    void handlesTrailingSlashesCorrectly() {
        var route = new Route("GET", "/hello", NOOP);

        // Route pattern "/hello" compiles to ^/hello$ — trailing slash won't match
        assertThat(route.match("GET", "/hello/")).isEmpty();
    }

    @Test
    void shouldPrefixSlashMatchAllSubPaths() {
        var route = new Route(null, "/", NOOP, true);

        assertThat(route.match("GET", "/")).isPresent();
        assertThat(route.match("GET", "/tasks")).isPresent();
        assertThat(route.match("GET", "/tasks/42/toggle")).isPresent();
    }
}
