-- official 도메인 FK 인덱스 누락 보완
CREATE INDEX idx_official_attachment_post_id    ON official_attachment (post_id);
CREATE INDEX idx_official_post_ai_meta_tag_id   ON official_post_ai_meta (tag_id);
CREATE INDEX idx_official_post_university_id    ON official_post (university_id);
CREATE INDEX idx_official_post_campus_id        ON official_post (campus_id);
CREATE INDEX idx_official_post_college_id       ON official_post (college_id);
CREATE INDEX idx_official_post_department_id    ON official_post (department_id);
CREATE INDEX idx_official_source_university_id  ON official_source (university_id);
CREATE INDEX idx_official_source_campus_id      ON official_source (campus_id);
CREATE INDEX idx_official_source_college_id     ON official_source (college_id);
CREATE INDEX idx_official_source_department_id  ON official_source (department_id);

-- 게시판/댓글/관심사 FK 인덱스 누락 보완 (역방향 조회용)
CREATE INDEX idx_post_like_post_id       ON post_like (post_id);
CREATE INDEX idx_post_scrap_post_id      ON post_scrap (post_id);
CREATE INDEX idx_comment_member_id       ON comment (member_id);
CREATE INDEX idx_comment_like_comment_id ON comment_like (comment_id);
CREATE INDEX idx_member_interest_tag_id  ON member_interest (tag_id);

-- 최신순 정렬 인덱스
CREATE INDEX idx_official_post_published_at ON official_post (published_at DESC);
CREATE INDEX idx_post_created_at            ON post (created_at DESC);

-- UNIQUE 제약과 중복되는 인덱스 제거
DROP INDEX idx_post_like_member_post;
DROP INDEX idx_post_scrap_member_post;
DROP INDEX idx_comment_like_member_comment;
