CREATE TABLE comment
(
    id           BIGSERIAL PRIMARY KEY,
    post_id      BIGINT       NOT NULL REFERENCES post (id),
    member_id    BIGINT       NOT NULL REFERENCES member (id),
    parent_id    BIGINT REFERENCES comment (id),
    content      TEXT         NOT NULL,
    is_anonymous BOOLEAN      NOT NULL DEFAULT TRUE,
    reply_count  INT          NOT NULL DEFAULT 0,
    like_count   INT          NOT NULL DEFAULT 0,
    deleted_at   TIMESTAMP,
    created_at   TIMESTAMP    NOT NULL
);

CREATE INDEX idx_comment_post_id ON comment (post_id);
CREATE INDEX idx_comment_parent_id ON comment (parent_id);

CREATE TABLE comment_like
(
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL REFERENCES member (id),
    comment_id BIGINT    NOT NULL REFERENCES comment (id),
    created_at TIMESTAMP NOT NULL,
    UNIQUE (member_id, comment_id)
);

CREATE INDEX idx_comment_like_member_comment ON comment_like (member_id, comment_id);
