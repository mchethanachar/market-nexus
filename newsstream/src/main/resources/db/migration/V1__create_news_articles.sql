CREATE TABLE news_articles (
    id            BIGSERIAL       PRIMARY KEY,
    title         TEXT            NOT NULL,
    content       TEXT,
    summary       TEXT,
    url           TEXT            NOT NULL,
    url_hash      VARCHAR(64)     NOT NULL UNIQUE,
    source_name   VARCHAR(255)    NOT NULL,
    source_type   VARCHAR(32)     NOT NULL,
    published_at  TIMESTAMPTZ,
    ingested_at   TIMESTAMPTZ     NOT NULL,
    is_processed  BOOLEAN         NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_news_articles_is_processed ON news_articles (is_processed);
CREATE INDEX idx_news_articles_published_at ON news_articles (published_at);
