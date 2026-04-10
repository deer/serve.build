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
 * Represents a compiled or resolved template that can render content to a {@link TemplateOutput}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
@FunctionalInterface
public interface Template {

    /**
     * Renders this template to the specified output with the given model parameters.
     *
     * @param output the {@link TemplateOutput} to write rendered content to
     * @param params the model parameters to pass to the template
     * @throws Exception if rendering fails
     */
    void render(TemplateOutput output, Map<String, Object> params) throws Exception;
}
