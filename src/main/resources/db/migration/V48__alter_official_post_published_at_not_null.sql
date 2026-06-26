-- 정상 경로(크롤러가 published_at == null을 저장 전 스킵)로는 null 행이 없으나
-- 방어적으로 크롤링 시각으로 보정한다. null이 없으면 0행 영향(no-op).
UPDATE official_post
SET published_at = crawled_at::date
WHERE published_at IS NULL;

ALTER TABLE official_post
    ALTER COLUMN published_at SET NOT NULL;
