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

/**
 * Thrown when a {@link TemplateEngine} cannot find a template with the requested name.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TemplateNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code TemplateNotFoundException} for the given template name.
     *
     * @param name the name of the template that was not found
     */
    public TemplateNotFoundException(final String name) {
        super("Template not found: " + name);
    }

    /**
     * Constructs a new {@code TemplateNotFoundException} for the given template name and cause.
     *
     * @param name  the name of the template that was not found
     * @param cause the underlying cause
     */
    public TemplateNotFoundException(final String name, final Throwable cause) {
        super("Template not found: " + name, cause);
    }
}
