/*-
 * #%L
 * Serve Transport (JSON)
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
package build.serve.transport.json;

import build.serve.foundation.Exchange;
import build.serve.foundation.Handler;
import build.serve.foundation.Request;
import build.serve.foundation.Response;
import build.serve.foundation.middleware.Middleware;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A {@link Middleware} that registers JSON body reading and writing capabilities on each
 * {@link Exchange}, enabling parsing of request bodies and serialization of {@link build.base.json.JsonValue}
 * response bodies via {@link Exchange#bodyAs(Class)} and {@link Exchange#sendBody(Object)}.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public class JsonMiddleware
    implements Middleware {

    /**
     * The attribute key used to store the {@link JsonBodyReader} on the exchange.
     */
    public static final String JSON_BODY_READER_ATTRIBUTE = "json.bodyReader";

    /**
     * The attribute key used to store the {@link JsonBodyWriter} on the exchange.
     */
    public static final String JSON_BODY_WRITER_ATTRIBUTE = "json.bodyWriter";

    private final JsonBodyReader bodyReader = new JsonBodyReader();
    private final JsonBodyWriter bodyWriter = new JsonBodyWriter();
    private final Function<Request, Object> readerFunction = bodyReader::read;
    private final BiConsumer<Response, Object> writerConsumer = bodyWriter::write;

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            exchange.attribute(JSON_BODY_READER_ATTRIBUTE, bodyReader);
            exchange.attribute(JSON_BODY_WRITER_ATTRIBUTE, bodyWriter);
            exchange.attribute(Exchange.BODY_READER_ATTRIBUTE, readerFunction);
            exchange.attribute(Exchange.BODY_WRITER_ATTRIBUTE, writerConsumer);

            next.handle(exchange);
        };
    }
}
