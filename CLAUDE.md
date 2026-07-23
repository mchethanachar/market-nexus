# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

Market Nexus is a stock recommendation system for Indian markets, driven by financial news from Indian digital print media. This repo currently contains one module, `newsstream`, which polls RSS feeds, normalizes/deduplicates articles, and stores them in PostgreSQL (target: AWS Aurora Postgres) for downstream processing. It's a Maven multi-module project (`pom.xml` at root declares `newsstream` as a module; future modules — e.g. an analysis/recommendation service — are expected to be added as siblings).

## Commands

All commands run from the repo root unless noted.

```bash
# Build the whole project
mvn clean install

# Build/compile just newsstream
mvn -pl newsstream clean package

# Run tests (currently no test sources exist in newsstream)
mvn -pl newsstream test

# Run a single test class/method (once tests exist)
mvn -pl newsstream test -Dtest=ClassName#methodName

# Run the newsstream service locally (needs DB_URL, DB_USERNAME, DB_PASSWORD env vars set — see below)
mvn -pl newsstream spring-boot:run

# Build the Docker image (multi-stage: maven build -> eclipse-temurin JRE alpine runtime)
docker build -t market-nexus-newsstream ./newsstream
```

`newsstream` requires `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` at runtime (referenced directly in `application.yml`, no defaults) since it talks to Postgres for both JPA and Flyway migrations.

## Architecture (newsstream module)

Package structure under `com.marketnexus.newsstream`, one package per architectural concern:

- **`source`** — `NewsSource` interface (`fetch()` -> `List<RawNews>`, `getName()`). `GenericRssFeedSource` is the sole implementation: fetches over `HttpURLConnection` with a spoofed browser User-Agent, sanitizes malformed feed XML (strips duplicate `<!DOCTYPE>`, escapes bare `&`) before parsing with Rome. `RssFeedSourceRegistry` builds the active `List<NewsSource>` at startup from `NewsStreamProperties`, filtering to `enabled: true` feeds only.
- **`config`** — `NewsStreamProperties` (`@ConfigurationProperties(prefix = "newsstream.feeds")`) binds the `sources` list (name/url/enabled) and `poll-interval-ms` from `application.yml`. `AppConfig` enables the properties binding; `SchedulerConfig` enables `@Scheduled`.
- **`scheduler`** — `FeedPollingScheduler` runs `IngestionService.ingestAll()` on a fixed delay (`newsstream.feeds.poll-interval-ms`, default 300000ms/5min via `@Scheduled(fixedDelayString = ...)`), catching and logging exceptions per cycle so one bad cycle doesn't kill scheduling.
- **`service`** — `IngestionServiceImpl` orchestrates: for each `NewsSource`, fetch raw articles, skip ones `DeduplicationService.isDuplicate()` flags, persist the rest, and log per-source and aggregate fetched/saved/skipped counts. `DeduplicationServiceImpl` hashes `title + "|" + link` with SHA-256 (`HashUtil`) and checks existence via `NewsArticleRepository.existsByUrlHash`.
- **`model`** — `RawNews` (record; raw parsed feed entry) vs. `NewsArticle` (JPA entity; persisted, adds `urlHash`, `sourceType`, `isProcessed`). `NewsArticle.from(RawNews, hash)` is the mapping point between the two.
- **`repository`** — `NewsArticleRepository` (Spring Data JPA). Note dedup is enforced at two layers: an app-level existence check before fetch, *and* a native `insertIfAbsent` query using `ON CONFLICT (url_hash) DO NOTHING` as the real guarantee against races/duplicates. `markProcessedByIds` is the intended hook for a downstream consumer to mark articles processed after analysis.
- **`util`** — `HashUtil.sha256(title, url)`, the sole dedup hashing primitive.

### Data flow
`FeedPollingScheduler` (timer) → `IngestionService.ingestAll()` → for each enabled `NewsSource.fetch()` → `DeduplicationService` filters → `NewsArticleRepository.insertIfAbsent()` (Postgres `ON CONFLICT DO NOTHING` on `url_hash`) → rows land with `is_processed = false` for a not-yet-built downstream module to consume via `findByIsProcessedFalse()` / `markProcessedByIds()`.

### Schema
Flyway migration `V1__create_news_articles.sql` is the source of truth for the `news_articles` table; `spring.jpa.hibernate.ddl-auto` is `validate`, so entity/schema drift will fail startup rather than auto-migrate — new columns/fields require a new Flyway migration, not just an entity change.

## Design docs

`prompts/*.md` are the original per-concern design prompts (config, dedup, ingestion, logging, model, refactor, repository, rss-source, scheduler, structure) used to build this module incrementally. They describe intended responsibilities per package/class and are useful context for *why* something is structured a certain way, but the actual source is authoritative over these when they disagree.
