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
 * An {@link Option} to define the address on which a server listens.
 *
 * @param value the listen address (e.g., "0.0.0.0" or "localhost")
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public record ListenAddress(String value)
    implements Option {

    /**
     * The default {@link ListenAddress} ("0.0.0.0").
     */
    public static final ListenAddress DEFAULT = new ListenAddress("0.0.0.0");

    /**
     * Creates a {@link ListenAddress} for the specified address.
     *
     * @param address the listen address
     * @return a new {@link ListenAddress}
     */
    public static ListenAddress of(final String address) {
        return new ListenAddress(address);
    }
}
