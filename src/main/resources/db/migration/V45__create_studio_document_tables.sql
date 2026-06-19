CREATE TABLE studio_document (
    id            BIGSERIAL    PRIMARY KEY,
    member_id     BIGINT       NOT NULL,
    document_type VARCHAR(50)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    metadata      JSONB        NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_sd_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE document_section (
    id           BIGSERIAL    PRIMARY KEY,
    document_id  BIGINT       NOT NULL,
    section_key  VARCHAR(50)  NOT NULL,
    sort_order   INT          NOT NULL,
    content      TEXT         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ds_document_section UNIQUE (document_id, section_key),
    CONSTRAINT fk_ds_document FOREIGN KEY (document_id) REFERENCES studio_document(id)
);

CREATE TABLE document_ai_feedback (
    id              BIGSERIAL PRIMARY KEY,
    section_id      BIGINT    NOT NULL,
    order_num       INT       NOT NULL,
    sentence        TEXT      NOT NULL,
    original_text   TEXT      NOT NULL,
    suggestion      TEXT      NOT NULL,
    reason          TEXT      NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_daf_section FOREIGN KEY (section_id) REFERENCES document_section(id)
);

CREATE INDEX idx_sd_member_updated ON studio_document(member_id, updated_at DESC);
CREATE INDEX idx_daf_section_id    ON document_ai_feedback(section_id);
