CREATE TABLE target_major (
    id         BIGSERIAL    PRIMARY KEY,
    campus_id  BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    major_type VARCHAR(30)  NOT NULL,
    CONSTRAINT fk_target_major_campus      FOREIGN KEY (campus_id) REFERENCES campus(id),
    CONSTRAINT uq_target_major_campus_name UNIQUE (campus_id, name)
);

CREATE INDEX idx_target_major_campus_type ON target_major(campus_id, major_type);

DO $$
    DECLARE
        seoul_id  BIGINT;
        sejong_id BIGINT;
    BEGIN
        SELECT id INTO seoul_id  FROM campus WHERE code = 'KU_SEOUL';
        SELECT id INTO sejong_id FROM campus WHERE code = 'KU_SEJONG';

        -- 서울캠퍼스 융합전공
        INSERT INTO target_major (campus_id, name, major_type)
        VALUES
            (seoul_id, '생태조경',                                    'CONVERGENCE_MAJOR'),
            (seoul_id, '기후변화',                                    'CONVERGENCE_MAJOR'),
            (seoul_id, '미생물융합기술',                               'CONVERGENCE_MAJOR'),
            (seoul_id, '금융공학',                                    'CONVERGENCE_MAJOR'),
            (seoul_id, '공직과 사회규범',                               'CONVERGENCE_MAJOR'),
            (seoul_id, 'PEP(Politics, Economics and Policy)',         'CONVERGENCE_MAJOR'),
            (seoul_id, '패션디자인 및 머천다이징',                     'CONVERGENCE_MAJOR'),
            (seoul_id, '다문화한국어교육',                             'CONVERGENCE_MAJOR'),
            (seoul_id, '기술·가정교육',                               'CONVERGENCE_MAJOR'),
            (seoul_id, '인문학과 정의',                               'CONVERGENCE_MAJOR'),
            (seoul_id, 'EML(Emerging Market&Latin America)',          'CONVERGENCE_MAJOR'),
            (seoul_id, 'LB&C(Language, Brain & Computer)',            'CONVERGENCE_MAJOR'),
            (seoul_id, '인문학과 문화산업',                            'CONVERGENCE_MAJOR'),
            (seoul_id, 'GLEAC(Global Leader for East Asian Century)', 'CONVERGENCE_MAJOR'),
            (seoul_id, '의료인문학',                                   'CONVERGENCE_MAJOR'),
            (seoul_id, '통일과 국제평화',                               'CONVERGENCE_MAJOR'),
            (seoul_id, '인문사회디지털',                               'CONVERGENCE_MAJOR'),
            (seoul_id, '뇌인지과학',                                   'CONVERGENCE_MAJOR'),
            (seoul_id, '소프트웨어기술벤처',                           'CONVERGENCE_MAJOR'),
            (seoul_id, '정보보호',                                     'CONVERGENCE_MAJOR'),
            (seoul_id, '인공지능응용',                                  'CONVERGENCE_MAJOR'),
            (seoul_id, '공공거버넌스와 리더십',                         'CONVERGENCE_MAJOR'),
            (seoul_id, '메디컬융합공학',                               'CONVERGENCE_MAJOR'),
            (seoul_id, 'GKS(Global Korean Studies)',                  'CONVERGENCE_MAJOR'),
            (seoul_id, '기술창업',                                     'CONVERGENCE_MAJOR'),
            (seoul_id, '에너지신산업',                                  'CONVERGENCE_MAJOR'),
            (seoul_id, '에코스마트시티',                               'CONVERGENCE_MAJOR'),
            (seoul_id, '차세대반도체',                                  'CONVERGENCE_MAJOR'),
            (seoul_id, '개인정보보호',                                  'CONVERGENCE_MAJOR');

        -- 서울캠퍼스 학생설계전공
        INSERT INTO target_major (campus_id, name, major_type)
        VALUES
            (seoul_id, '보험과 위험관리',              'STUDENT_DESIGN'),
            (seoul_id, '인적자원개발학',              'STUDENT_DESIGN'),
            (seoul_id, '소비자분석학',                'STUDENT_DESIGN'),
            (seoul_id, 'AI기반 에듀테크를 위한 학습과학', 'STUDENT_DESIGN'),
            (seoul_id, '생태진화생물학',              'STUDENT_DESIGN'),
            (seoul_id, '영상미디어정책학',            'STUDENT_DESIGN'),
            (seoul_id, '시민사회와 사회적 가치',         'STUDENT_DESIGN'),
            (seoul_id, '음악학·영화학',              'STUDENT_DESIGN'),
            (seoul_id, '머신러닝기반 미디어마케팅',   'STUDENT_DESIGN'),
            (seoul_id, '바이오헬스 혁신경영과법',     'STUDENT_DESIGN'),
            (seoul_id, '의학물리보건 융합공학',       'STUDENT_DESIGN');

        -- 세종캠퍼스 융합전공
        INSERT INTO target_major (campus_id, name, major_type)
        VALUES
            (sejong_id, '사회인구학',                  'CONVERGENCE_MAJOR'),
            (sejong_id, '공기업경영-지능형기술관리',    'CONVERGENCE_MAJOR'),
            (sejong_id, '한류문화산업경영',             'CONVERGENCE_MAJOR'),
            (sejong_id, '인공지능·데이터분석',         'CONVERGENCE_MAJOR'),
            (sejong_id, '자율주행시스템',              'CONVERGENCE_MAJOR'),
            (sejong_id, '바이오헬스케어',              'CONVERGENCE_MAJOR'),
            (sejong_id, '친환경동력시스템',            'CONVERGENCE_MAJOR'),
            (sejong_id, '지능형전장제어시스템',         'CONVERGENCE_MAJOR'),
            (sejong_id, '첨단센서융합디바이스',         'CONVERGENCE_MAJOR'),
            (sejong_id, '디스플레이-시스템반도체소부장', 'CONVERGENCE_MAJOR'),
            (sejong_id, '스마트휴먼인터페이스',         'CONVERGENCE_MAJOR'),
            (sejong_id, '차세대통신융합디바이스',       'CONVERGENCE_MAJOR'),
            (sejong_id, '모빌리티SW/AI',              'CONVERGENCE_MAJOR'),
            (sejong_id, '첨단반도체공정장비',           'CONVERGENCE_MAJOR'),
            (sejong_id, '스마트에코시티',              'CONVERGENCE_MAJOR');

        -- 세종캠퍼스 학생설계전공
        INSERT INTO target_major (campus_id, name, major_type)
        VALUES
            (sejong_id, '한중 예술 문화경영', 'STUDENT_DESIGN'),
            (sejong_id, '박물관교육학',       'STUDENT_DESIGN');
    END
$$;
