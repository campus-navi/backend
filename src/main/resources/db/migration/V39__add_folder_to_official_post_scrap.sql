-- 공식정보 스크랩을 폴더 종속으로 확장 (member, post, folder) 단위

ALTER TABLE official_post_scrap
    ADD COLUMN scrap_folder_id BIGINT;

-- 기존 플랫 스크랩을 회원별 기본 폴더 '나중에 볼 스크랩'으로 백필
UPDATE official_post_scrap s
SET scrap_folder_id = (SELECT f.id
                       FROM scrap_folder f
                       WHERE f.member_id = s.member_id
                         AND f.name = '나중에 볼 스크랩');

ALTER TABLE official_post_scrap
    ALTER COLUMN scrap_folder_id SET NOT NULL;

ALTER TABLE official_post_scrap
    DROP CONSTRAINT uq_official_post_scrap;
ALTER TABLE official_post_scrap
    ADD CONSTRAINT uq_official_post_scrap UNIQUE (member_id, post_id, scrap_folder_id);

-- 폴더 삭제 시 보관된 스크랩 동반 삭제 (도메인 경계 보호: DB 연쇄)
ALTER TABLE official_post_scrap
    ADD CONSTRAINT fk_official_post_scrap_folder
        FOREIGN KEY (scrap_folder_id) REFERENCES scrap_folder (id) ON DELETE CASCADE;

CREATE INDEX idx_official_post_scrap_folder
    ON official_post_scrap (scrap_folder_id);
