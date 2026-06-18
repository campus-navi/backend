CREATE TABLE department_restriction (
    id                      BIGSERIAL PRIMARY KEY,
    from_campus_id          BIGINT,
    from_department_id      BIGINT,
    to_department_id        BIGINT  NOT NULL,
    restrict_double_major   BOOLEAN NOT NULL DEFAULT FALSE,
    restrict_complex_major BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_dr_from_campus     FOREIGN KEY (from_campus_id)     REFERENCES campus(id),
    CONSTRAINT fk_dr_from_department FOREIGN KEY (from_department_id) REFERENCES department(id),
    CONSTRAINT fk_dr_to_department   FOREIGN KEY (to_department_id)   REFERENCES department(id)
);

CREATE INDEX idx_dr_from_campus_id     ON department_restriction(from_campus_id);
CREATE INDEX idx_dr_from_department_id ON department_restriction(from_department_id);

DO $$
    DECLARE
        seoul_id  BIGINT;
        sejong_id BIGINT;
    BEGIN
        SELECT id INTO seoul_id  FROM campus WHERE code = 'KU_SEOUL';
        SELECT id INTO sejong_id FROM campus WHERE code = 'KU_SEJONG';

        -- ============================================================
        -- 서울캠퍼스 공통 제한 (from_campus = KU_SEOUL)
        -- ============================================================

        -- 서울→세종 이중전공만 제한 (세종 22개 학과)
        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT seoul_id, NULL, d.id, TRUE, FALSE
        FROM department d JOIN campus c ON d.campus_id = c.id
        WHERE c.code = 'KU_SEJONG'
          AND d.name IN (
            '융합경영학부', '한국학전공', '중국학전공', '영미학전공', '독일학전공',
            '공공사회학전공', '경제정책학전공', '정부행정학부', '데이터계산과학전공',
            '반도체물리학부', '신소재화학과', '컴퓨터소프트웨어학과',
            '전자및정보공학과', '생명정보공학과', '식품생명공학과', '환경시스템공학과',
            '빅데이터사이언스학부', '전자·기계융합공학과', '미래모빌리티학과',
            '지능형반도체공학과', '인공지능사이버보안학과', '디지털헬스케어공학과'
        );

        -- 서울→서울/세종 이중+복합전공 제한
        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT seoul_id, NULL, d.id, TRUE, TRUE
        FROM department d JOIN campus c ON d.campus_id = c.id
        WHERE (c.code = 'KU_SEOUL' AND d.name IN (
                '학부대학', '자유전공학부', '의예과', '의학과', '간호학과',
                '사이버국방학과', '반도체공학과', '차세대통신학과', '스마트모빌리티학부',
                '교육학과', '체육교육과', '가정교육과', '수학교육과', '국어교육과', '영어교육과', '지리교육과', '역사교육과'
            ))
           OR (c.code = 'KU_SEJONG' AND d.name IN ('약학과', '첨단융합신약학과'));

        -- ============================================================
        -- 서울 개별 학과별 복합전공 제한 (from_department = 특정 서울 학과)
        -- ============================================================

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name IN ('글로벌경영전공', '디지털경영전공')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '경영학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '한국학전공'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '국어국문학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '공공사회학전공'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '사회학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '영미학전공'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '영어영문학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '독일학전공'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '독어독문학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '중국학전공'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '중어중문학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name IN ('생명정보공학과', '식품생명공학과')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '생명과학부';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '생명정보공학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '생명공학부';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '빅데이터사이언스학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '통계학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '정부행정학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '행정학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name IN ('응용수리과학부', '데이터계산과학전공')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '수학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '신소재화학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '화학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name IN ('경제정책학전공', '경제통계학부')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '경제학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '전자·기계융합공학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '기계공학부';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name IN ('전자및정보공학과', '전자·기계융합공학과')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '전기전자공학부';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEOUL'
             JOIN department t ON t.name = '인공지능사이버보안학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEJONG'
        WHERE s.name = '스마트보안학부';

        -- ============================================================
        -- 세종캠퍼스 공통 제한 (from_campus = KU_SEJONG)
        -- ============================================================

        -- 세종→서울 이중전공만 제한 (서울 21개 학과)
        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT sejong_id, NULL, d.id, TRUE, FALSE
        FROM department d JOIN campus c ON d.campus_id = c.id
        WHERE c.code = 'KU_SEOUL'
          AND d.name IN (
            '경영학과', '국어국문학과', '영어영문학과', '사회학과', '중어중문학과',
            '생명과학부', '생명공학부', '식품공학과', '경제학과', '통계학과',
            '행정학과', '수학과', '물리학과', '화학과', '전기전자공학부',
            '컴퓨터학과', '데이터과학과', '인공지능학과', '글로벌자율학부',
            '자유전공학부', '학부대학'
        );

        -- 세종→서울/세종 이중+복합전공 제한
        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT sejong_id, NULL, d.id, TRUE, TRUE
        FROM department d JOIN campus c ON d.campus_id = c.id
        WHERE (c.code = 'KU_SEOUL' AND d.name IN (
                '학부대학', '자유전공학부', '의예과', '의학과', '간호학과',
                '사이버국방학과', '반도체공학과', '차세대통신학과', '스마트모빌리티학부',
                '교육학과', '체육교육과', '가정교육과', '수학교육과', '국어교육과', '영어교육과', '지리교육과', '역사교육과'
            ))
           OR (c.code = 'KU_SEJONG' AND d.name IN ('약학과', '첨단융합신약학과'));

        -- ============================================================
        -- 세종 개별 학과별 복합전공 제한 (from_department = 특정 세종 학과)
        -- ============================================================

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '수학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '데이터계산과학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '컴퓨터학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '반도체물리학부';

        -- 신소재화학과 → 서울캠 전 학과 복합전공 불가
        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             CROSS JOIN department t JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '신소재화학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '전기전자공학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '전자및정보공학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name IN ('생명과학부', '생명공학부')
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '생명정보공학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '생명과학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '식품생명공학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '전기전자공학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '전자·기계융합공학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '스마트보안학부'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '인공지능사이버보안학과';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '국어국문학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '한국학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '영어영문학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '영미학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '독어독문학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '독일학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '중어중문학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '중국학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '경영학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name IN ('글로벌경영전공', '디지털경영전공');

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '행정학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '정부행정학부';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '사회학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '공공사회학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '경제학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '경제정책학전공';

        INSERT INTO department_restriction (from_campus_id, from_department_id, to_department_id, restrict_double_major, restrict_complex_major)
        SELECT NULL, s.id, t.id, FALSE, TRUE
        FROM department s JOIN campus sc ON s.campus_id = sc.id AND sc.code = 'KU_SEJONG'
             JOIN department t ON t.name = '통계학과'
             JOIN campus tc ON t.campus_id = tc.id AND tc.code = 'KU_SEOUL'
        WHERE s.name = '빅데이터사이언스학부';
    END
$$;
