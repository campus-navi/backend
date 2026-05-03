CREATE TABLE official_post_notification (
    id         BIGSERIAL PRIMARY KEY,
    member_id  BIGINT    NOT NULL,
    post_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_official_post_notification UNIQUE (member_id, post_id),
    CONSTRAINT fk_official_post_notification_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_official_post_notification_post   FOREIGN KEY (post_id)   REFERENCES official_post (id)
);

CREATE INDEX idx_official_post_notification_post_id
    ON official_post_notification (post_id);

CREATE TABLE official_post_view (
    id               BIGSERIAL  PRIMARY KEY,
    member_id        BIGINT     NOT NULL,
    post_id          BIGINT     NOT NULL,
    first_viewed_at  TIMESTAMP  NOT NULL DEFAULT now(),
    last_viewed_at   TIMESTAMP  NOT NULL DEFAULT now(),
    view_count       INT        NOT NULL DEFAULT 1,
    CONSTRAINT uq_official_post_view UNIQUE (member_id, post_id),
    CONSTRAINT fk_official_post_view_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_official_post_view_post   FOREIGN KEY (post_id)   REFERENCES official_post (id)
);

CREATE INDEX idx_official_post_view_member_post
    ON official_post_view (member_id, post_id);
