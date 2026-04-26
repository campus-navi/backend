ALTER TABLE official_source
    DROP COLUMN category,
    DROP COLUMN created_at,
    DROP COLUMN updated_at,
    ALTER COLUMN source_type SET DEFAULT 'CRAWL',
    ALTER COLUMN last_crawled_at TYPE DATE USING last_crawled_at::DATE,
    ALTER COLUMN last_crawled_at SET NOT NULL,
    ALTER COLUMN last_crawled_at SET DEFAULT CURRENT_DATE;

ALTER TABLE official_post
    DROP COLUMN category,
    ALTER COLUMN published_at TYPE DATE USING published_at::DATE,
    ADD COLUMN publisher VARCHAR(50) NOT NULL;

