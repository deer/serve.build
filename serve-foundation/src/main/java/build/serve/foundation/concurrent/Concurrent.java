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
package build.serve.foundation.concurrent;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.StructuredTaskScope;

/**
 * Utilities for structured concurrent fan-out using {@link StructuredTaskScope}.
 * <p>
 * {@link ScopedValue}s from the calling thread are automatically inherited by forked subtasks.
 *
 * @author reed.vonredwitz
 * @since Mar-2026
 */
public final class Concurrent {

    private Concurrent() {
        // utility class
    }

    /**
     * Forks two tasks concurrently and returns both results.
     *
     * @param taskA the first task
     * @param taskB the second task
     * @param <A>   the first result type
     * @param <B>   the second result type
     * @return a {@link Pair} containing both results
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws RuntimeException     if any subtask fails
     */
    public static <A, B> Pair<A, B> fork(final Callable<A> taskA,
                                         final Callable<B> taskB) throws InterruptedException {
        try (var scope = StructuredTaskScope.open()) {
            final var a = scope.fork(taskA);
            final var b = scope.fork(taskB);
            scope.join();
            return new Pair<>(a.get(), b.get());
        }
    }

    /**
     * Forks three tasks concurrently and returns all three results.
     *
     * @param taskA the first task
     * @param taskB the second task
     * @param taskC the third task
     * @param <A>   the first result type
     * @param <B>   the second result type
     * @param <C>   the third result type
     * @return a {@link Triple} containing all three results
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws RuntimeException     if any subtask fails
     */
    public static <A, B, C> Triple<A, B, C> fork(final Callable<A> taskA,
                                                 final Callable<B> taskB,
                                                 final Callable<C> taskC) throws InterruptedException {
        try (var scope = StructuredTaskScope.open()) {
            final var a = scope.fork(taskA);
            final var b = scope.fork(taskB);
            final var c = scope.fork(taskC);
            scope.join();
            return new Triple<>(a.get(), b.get(), c.get());
        }
    }

    /**
     * Forks N tasks of the same type concurrently and returns all results as a list.
     *
     * @param tasks the tasks to fork
     * @param <T>   the result type
     * @return a list of results in the same order as the input tasks
     * @throws InterruptedException if the current thread is interrupted while waiting
     * @throws RuntimeException     if any subtask fails
     */
    public static <T> List<T> forkAll(final List<Callable<T>> tasks) throws InterruptedException {
        try (var scope = StructuredTaskScope.open()) {
            final var subtasks = tasks.stream()
                .map(scope::fork)
                .toList();
            scope.join();
            return subtasks.stream()
                .map(StructuredTaskScope.Subtask::get)
                .toList();
        }
    }

    /**
     * A pair of two values.
     *
     * @param first  the first value
     * @param second the second value
     * @param <A>    the first type
     * @param <B>    the second type
     */
    public record Pair<A, B>(A first, B second) {
    }

    /**
     * A triple of three values.
     *
     * @param first  the first value
     * @param second the second value
     * @param third  the third value
     * @param <A>    the first type
     * @param <B>    the second type
     * @param <C>    the third type
     */
    public record Triple<A, B, C>(A first, B second, C third) {
    }
}
