CREATE TABLE crawl_failure
(
    id              BIGSERIAL PRIMARY KEY,
    source_id       BIGINT        NOT NULL REFERENCES official_source (id) ON DELETE CASCADE,
    original_id     VARCHAR(200)  NOT NULL,
    detail_url      VARCHAR(500)  NOT NULL,
    title           VARCHAR(500),
    publisher       VARCHAR(50),
    published_at    DATE,
    retry_count     SMALLINT      NOT NULL DEFAULT 0,
    last_error      VARCHAR(4000),
    created_at      TIMESTAMP     NOT NULL DEFAULT now(),
    last_retried_at TIMESTAMP,
    CONSTRAINT uq_crawl_failure_source_original UNIQUE (source_id, original_id)
);

CREATE INDEX idx_crawl_failure_retry_count ON crawl_failure (retry_count);
