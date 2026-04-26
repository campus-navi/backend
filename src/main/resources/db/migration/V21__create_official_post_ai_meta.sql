CREATE TABLE official_post_ai_meta
(
    id                  BIGSERIAL    PRIMARY KEY,
    official_post_id    BIGINT       NOT NULL UNIQUE,
    summary             TEXT,
    target_grade_min    SMALLINT,
    target_grade_max    SMALLINT,
    tag_id              BIGINT,
    keyword             TEXT[],
    contact_phone       VARCHAR(50),
    contact_email       VARCHAR(200),
    start_date          DATE,
    start_time          TIME,
    end_date            DATE,
    end_time            TIME,
    required_documents  VARCHAR(100),
    apply_method        TEXT,
    eligibility         TEXT,
    recruitment_count   VARCHAR(100),
    status              VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    retry_count         INT          NOT NULL DEFAULT 0,
    failure_reason      TEXT,
    processed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,

    CONSTRAINT fk_ai_meta_post FOREIGN KEY (official_post_id) REFERENCES official_post (id),
    CONSTRAINT fk_ai_meta_tag  FOREIGN KEY (tag_id) REFERENCES tag (id)
);

CREATE INDEX idx_ai_meta_status ON official_post_ai_meta (status, retry_count);
CREATE INDEX idx_ai_meta_sub_keyword ON official_post_ai_meta USING GIN (keyword);
