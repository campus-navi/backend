-- feed_recommend_snapshot: 단일 row → 슬롯별 다중 row
-- V33의 단일 row 결정 부분 번복. 슬롯별 history 역할까지 겸함
TRUNCATE TABLE feed_recommend_snapshot;
ALTER TABLE feed_recommend_snapshot
    DROP CONSTRAINT IF EXISTS uq_feed_recommend_snapshot_member;

ALTER TABLE feed_recommend_snapshot
    ADD COLUMN slot_at TIMESTAMPTZ NOT NULL DEFAULT now();

ALTER TABLE feed_recommend_snapshot
    ALTER COLUMN slot_at DROP DEFAULT;

ALTER TABLE feed_recommend_snapshot
    DROP COLUMN computed_at;

ALTER TABLE feed_recommend_snapshot
    ADD CONSTRAINT uq_feed_recommend_snapshot_member_slot
        UNIQUE (member_id, slot_at);

CREATE INDEX idx_feed_recommend_snapshot_member_slot
    ON feed_recommend_snapshot (member_id, slot_at DESC);

-- activity_notification_snapshot: 09:00 캡쳐 결과
-- missed_date = 사용자가 그 공지를 놓친 날짜 (cron 실행일의 어제, 윈도우 시작 측).
CREATE TABLE activity_notification_snapshot (
    id          BIGSERIAL    PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    missed_date DATE         NOT NULL,
    post_ids    JSONB        NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_activity_notification_snapshot_member_date UNIQUE (member_id, missed_date),
    CONSTRAINT fk_activity_notification_snapshot_member
        FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE INDEX idx_activity_notification_snapshot_member_date
    ON activity_notification_snapshot (member_id, missed_date DESC);
