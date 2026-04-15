# Build stage
FROM denoland/deno:2.7.2 AS builder

WORKDIR /app

COPY deno.json deno.lock* ./
RUN deno install

COPY . .
RUN deno task build

# Runtime stage
FROM denoland/deno:2.7.2

WORKDIR /app

COPY --from=builder /app/_fresh ./_fresh
COPY --from=builder /app/content ./content
COPY --from=builder /app/denote.config.ts ./denote.config.ts
COPY --from=builder /app/static ./static

EXPOSE 8000

CMD ["deno", "serve", "--allow-read=/app", "--allow-net", "--allow-env=ANALYTICS_SITE_ID,DENO_DEPLOYMENT_ID,DENO_TESTING,NODE_ENV", "_fresh/server.js"]
