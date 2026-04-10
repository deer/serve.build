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

import build.serve.template.TemplateOutput;

/**
 * Bridges JTE's {@link gg.jte.TemplateOutput} to our {@link TemplateOutput} interface.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
final class JteOutputAdapter implements gg.jte.TemplateOutput {

    private final TemplateOutput output;

    JteOutputAdapter(final TemplateOutput output) {
        this.output = output;
    }

    @Override
    public void writeContent(final String value) {
        output.writeContent(value);
    }

    @Override
    public void writeContent(final String value, final int offset, final int length) {
        output.writeContent(value.substring(offset, offset + length));
    }
}
