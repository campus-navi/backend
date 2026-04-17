-- 대학
INSERT INTO university(name)
VALUES ('고려대학교');

-- 캠퍼스
INSERT INTO campus(university_id, name, code, domain)
VALUES ((SELECT id FROM university WHERE name = '고려대학교'), '고려대학교(서울캠퍼스)', 'KU_SEOUL', 'korea.ac.kr'),
       ((SELECT id FROM university WHERE name = '고려대학교'), '고려대학교(세종캠퍼스)', 'KU_SEJONG', 'korea.ac.kr');

-- 단과대 목록
INSERT INTO college(campus_id, name)
VALUES
    -- 서울캠퍼스
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '간호대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '경영대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '공과대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '국제대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '디자인조형학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '문과대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '미디어대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '보건과학대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '사범대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '생명과학대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '스마트모빌리티학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '심리학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '의과대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '스마트보안학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '이과대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '자유전공학부'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '정경대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '정보대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '학부대학'),

    -- 세종캠퍼스
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '공공정책대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '과학기술대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '글로벌비즈니스대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '문화스포츠대학'),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '약학대학');

-- 학과
INSERT INTO department(campus_id, name, college_id)
VALUES
    -- ===== 서울캠퍼스 =====

    -- 간호대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '간호학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '간호대학')),

    -- 경영대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '경영학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '경영대학')),

    -- 공과대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '건축사회환경공학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '건축학과',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '공과대학',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '기계공학부',        (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '반도체공학과',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '산업경영공학부',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '신소재공학부',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '융합에너지공학과',  (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '전기전자공학부',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '차세대통신학과',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '화공생명공학과',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '공과대학')),

    -- 국제대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '국제학부',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '국제대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '글로벌자율학부',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '국제대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '글로벌한국융합학부',(SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '국제대학')),

    -- 디자인조형학부 (단과대=학부 1:1)
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '디자인조형학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '디자인조형학부')),

    -- 문과대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '국어국문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '노어노문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '독어독문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '불어불문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '사학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '사회학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '서어서문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '언어학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '영어영문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '일어일문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '중어중문학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '철학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '한국사학과',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '한문학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '문과대학')),

    -- 미디어대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '글로벌엔터테인먼트학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '미디어대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '미디어학부',             (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '미디어대학')),

    -- 보건과학대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '바이오시스템의과학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '보건과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '바이오의공학부',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '보건과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '보건정책관리학부',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '보건과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '보건환경융합과학부',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '보건과학대학')),

    -- 사범대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '가정교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '교육학과',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '국어교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '수학교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '역사교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '영어교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '지리교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '체육교육과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '사범대학')),

    -- 생명과학대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '생명공학부',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '생명과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '생명과학부',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '생명과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '식품공학과',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '생명과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '식품자원경제학과',(SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '생명과학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '환경생태공학부',  (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '생명과학대학')),

    -- 스마트모빌리티학부 (단과대=학부 1:1)
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '스마트모빌리티학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '스마트모빌리티학부')),

    -- 스마트보안학부
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '사이버국방학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '스마트보안학부')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '스마트보안학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '스마트보안학부')),

    -- 심리학부 (단과대=학부 1:1)
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '심리학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '심리학부')),

    -- 의과대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '의예과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '의과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '의학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '의과대학')),

    -- 이과대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '물리학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '이과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '수학과',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '이과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '지구환경과학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '이과대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '화학과',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '이과대학')),

    -- 자유전공학부 (단과대=학부 1:1)
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '자유전공학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '자유전공학부')),

    -- 정경대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '경제학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정경대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '정경대학',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정경대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '정치외교학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정경대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '통계학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정경대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '행정학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정경대학')),

    -- 정보대학
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '데이터과학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정보대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '인공지능학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정보대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '컴퓨터학과',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '정보대학')),

    -- 학부대학 (단과대=학부 1:1)
    ((SELECT id FROM campus WHERE code = 'KU_SEOUL'), '학부대학', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEOUL') AND name = '학부대학')),

    -- ===== 세종캠퍼스 =====

    -- 공공정책대학
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '경제정책학전공',        (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '경제통계학부',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '공공사회·통일외교학부', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '공공사회학전공',        (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '빅데이터사이언스학부',  (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '정부행정학부',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '통일외교안보전공',      (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '자유전공학부(공공정책)',(SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '공공정책대학')),

    -- 과학기술대학
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '데이터계산과학전공',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '디지털헬스케어공학과',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '미래모빌리티학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '반도체물리학부',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '생명정보공학과',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '식품생명공학과',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '신소재화학과',           (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '응용수리과학부',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '인공지능사이버보안학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '전자·기계융합공학과',    (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '전자및정보공학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '지능형반도체공학과',     (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '환경시스템공학과',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '자유전공학부(과학기술)', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '컴퓨터소프트웨어학과',   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '과학기술대학')),

    -- 글로벌비즈니스대학
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '글로벌경영전공',               (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '글로벌학부',                   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '디지털경영전공',               (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '영미학전공',                   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '융합경영학부',                 (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '중국학전공',                   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '표준·지식학과',               (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '한국학전공',                   (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '자유전공학부(글로벌비즈니스)', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '글로벌비즈니스대학')),

    -- 단과대 구분없음
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '스마트도시학부', NULL),

    -- 문화스포츠대학
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '국제스포츠학부',           (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '문화유산융합학부',         (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '문화창의학부',             (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '문화콘텐츠전공',           (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '미디어문예창작전공',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '스포츠과학전공',           (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '스포츠비즈니스전공',       (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '자유전공학부(문화스포츠)', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '문화스포츠대학')),

    -- 약학대학
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '약학과',          (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '약학대학')),
    ((SELECT id FROM campus WHERE code = 'KU_SEJONG'), '첨단융합신약학과', (SELECT id FROM college WHERE campus_id = (SELECT id FROM campus WHERE code = 'KU_SEJONG') AND name = '약학대학'));
