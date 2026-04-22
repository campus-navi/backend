CREATE TABLE interest_tag
(
    id               BIGSERIAL    PRIMARY KEY,
    code             VARCHAR(100) NOT NULL,
    name             VARCHAR(100) NOT NULL,
    sort_order       SMALLINT     NOT NULL DEFAULT 0,
    is_recommendable BOOLEAN      NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_interest_tag_code UNIQUE (code)
);

CREATE INDEX idx_interest_tag_recommendable_sort ON interest_tag (is_recommendable, sort_order);

CREATE TABLE member_interest
(
    id              BIGSERIAL PRIMARY KEY,
    member_id       BIGINT NOT NULL,
    interest_tag_id BIGINT NOT NULL,
    CONSTRAINT uq_member_interest UNIQUE (member_id, interest_tag_id),
    CONSTRAINT fk_member_interest_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_interest_tag FOREIGN KEY (interest_tag_id) REFERENCES interest_tag (id)
);

CREATE INDEX idx_member_interest_member ON member_interest (member_id);
