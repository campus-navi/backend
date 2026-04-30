ALTER TABLE official_post_ai_meta
    DROP COLUMN recruitment_count,
    DROP COLUMN apply_method,
    ADD COLUMN apply_method_type   VARCHAR(20),
    ADD COLUMN apply_method_detail TEXT,
    ADD COLUMN is_applicable       BOOLEAN NOT NULL DEFAULT FALSE;
