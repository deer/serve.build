/*-
 * #%L
 * Serve LSP
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
package build.serve.lsp.types;

/**
 * The value of a {@code $/progress} notification reporting work-done progress, per the
 * {@code WorkDoneProgressBegin}/{@code WorkDoneProgressReport}/{@code WorkDoneProgressEnd} spec
 * shapes. Sent via {@code LspContext.progress(token, value)} against a token either supplied by
 * the client (a request's {@code workDoneToken}) or created by the server via
 * {@code LspContext.createWorkDoneProgress()}.
 *
 * @author reed.vonredwitz
 * @since Jul-2026
 */
public sealed interface WorkDoneProgress extends LspType
    permits WorkDoneProgress.Begin, WorkDoneProgress.Report, WorkDoneProgress.End {

    /**
     * The first {@code $/progress} notification for a token, opening the reported span of work.
     *
     * @param title       a short title describing the operation
     * @param cancellable whether the client may offer a UI affordance to cancel, or {@code null} to omit
     * @param message     an optional human-readable status message
     * @param percentage  an optional 0-100 completion percentage
     */
    record Begin(String title, Boolean cancellable, String message, Integer percentage) implements WorkDoneProgress {

        /**
         * Creates a begin report with only a title.
         *
         * @param title a short title describing the operation
         * @return the begin report
         */
        public static Begin of(final String title) {
            return new Begin(title, null, null, null);
        }
    }

    /**
     * An intermediate {@code $/progress} notification updating an already-begun span of work.
     *
     * @param cancellable whether the client may offer a UI affordance to cancel, or {@code null} to omit
     * @param message     an optional human-readable status message
     * @param percentage  an optional 0-100 completion percentage
     */
    record Report(Boolean cancellable, String message, Integer percentage) implements WorkDoneProgress {

        /**
         * Creates a report with only a status message.
         *
         * @param message a human-readable status message
         * @return the report
         */
        public static Report of(final String message) {
            return new Report(null, message, null);
        }

        /**
         * Creates a report with only a completion percentage.
         *
         * @param percentage a 0-100 completion percentage
         * @return the report
         */
        public static Report of(final int percentage) {
            return new Report(null, null, percentage);
        }
    }

    /**
     * The final {@code $/progress} notification for a token, closing the reported span of work.
     *
     * @param message an optional human-readable closing status message
     */
    record End(String message) implements WorkDoneProgress {

        /**
         * Creates an end report with no closing message.
         *
         * @return the end report
         */
        public static End of() {
            return new End(null);
        }
    }
}
