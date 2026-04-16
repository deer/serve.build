/*-
 * #%L
 * Serve JTE
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
package build.serve.jte;

import build.serve.template.StringTemplateOutput;
import build.serve.template.TemplateNotFoundException;
import build.serve.template.TemplateOutput;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JteTemplateEngineTests {

    private static final Path TEMPLATE_DIR = Path.of("src/test/jte");

    @Test
    void developmentModeRendersTemplateWithStringParam() throws Exception {
        var engine = JteTemplateEngine.development(TEMPLATE_DIR);
        var template = engine.template("hello.jte");
        var output = TemplateOutput.capture();

        template.render(output, Map.of("name", "World"));

        assertThat(output.get()).contains("Hello, World!");
    }

    @Test
    void stringTemplateOutputCapturesRenderedOutput() throws Exception {
        var engine = JteTemplateEngine.development(TEMPLATE_DIR);
        var template = engine.template("hello.jte");
        var output = new StringTemplateOutput();

        template.render(output, Map.of("name", "JTE"));

        assertThat(output.get()).contains("Hello, JTE!");
        assertThat(output.toString()).contains("Hello, JTE!");
    }

    @Test
    void developmentModeRendersTemplateWithMultipleParams() throws Exception {
        var engine = JteTemplateEngine.development(TEMPLATE_DIR);
        var template = engine.template("user.jte");
        var output = TemplateOutput.capture();

        template.render(output, Map.of("name", "Alice", "email", "alice@example.com"));

        assertThat(output.get()).contains("Alice").contains("alice@example.com");
    }

    @Test
    void missingTemplateThrowsTemplateNotFoundException() {
        var engine = JteTemplateEngine.development(TEMPLATE_DIR);

        assertThatThrownBy(() -> engine.template("nonexistent.jte"))
            .isInstanceOf(TemplateNotFoundException.class)
            .hasMessageContaining("nonexistent.jte");
    }
}
