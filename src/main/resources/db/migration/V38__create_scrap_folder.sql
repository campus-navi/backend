-- 스크랩 폴더 (공용 도메인: 추후 커뮤니티 스크랩도 동일 폴더 참조 가능)

CREATE TABLE scrap_folder
(
    id          BIGSERIAL    PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    name        VARCHAR(20)  NOT NULL,
    description VARCHAR(20),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_scrap_folder_member_name UNIQUE (member_id, name),
    CONSTRAINT fk_scrap_folder_member FOREIGN KEY (member_id) REFERENCES member (id)
);

CREATE INDEX idx_scrap_folder_member
    ON scrap_folder (member_id);

-- 기존 전체 회원 시드 폴더 백필
INSERT INTO scrap_folder (member_id, name)
SELECT id, '나중에 볼 스크랩'
FROM member;
