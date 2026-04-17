-- PostLike 테이블 생성
CREATE TABLE post_like
(
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    post_id   BIGINT NOT NULL,
    CONSTRAINT uq_post_like UNIQUE (member_id, post_id),
    CONSTRAINT fk_post_like_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_post_like_post FOREIGN KEY (post_id) REFERENCES post (id)
);

-- PostScrap 테이블 생성
CREATE TABLE post_scrap
(
    id        BIGSERIAL PRIMARY KEY,
    member_id BIGINT NOT NULL,
    post_id   BIGINT NOT NULL,
    CONSTRAINT uq_post_scrap UNIQUE (member_id, post_id),
    CONSTRAINT fk_post_scrap_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_post_scrap_post FOREIGN KEY (post_id) REFERENCES post (id)
);

CREATE INDEX idx_post_like_member_post ON post_like (member_id, post_id);
CREATE INDEX idx_post_scrap_member_post ON post_scrap (member_id, post_id);
