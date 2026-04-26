ALTER TABLE interest_tag RENAME TO tag;
ALTER INDEX idx_interest_tag_recommendable_sort RENAME TO idx_tag_recommendable_sort;

ALTER TABLE member_interest RENAME COLUMN interest_tag_id TO tag_id;
