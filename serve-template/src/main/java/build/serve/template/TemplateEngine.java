/*-
 * #%L
 * Serve Template
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
package build.serve.template;

import java.util.Map;

/**
 * A factory for resolving and obtaining {@link Template} instances by name.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@FunctionalInterface
public interface TemplateEngine {

    /**
     * Obtains a {@link Template} by name.
     *
     * @param name the template name (e.g., {@code "index.jte"})
     * @return the resolved {@link Template}
     * @throws TemplateNotFoundException if no template exists with the given name
     */
    Template template(String name);

    /**
     * Renders the named template to the given output using the provided model parameters.
     * <p>
     * Convenience shorthand for {@code engine.template(name).render(output, params)}.
     *
     * @param name   the template name
     * @param output the {@link TemplateOutput} to write rendered content to
     * @param params the model parameters
     * @throws Exception if rendering fails
     */
    default void render(final String name, final TemplateOutput output, final Map<String, Object> params)
        throws Exception {
        template(name).render(output, params);
    }
}
