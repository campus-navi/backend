-- University: name 유니크 제약 추가
ALTER TABLE university
    ADD CONSTRAINT uq_university_name UNIQUE (name);

-- Campus: name, code 유니크 제약 추가
ALTER TABLE campus
    ADD CONSTRAINT uq_campus_name UNIQUE (name),
    ADD CONSTRAINT uq_campus_code UNIQUE (code);

-- Department: (campus_id, name) 복합 유니크 제약 추가
ALTER TABLE department
    ADD CONSTRAINT uq_campus_department_name UNIQUE (campus_id, name);
