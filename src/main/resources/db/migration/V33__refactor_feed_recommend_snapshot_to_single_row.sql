-- 추천 스냅샷을 슬롯별 누적에서 멤버당 단일 row 갈아끼우기로 단순화
-- 단일 row 전환으로 7일 cleanup용 단독 인덱스는 도입하지 않음

TRUNCATE TABLE feed_recommend_snapshot;

ALTER TABLE feed_recommend_snapshot
    DROP CONSTRAINT uq_feed_recommend_snapshot;

ALTER TABLE feed_recommend_snapshot
    RENAME COLUMN slot_at TO computed_at;

ALTER TABLE feed_recommend_snapshot
    ADD CONSTRAINT uq_feed_recommend_snapshot_member UNIQUE (member_id);

ALTER TABLE feed_recommend_snapshot
    ALTER COLUMN created_at SET DEFAULT now();
