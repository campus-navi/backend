-- University 테이블 생성
CREATE TABLE university (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL
);

-- Campus 테이블 생성
CREATE TABLE campus (
    id BIGSERIAL PRIMARY KEY,
    university_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(20) NOT NULL,
    domain VARCHAR(255) NOT NULL,
    CONSTRAINT fk_campus_university FOREIGN KEY (university_id) REFERENCES university(id)
);

-- Department 테이블 생성
CREATE TABLE department (
    id BIGSERIAL PRIMARY KEY,
    campus_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    college VARCHAR(100) NOT NULL,
    CONSTRAINT fk_department_campus FOREIGN KEY (campus_id) REFERENCES campus(id)
);

-- 인덱스 생성
CREATE INDEX idx_campus_university_id ON campus(university_id);
CREATE INDEX idx_department_campus_id ON department(campus_id);