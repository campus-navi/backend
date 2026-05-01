CREATE TABLE official_attachment_download
(
    id            BIGSERIAL PRIMARY KEY,
    member_id     BIGINT    NOT NULL,
    attachment_id BIGINT    NOT NULL,
    post_id       BIGINT    NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    CONSTRAINT fk_official_attachment_download_member
        FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_official_attachment_download_attachment
        FOREIGN KEY (attachment_id) REFERENCES official_attachment (id),
    CONSTRAINT fk_official_attachment_download_post
        FOREIGN KEY (post_id) REFERENCES official_post (id)
);

CREATE INDEX idx_official_attachment_download_member_post
    ON official_attachment_download (member_id, post_id);

CREATE INDEX idx_official_attachment_download_attachment_id
    ON official_attachment_download (attachment_id);
