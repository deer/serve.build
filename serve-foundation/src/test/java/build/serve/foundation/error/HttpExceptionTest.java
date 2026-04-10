package build.serve.foundation.error;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpExceptionTest {

    @Test
    void httpExceptionHasStatusCodeAndMessage() {
        var ex = new HttpException(418, "I'm a teapot");

        assertThat(ex.statusCode()).isEqualTo(418);
        assertThat(ex.getMessage()).isEqualTo("I'm a teapot");
    }

    @Test
    void httpExceptionWithCause() {
        var cause = new RuntimeException("root");
        var ex = new HttpException(500, "fail", cause);

        assertThat(ex.statusCode()).isEqualTo(500);
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void notFoundExceptionDefaultMessage() {
        var ex = new NotFoundException();

        assertThat(ex.statusCode()).isEqualTo(404);
        assertThat(ex.getMessage()).isEqualTo("Not Found");
    }

    @Test
    void notFoundExceptionCustomMessage() {
        var ex = new NotFoundException("User 42 not found");

        assertThat(ex.statusCode()).isEqualTo(404);
        assertThat(ex.getMessage()).isEqualTo("User 42 not found");
    }

    @Test
    void badRequestException() {
        var ex = new BadRequestException("missing field");

        assertThat(ex.statusCode()).isEqualTo(400);
        assertThat(ex.getMessage()).isEqualTo("missing field");
    }

    @Test
    void unauthorizedException() {
        var ex = new UnauthorizedException("invalid token");

        assertThat(ex.statusCode()).isEqualTo(401);
        assertThat(ex.getMessage()).isEqualTo("invalid token");
    }

    @Test
    void forbiddenException() {
        var ex = new ForbiddenException("access denied");

        assertThat(ex.statusCode()).isEqualTo(403);
        assertThat(ex.getMessage()).isEqualTo("access denied");
    }

    @Test
    void conflictException() {
        var ex = new ConflictException("already exists");

        assertThat(ex.statusCode()).isEqualTo(409);
        assertThat(ex.getMessage()).isEqualTo("already exists");
    }

    @Test
    void internalServerException() {
        var ex = new InternalServerException("something broke");

        assertThat(ex.statusCode()).isEqualTo(500);
        assertThat(ex.getMessage()).isEqualTo("something broke");
    }

    @Test
    void internalServerExceptionWithCause() {
        var cause = new RuntimeException("root");
        var ex = new InternalServerException("something broke", cause);

        assertThat(ex.statusCode()).isEqualTo(500);
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
