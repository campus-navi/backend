DROP INDEX idx_tag_recommendable_sort;
ALTER TABLE tag DROP COLUMN sort_order;
CREATE INDEX idx_tag_recommendable ON tag (is_recommendable);
