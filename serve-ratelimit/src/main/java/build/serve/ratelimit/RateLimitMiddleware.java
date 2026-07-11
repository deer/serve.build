/*-
 * #%L
 * Serve Rate Limit
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
package build.serve.ratelimit;

import build.serve.foundation.Handler;
import build.serve.foundation.Request;
import build.serve.foundation.middleware.Middleware;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A {@link Middleware} that enforces per-key token-bucket rate limiting.
 * <p>
 * Requests within the configured limit pass through; requests exceeding it receive a 429 response
 * with {@code Retry-After}, {@code X-RateLimit-Limit}, and {@code X-RateLimit-Remaining} headers.
 * <p>
 * The default rate limit key is the remote TCP peer address — not spoofable by a client-supplied
 * header. Use {@link Builder#keyExtractor(Function)} to rate limit by user ID, API key, or (in a
 * trusted reverse-proxy deployment) {@code X-Forwarded-For}.
 *
 * @author reed.vonredwitz
 * @since Apr-2026
 */
public final class RateLimitMiddleware implements Middleware {

    private final int capacity;
    private final Duration per;
    private final Function<Request, String> keyExtractor;
    private final Clock clock;
    private final int maxBuckets;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private RateLimitMiddleware(final Builder builder) {
        this.capacity = builder.limit;
        this.per = builder.per;
        this.keyExtractor = builder.keyExtractor != null
            ? builder.keyExtractor
            : req -> req.remoteAddress()
                     .map(a -> a.getAddress().getHostAddress())
                     .orElse("unknown");
        this.clock = builder.clock != null ? builder.clock : Clock.systemUTC();
        this.maxBuckets = builder.maxBuckets;
    }

    /**
     * Creates a new {@link Builder} for configuring a {@link RateLimitMiddleware}.
     *
     * @return a new {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Handler apply(final Handler next) {
        return exchange -> {
            final var key = keyExtractor.apply(exchange.request());
            if (!buckets.containsKey(key) && buckets.size() >= maxBuckets) {
                evictLeastRecentlyUsed();
            }
            final var bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, clock.instant()));
            final var result = bucket.consume(clock.instant(), capacity, per);

            exchange.response().header("X-RateLimit-Limit", String.valueOf(capacity));

            if (result.allowed()) {
                exchange.response().header("X-RateLimit-Remaining", String.valueOf(result.remaining()));
                next.handle(exchange);
            } else {
                exchange.response()
                    .header("X-RateLimit-Remaining", "0")
                    .header("Retry-After", String.valueOf(result.retryAfterSeconds()))
                    .status(429)
                    .send("");
            }
        };
    }

    /**
     * Evicts the least-recently-used bucket to make room for a new key, so that {@code maxBuckets}
     * bounds memory without ever permanently locking out legitimate clients once the cap is hit.
     */
    private void evictLeastRecentlyUsed() {
        String oldestKey = null;
        Instant oldest = null;
        for (final var entry : buckets.entrySet()) {
            final var lastAccessedAt = entry.getValue().lastAccessedAt();
            if (oldest == null || lastAccessedAt.isBefore(oldest)) {
                oldest = lastAccessedAt;
                oldestKey = entry.getKey();
            }
        }
        if (oldestKey != null) {
            buckets.remove(oldestKey);
        }
    }

    private record ConsumeResult(boolean allowed, int remaining, long retryAfterSeconds) {
    }

    private static final class Bucket {

        private double tokens;
        private Instant lastRefillAt;

        Bucket(final int capacity, final Instant now) {
            this.tokens = capacity;
            this.lastRefillAt = now;
        }

        synchronized ConsumeResult consume(final Instant now, final int capacity, final Duration per) {
            final var elapsed = Duration.between(lastRefillAt, now);
            if (!elapsed.isNegative()) {
                final double refill = (double) elapsed.toNanos() / per.toNanos() * capacity;
                tokens = Math.min(capacity, tokens + refill);
                lastRefillAt = now;
            }

            if (tokens >= 1.0) {
                tokens -= 1.0;
                return new ConsumeResult(true, (int) Math.floor(tokens), 0);
            }

            final long retryNanos = (long) Math.ceil((1.0 - tokens) * per.toNanos() / capacity);
            final long retrySeconds = Math.max(1L, (retryNanos + 999_999_999L) / 1_000_000_000L);
            return new ConsumeResult(false, 0, retrySeconds);
        }

        synchronized Instant lastAccessedAt() {
            return lastRefillAt;
        }
    }

    /**
     * A builder for configuring a {@link RateLimitMiddleware}.
     *
     * @author reed.vonredwitz
     * @since Apr-2026
     */
    public static final class Builder {

        private int limit;
        private Duration per;
        private Function<Request, String> keyExtractor;
        private Clock clock;
        private int maxBuckets = 100_000;

        private Builder() {
        }

        /**
         * Sets the maximum number of requests allowed per window.
         *
         * @param limit the request limit
         * @return this {@link Builder}
         */
        public Builder limit(final int limit) {
            this.limit = limit;
            return this;
        }

        /**
         * Sets the duration of each rate limit window.
         *
         * @param per the window {@link Duration}
         * @return this {@link Builder}
         */
        public Builder per(final Duration per) {
            this.per = Objects.requireNonNull(per, "per");
            return this;
        }

        /**
         * Sets a custom function that extracts the rate limit key from a request.
         * <p>
         * Defaults to the remote TCP peer address ({@link Request#remoteAddress()}), or
         * {@code "unknown"} if the transport doesn't expose one. This default is not spoofable by
         * a client-supplied header. In a trusted reverse-proxy deployment where the real client
         * address is only available via a header (e.g. {@code X-Forwarded-For} set by a proxy you
         * control), supply a {@code keyExtractor} that reads it — but note that header is
         * client-controlled unless your proxy strips/overwrites it first. For other deployments,
         * a non-spoofable dimension such as an authenticated user ID is another good choice.
         *
         * @param keyExtractor the key extraction function
         * @return this {@link Builder}
         */
        public Builder keyExtractor(final Function<Request, String> keyExtractor) {
            this.keyExtractor = Objects.requireNonNull(keyExtractor, "keyExtractor");
            return this;
        }

        /**
         * Sets the maximum number of distinct rate limit keys tracked simultaneously.
         * When this limit is reached, the least-recently-used tracked key is evicted to make room
         * for the new one — the cap bounds memory without permanently locking out legitimate
         * clients once it's reached.
         * <p>
         * Defaults to {@code 100_000}.
         *
         * @param maxBuckets the maximum number of tracked keys
         * @return this {@link Builder}
         */
        public Builder maxBuckets(final int maxBuckets) {
            if (maxBuckets <= 0) {
                throw new IllegalArgumentException("maxBuckets must be positive");
            }
            this.maxBuckets = maxBuckets;
            return this;
        }

        Builder clock(final Clock clock) {
            this.clock = Objects.requireNonNull(clock, "clock");
            return this;
        }

        /**
         * Builds the {@link RateLimitMiddleware}.
         *
         * @return a new {@link RateLimitMiddleware}
         */
        public RateLimitMiddleware build() {
            if (limit <= 0) {
                throw new IllegalArgumentException("limit must be positive");
            }
            Objects.requireNonNull(per, "per");
            return new RateLimitMiddleware(this);
        }
    }
}
