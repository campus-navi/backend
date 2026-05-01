CREATE TABLE official_post_scrap
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL,
    post_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_official_post_scrap UNIQUE (member_id, post_id),
    CONSTRAINT fk_official_post_scrap_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_official_post_scrap_post FOREIGN KEY (post_id) REFERENCES official_post (id)
);

CREATE INDEX idx_official_post_scrap_post_id
    ON official_post_scrap (post_id);
