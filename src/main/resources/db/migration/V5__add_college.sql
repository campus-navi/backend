-- College 테이블 생성
CREATE TABLE college (
    id        BIGSERIAL    PRIMARY KEY,
    campus_id BIGINT       NOT NULL,
    name      VARCHAR(100) NOT NULL,
    code      VARCHAR(20)  NOT NULL,
    CONSTRAINT fk_college_campus       FOREIGN KEY (campus_id) REFERENCES campus(id),
    CONSTRAINT uq_campus_college_name  UNIQUE (campus_id, name)
);

-- Department에 college_id 컬럼 추가
ALTER TABLE department
    ADD COLUMN college_id BIGINT,
    ADD CONSTRAINT fk_department_college FOREIGN KEY (college_id) REFERENCES college(id);

-- 인덱스 생성
CREATE INDEX idx_college_campus_id    ON college(campus_id);
CREATE INDEX idx_department_college_id ON department(college_id);
