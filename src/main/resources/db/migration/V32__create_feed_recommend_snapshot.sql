CREATE TABLE feed_recommend_snapshot (
    id         BIGSERIAL  PRIMARY KEY,
    member_id  BIGINT     NOT NULL,
    slot_at    TIMESTAMP  NOT NULL,
    post_ids   JSONB      NOT NULL,
    created_at TIMESTAMP  NOT NULL,
    CONSTRAINT uq_feed_recommend_snapshot UNIQUE (member_id, slot_at),
    CONSTRAINT fk_feed_recommend_snapshot_member FOREIGN KEY (member_id) REFERENCES member (id)
);

-- 슬롯 단위 조회는 UNIQUE(member_id, slot_at)가 커버한다.
-- 7일 보관 정리 잡용 slot_at 단독 인덱스는 PR2에서 정리 잡을 도입할 때 함께 추가한다.
