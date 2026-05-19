ALTER TABLE scrap_folder
    ADD COLUMN scrap_count BIGINT NOT NULL DEFAULT 0;

-- 기존 데이터 백필
UPDATE scrap_folder f
SET scrap_count = (SELECT COUNT(*)
                   FROM official_post_scrap s
                   WHERE s.scrap_folder_id = f.id);
