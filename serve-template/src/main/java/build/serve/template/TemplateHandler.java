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

import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * A {@link Handler} that renders a named template and sends the result as the HTTP response.
 * <p>
 * The response content type is set to {@code text/html; charset=utf-8}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class TemplateHandler implements Handler {

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEXT_HTML = "text/html; charset=utf-8";

    private final TemplateEngine engine;
    private final String name;
    private final Function<Exchange, Map<String, Object>> modelExtractor;

    private TemplateHandler(final TemplateEngine engine,
                            final String name,
                            final Function<Exchange, Map<String, Object>> modelExtractor) {
        this.engine = Objects.requireNonNull(engine, "engine");
        this.name = Objects.requireNonNull(name, "name");
        this.modelExtractor = Objects.requireNonNull(modelExtractor, "modelExtractor");
    }

    /**
     * Creates a {@link TemplateHandler} that renders the named template with a static model.
     *
     * @param engine the {@link TemplateEngine} to resolve the template
     * @param name   the template name
     * @param params the static model parameters
     * @return a new {@link TemplateHandler}
     */
    public static TemplateHandler render(final TemplateEngine engine,
                                         final String name,
                                         final Map<String, Object> params) {
        Objects.requireNonNull(params, "params");
        return new TemplateHandler(engine, name, exchange -> params);
    }

    /**
     * Creates a {@link TemplateHandler} that renders the named template with a dynamic model
     * derived from the {@link Exchange}.
     *
     * @param engine         the {@link TemplateEngine} to resolve the template
     * @param name           the template name
     * @param modelExtractor a function that produces a model from the current exchange
     * @return a new {@link TemplateHandler}
     */
    public static TemplateHandler render(final TemplateEngine engine,
                                         final String name,
                                         final Function<Exchange, Map<String, Object>> modelExtractor) {
        return new TemplateHandler(engine, name, modelExtractor);
    }

    @Override
    public void handle(final Exchange exchange) throws Exception {
        final var output = TemplateOutput.capture();
        engine.template(name).render(output, modelExtractor.apply(exchange));
        exchange.response().header(CONTENT_TYPE, TEXT_HTML).send(output.get());
    }
}
