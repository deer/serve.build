/*-
 * #%L
 * Serve Foundation
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
package build.serve.foundation.option;

import build.base.configuration.Option;

/**
 * An {@link Option} to define the maximum request body size in bytes.
 *
 * @param bytes the maximum request body size in bytes
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record MaxRequestSize(long bytes)
    implements Option {

    /**
     * The default {@link MaxRequestSize} (10 MB).
     */
    public static final MaxRequestSize DEFAULT = new MaxRequestSize(10 * 1024 * 1024);

    /**
     * Creates a {@link MaxRequestSize} for the specified number of bytes.
     *
     * @param bytes the maximum size in bytes
     * @return a new {@link MaxRequestSize}
     */
    public static MaxRequestSize of(final long bytes) {
        return new MaxRequestSize(bytes);
    }
}
