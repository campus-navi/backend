ALTER TABLE feed_recommend_snapshot DROP COLUMN post_ids;

CREATE TABLE feed_recommend_snapshot_item (
    id          BIGSERIAL PRIMARY KEY,
    snapshot_id BIGINT    NOT NULL,
    post_id     BIGINT    NOT NULL,
    sort_order  INT       NOT NULL,
    CONSTRAINT uq_feed_recommend_snapshot_item UNIQUE (snapshot_id, post_id),
    CONSTRAINT fk_feed_recommend_snapshot_item_snapshot
        FOREIGN KEY (snapshot_id) REFERENCES feed_recommend_snapshot (id) ON DELETE CASCADE
);

CREATE INDEX idx_feed_recommend_snapshot_item_snapshot ON feed_recommend_snapshot_item (snapshot_id, sort_order);
