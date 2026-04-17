-- Member 테이블 생성
CREATE TABLE member (
    id           BIGSERIAL    PRIMARY KEY,
    email        VARCHAR(255) NOT NULL,
    username     VARCHAR(30)  NOT NULL,
    password     VARCHAR(255) NOT NULL,
    nickname     VARCHAR(30) NOT NULL,
    university_id BIGINT      NOT NULL,
    campus_id    BIGINT       NOT NULL,
    admission_year INT        NOT NULL,
    role         VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP,
    deleted_at   TIMESTAMP,
    CONSTRAINT fk_member_campus     FOREIGN KEY (campus_id) REFERENCES campus(id),
    CONSTRAINT uq_member_email      UNIQUE (email),
    CONSTRAINT uq_member_username   UNIQUE (username),
    CONSTRAINT uq_member_nickname   UNIQUE (nickname)
);

-- MemberDepartment 테이블 생성
CREATE TABLE member_department (
    id            BIGSERIAL PRIMARY KEY,
    member_id     BIGINT    NOT NULL,
    department_id BIGINT    NOT NULL,
    CONSTRAINT fk_member_department_member     FOREIGN KEY (member_id)     REFERENCES member(id),
    CONSTRAINT fk_member_department_department FOREIGN KEY (department_id) REFERENCES department(id),
    CONSTRAINT uq_member_department            UNIQUE (member_id, department_id)
);

-- 인덱스 생성
CREATE INDEX idx_member_university_id            ON member(university_id);
CREATE INDEX idx_member_campus_id                ON member(campus_id);
CREATE INDEX idx_member_department_member_id     ON member_department(member_id);
CREATE INDEX idx_member_department_department_id ON member_department(department_id);
