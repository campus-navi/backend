-- Post 테이블 생성
CREATE TABLE post
(
    id            BIGSERIAL PRIMARY KEY,
    university_id BIGINT       NOT NULL,
    member_id     BIGINT       NOT NULL,
    title         VARCHAR(255) NOT NULL,
    content       TEXT         NOT NULL,
    is_anonymous  BOOLEAN      NOT NULL DEFAULT TRUE,
    like_count    INT          NOT NULL DEFAULT 0,
    scrap_count   INT          NOT NULL DEFAULT 0,
    comment_count INT          NOT NULL DEFAULT 0,
    deleted_at    TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP,
    CONSTRAINT fk_post_university FOREIGN KEY (university_id) REFERENCES university (id),
    CONSTRAINT fk_post_member FOREIGN KEY (member_id) REFERENCES member (id)
);

-- PostImage 테이블 생성
CREATE TABLE post_image
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT   NOT NULL,
    s3_url     TEXT     NOT NULL,
    sort_order SMALLINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_post_image_post FOREIGN KEY (post_id) REFERENCES post (id)
);

-- 인덱스 생성
CREATE INDEX idx_post_university_id ON post (university_id);
CREATE INDEX idx_post_member_id ON post (member_id);
CREATE INDEX idx_post_deleted_at ON post (deleted_at);
CREATE INDEX idx_post_image_post_id ON post_image (post_id);
