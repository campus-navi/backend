CREATE INDEX idx_official_post_view_member_last_viewed
    ON official_post_view (member_id, last_viewed_at DESC);
