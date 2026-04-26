INSERT INTO college(campus_id, name)
VALUES
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '자유전공학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '스마트도시학부');

-- 글로벌학부 → 독일학전공 이름 변경
UPDATE department
SET name = '독일학전공'
WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG')
  AND name = '글로벌학부';

-- 스마트도시학부 college 연결 (NULL → 스마트도시학부)
UPDATE department
SET college_id = (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '스마트도시학부')
WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG')
  AND name = '스마트도시학부';

-- 각 단과대별 자유전공 제거 후 단일 자유전공학부로 통합
DELETE FROM department
WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG')
  AND name IN (
      '자유전공학부(공공정책)',
      '자유전공학부(과학기술)',
      '자유전공학부(글로벌비즈니스)',
      '자유전공학부(문화스포츠)'
  );

INSERT INTO department(campus_id, name, college_id)
VALUES (
    (SELECT id FROM campus WHERE code = 'KU_SEJONG'),
    '자유전공학부',
    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '자유전공학부')
);
