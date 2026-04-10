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
import build.serve.template.TemplateNotFoundException;
import build.serve.template.TemplateOutput;

import java.util.Map;
import java.util.Objects;

/**
 * A {@link Template} backed by a JTE template engine.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class JteTemplate implements Template {

    private final String name;
    private final gg.jte.TemplateEngine jteEngine;

    JteTemplate(final String name, final gg.jte.TemplateEngine jteEngine) {
        this.name = Objects.requireNonNull(name, "name");
        this.jteEngine = Objects.requireNonNull(jteEngine, "jteEngine");
    }

    @Override
    public void render(final TemplateOutput output, final Map<String, Object> params) throws Exception {
        try {
            jteEngine.render(name, params, new JteOutputAdapter(output));
        } catch (final gg.jte.TemplateNotFoundException ex) {
            throw new TemplateNotFoundException(name, ex);
        }
    }
}
