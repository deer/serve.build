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

import build.serve.template.Template;
import build.serve.template.TemplateEngine;
import build.serve.template.TemplateNotFoundException;
import gg.jte.ContentType;
import gg.jte.resolve.DirectoryCodeResolver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * A {@link TemplateEngine} implementation backed by the JTE template engine.
 * <p>
 * Supports development mode (hot reload from the filesystem) and production mode
 * (precompiled templates from the classpath).
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class JteTemplateEngine implements TemplateEngine {

    private final gg.jte.TemplateEngine jteEngine;

    private JteTemplateEngine(final gg.jte.TemplateEngine jteEngine) {
        this.jteEngine = Objects.requireNonNull(jteEngine, "jteEngine");
    }

    /**
     * Creates a development-mode {@link JteTemplateEngine} that loads and hot-reloads
     * templates from the given directory.
     *
     * @param templateDirectory the directory containing JTE template source files
     * @return a new {@link JteTemplateEngine} in development mode
     */
    public static JteTemplateEngine development(final Path templateDirectory) {
        Objects.requireNonNull(templateDirectory, "templateDirectory");
        try {
            final var classDir = Files.createTempDirectory("jte-serve-");
            final var resolver = new DirectoryCodeResolver(templateDirectory);
            final var engine = gg.jte.TemplateEngine.create(resolver, classDir, ContentType.Html);
            return new JteTemplateEngine(engine);
        } catch (final IOException ex) {
            throw new UncheckedIOException("Failed to create JTE development engine", ex);
        }
    }

    /**
     * Creates a production-mode {@link JteTemplateEngine} that loads precompiled templates
     * using the classloader of the given base class.
     *
     * @param baseClass the class whose classloader is used to locate precompiled templates
     * @return a new {@link JteTemplateEngine} in production mode
     */
    public static JteTemplateEngine production(final Class<?> baseClass) {
        Objects.requireNonNull(baseClass, "baseClass");
        final var engine = gg.jte.TemplateEngine.createPrecompiled(
            null,
            ContentType.Html,
            baseClass.getClassLoader()
        );
        return new JteTemplateEngine(engine);
    }

    /**
     * Creates a production-mode {@link JteTemplateEngine} that loads precompiled templates
     * from the given class directory.
     *
     * @param precompiledClassDirectory the directory containing precompiled JTE class files
     * @return a new {@link JteTemplateEngine} in production mode
     */
    public static JteTemplateEngine production(final Path precompiledClassDirectory) {
        Objects.requireNonNull(precompiledClassDirectory, "precompiledClassDirectory");
        final var engine = gg.jte.TemplateEngine.createPrecompiled(
            precompiledClassDirectory,
            ContentType.Html
        );
        return new JteTemplateEngine(engine);
    }

    @Override
    public Template template(final String name) {
        Objects.requireNonNull(name, "name");
        if (!jteEngine.hasTemplate(name)) {
            throw new TemplateNotFoundException(name);
        }
        return new JteTemplate(name, jteEngine);
    }
}
