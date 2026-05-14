-- 운영 JVM이 UTC로 동작하여 기존 TIMESTAMP 컬럼은 UTC 의미로 저장됨.
-- USING ... AT TIME ZONE 'UTC' 로 절대시각을 보존하며 TIMESTAMPTZ 로 통일.

ALTER TABLE member
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE post
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE comment
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN deleted_at TYPE TIMESTAMPTZ USING deleted_at AT TIME ZONE 'UTC';

ALTER TABLE official_post
    ALTER COLUMN crawled_at TYPE TIMESTAMPTZ USING crawled_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC',
    ALTER COLUMN updated_at TYPE TIMESTAMPTZ USING updated_at AT TIME ZONE 'UTC';

ALTER TABLE official_attachment
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE official_attachment_download
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE official_post_scrap
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE official_post_view
    ALTER COLUMN first_viewed_at TYPE TIMESTAMPTZ USING first_viewed_at AT TIME ZONE 'UTC',
    ALTER COLUMN last_viewed_at  TYPE TIMESTAMPTZ USING last_viewed_at  AT TIME ZONE 'UTC';

ALTER TABLE official_post_notification
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE post_like
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE post_scrap
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE comment_like
    ALTER COLUMN created_at TYPE TIMESTAMPTZ USING created_at AT TIME ZONE 'UTC';

ALTER TABLE feed_recommend_snapshot
    ALTER COLUMN computed_at TYPE TIMESTAMPTZ USING computed_at AT TIME ZONE 'UTC',
    ALTER COLUMN created_at  TYPE TIMESTAMPTZ USING created_at  AT TIME ZONE 'UTC';

ALTER TABLE crawl_failure
    ALTER COLUMN created_at      TYPE TIMESTAMPTZ USING created_at      AT TIME ZONE 'UTC',
    ALTER COLUMN last_retried_at TYPE TIMESTAMPTZ USING last_retried_at AT TIME ZONE 'UTC';
