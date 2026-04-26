DO
$$
    DECLARE
        ku_id        BIGINT;
        ku_sejong_id BIGINT;
        ku_seoul_id  BIGINT;

        sejong_basic CONSTANT VARCHAR(255) := 'KU_SEJONG_BASIC';
        seoul_basic  CONSTANT VARCHAR(255) := 'KU_SEOUL_BASIC';
        sejong_rss   CONSTANT VARCHAR(255) := 'KU_SEJONG_RSS';
        seed_date    CONSTANT DATE := '2026-03-01';
    BEGIN
        SELECT id INTO ku_id FROM university WHERE name = '고려대학교';
        SELECT id INTO ku_sejong_id FROM campus WHERE code = 'KU_SEJONG';
        SELECT id INTO ku_seoul_id FROM campus WHERE code = 'KU_SEOUL';

        -- 고려대학교 세종캠퍼스 공통 공지사항
        INSERT INTO official_source(university_id, campus_id, parser_type, name, list_url, last_crawled_at)
        VALUES
            (ku_id, ku_sejong_id, sejong_rss, '고려대학교 세종캠퍼스 학사공지', 'https://sejong.korea.ac.kr/bbs/koreaSejong/658/rssList.do?row=50', seed_date),
            (ku_id, ku_sejong_id, sejong_rss, '고려대학교 세종캠퍼스 등록공지', 'https://sejong.korea.ac.kr/bbs/koreaSejong/658/rssList.do?row=50', seed_date),
            (ku_id, ku_sejong_id, sejong_rss, '고려대학교 세종캠퍼스 장학공지', 'https://sejong.korea.ac.kr/bbs/koreaSejong/659/rssList.do?row=50', seed_date),
            (ku_id, ku_sejong_id, sejong_rss, '고려대학교 세종캠퍼스 일반공지', 'https://sejong.korea.ac.kr/bbs/koreaSejong/664/rssList.do?row=50', seed_date);

        -- 고려대학교 세종캠퍼스 단과대 공지사항
        INSERT INTO official_source(university_id, campus_id, college_id, parser_type, name, list_url, last_crawled_at)
        VALUES
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '과학기술대학'),    sejong_basic, '과학기술대학 공지',          'https://st.korea.ac.kr/st/2496/subview.do',              seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '약학대학'),        sejong_basic, '약학대학 학사공지',          'https://pharm.korea.ac.kr/pharm/3674/subview.do',        seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '약학대학'),        sejong_rss, '약학대학 일반공지',          'https://pharm.korea.ac.kr/bbs/pharm/430/rssList.do?row=50',        seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '글로벌비즈니스대학'), sejong_basic, '글로벌비즈니스대학 학사공지', 'https://kucec.korea.ac.kr/kucec/3787/subview.do',         seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '글로벌비즈니스대학'), sejong_rss, '글로벌비즈니스대학 일반공지', 'https://kucec.korea.ac.kr/bbs/kucec/439/rssList.do?row=50',         seed_date),
                                (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '공공정책대학'),    sejong_basic, '공공정책대학 공지',          'https://cpp.korea.ac.kr/cpp/4384/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM college WHERE campus_id = ku_sejong_id AND name = '문화스포츠대학'),  sejong_rss, '문화스포츠대학 공지',        'https://kuccs.korea.ac.kr/bbs/kuccs/161/rssList.do?row=50',        seed_date);

        -- 고려대학교 세종캠퍼스 학과 공지사항
        INSERT INTO official_source(university_id, campus_id, department_id, parser_type, name, list_url, last_crawled_at)
        VALUES
            -- 과학기술대학
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '데이터계산과학전공'),   sejong_basic, '데이터계산과학전공 공지사항',   'https://imath.korea.ac.kr/imath/2567/subview.do',          seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '인공지능사이버보안학과'), sejong_basic, '인공지능사이버보안학과 공지사항', 'https://secu.korea.ac.kr/secu/5393/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '반도체물리학부'),       sejong_basic, '반도체물리학부 공지사항',       'https://dsphy.korea.ac.kr/dsphy/2658/subview.do',          seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '신소재화학과'),         sejong_rss, '신소재화학과 공지사항',         'https://amchem.korea.ac.kr/bbs/amchem/600/rssList.do?row=50',        seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '컴퓨터소프트웨어학과'), sejong_rss, '컴퓨터소프트웨어학과 공지사항', 'https://software.korea.ac.kr/bbs/software/352/rssList.do?row=50',   seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '전자및정보공학과'),     sejong_basic, '전자및정보공학과 공지사항',     'https://kueie.korea.ac.kr/kueie/2929/subview.do',          seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '생명정보공학과'),       sejong_basic, '생명정보공학과 공지사항',       'https://biotechnology.korea.ac.kr/biotechnology/3008/subview.do', seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '식품생명공학과'),       sejong_rss, '식품생명공학과 공지사항',       'https://kfbt.korea.ac.kr/bbs/kfbt/118/rssList.do?row=50',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '전자·기계융합공학과'),  sejong_rss, '전자·기계융합공학과 공지사항',  'https://emse.korea.ac.kr/bbs/emse/408/rssList.do?row=50',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '환경시스템공학과'),     sejong_rss, '환경시스템공학과 공지사항',     'https://env.korea.ac.kr/bbs/env/414/rssList.do?row=50',              seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '미래모빌리티학과'),     sejong_rss, '미래모빌리티학과 공지사항',     'https://am.korea.ac.kr/bbs/am/610/rssList.do?row=50',                seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '지능형반도체공학과'),   sejong_rss, '지능형반도체공학과 공지사항',   'https://aisemi.korea.ac.kr/bbs/AISEMI/425/rssList.do?row=50',        seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '디지털헬스케어공학과'), sejong_rss, '디지털헬스케어공학과 공지사항', 'https://gpa.korea.ac.kr/bbs/dhe/1047/rssList.do?row=50',             seed_date),

            -- 약학대학
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '약학과'),          sejong_basic, '약학과 공지사항',          'https://pharm.korea.ac.kr/pharm/3674/subview.do',          seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '첨단융합신약학과'), sejong_basic, '첨단융합신약학과 공지사항', 'https://sejong.korea.ac.kr/dcps/11622/subview.do',         seed_date),

            -- 글로벌비즈니스대학
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '한국학전공'),     sejong_basic, '한국학전공 공지사항',     'https://kucec.korea.ac.kr/koreanstudies/3889/subview.do',  seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '중국학전공'),     sejong_basic, '중국학전공 공지사항',     'https://kucec.korea.ac.kr/gchina/3998/subview.do',         seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '영미학전공'),     sejong_basic, '영미학전공 공지사항',     'https://kucec.korea.ac.kr/ell/4073/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '글로벌경영전공'), sejong_basic, '글로벌경영전공 공지사항', 'https://kucec.korea.ac.kr/ba/4145/subview.do',             seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '디지털경영전공'), sejong_basic, '디지털경영전공 공지사항', 'https://digb.korea.ac.kr/digb/4229/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '표준·지식학과'), sejong_basic, '표준·지식학과 공지사항', 'https://kucec.korea.ac.kr/sti/4310/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '독일학전공'), sejong_basic, '독일학전공 공지사항', 'https://german.korea.ac.kr/german/8102/subview.do',             seed_date),

            -- 공공정책대학
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '정부행정학부'),        sejong_basic, '정부행정학부 공지사항',        'https://cpp.korea.ac.kr/spa/4479/subview.do',              seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '공공사회학전공'),      sejong_basic, '공공사회학전공 공지사항',      'https://cpp.korea.ac.kr/pubs/4567/subview.do',             seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '통일외교안보전공'),    sejong_basic, '통일외교안보전공 공지사항',    'https://kuds.korea.ac.kr/kuds/4648/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '경제정책학전공'),      sejong_rss, '경제정책학전공 공지사항',      'https://cpp.korea.ac.kr/bbs/economics/158/rssList.do?row=50',        seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '빅데이터사이언스학부'), sejong_rss, '빅데이터사이언스학부 공지사항', 'https://cpp.korea.ac.kr/bbs/bigdatascience/479/rssList.do?row=50',   seed_date),

            -- 문화스포츠대학
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '국제스포츠학부'),    sejong_basic, '국제스포츠학부 공지사항',    'https://kuccs.korea.ac.kr/sfa/4927/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '문화유산융합학부'),  sejong_basic, '문화유산융합학부 공지사항',  'https://kuccs.korea.ac.kr/cuhc/5033/subview.do',           seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '미디어문예창작전공'), sejong_basic, '미디어문예창작전공 공지사항', 'https://cwms.korea.ac.kr/cwms/5130/subview.do',            seed_date),
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '문화콘텐츠전공'),    sejong_rss, '문화콘텐츠전공 공지사항',    'https://kuccs.korea.ac.kr/bbs/kuccs/161/rssList.do?row=50',           seed_date),

            -- 스마트도시학부
            (ku_id, ku_sejong_id, (SELECT id FROM department WHERE campus_id = ku_sejong_id AND name = '스마트도시학부'),    sejong_rss, '스마트도시학부 공지사항',    'https://smartcity.korea.ac.kr/bbs/smartcity/517/rssList.do?row=50',  seed_date);

        -- 고려대학교 서울캠퍼스 학과 공지사항
        INSERT INTO official_source(university_id, campus_id, department_id, parser_type, name, list_url, last_crawled_at)
        VALUES
            -- 문과대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '국어국문학과'), seoul_basic, '국어국문학과 공지사항', 'https://lib001.korea.ac.kr/lib001/community/depart_notice.do',          seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '철학과'),       seoul_basic, '철학과 공지사항',       'https://lib003.korea.ac.kr/lib003/notice/notice.do',                    seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '한국사학과'),   seoul_basic, '한국사학과 공지사항',   'https://koreahistory.korea.ac.kr/koreahistory/news/notice.do',          seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '사학과'),       seoul_basic, '사학과 공지사항',       'https://kuhistory.korea.ac.kr/kuhistory/freeboard.do',                  seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '사회학과'),     seoul_basic, '사회학과 공지사항',     'https://socio.korea.ac.kr/socio/commu/notice.do',                       seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '한문학과'),     seoul_basic, '한문학과 공지사항',     'https://clschn.korea.ac.kr/lib015/community/notice_under.do',           seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '영어영문학과'), seoul_basic, '영어영문학과 공지사항', 'https://english.korea.ac.kr/english/community/notice.do',               seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '독어독문학과'), seoul_basic, '독어독문학과 공지사항', 'https://kugermanistik.korea.ac.kr/german/board/01.do',                  seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '불어불문학과'), seoul_basic, '불어불문학과 공지사항', 'https://kufra.korea.ac.kr/kufra/board/notice.do',                       seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '중어중문학과'), seoul_basic, '중어중문학과 공지사항', 'https://kuchinese.korea.ac.kr/kuchinese/under/notice.do',               seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '노어노문학과'), seoul_basic, '노어노문학과 공지사항', 'https://kuruss.korea.ac.kr/kuruss/board/notice.do',                     seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '일어일문학과'), seoul_basic, '일어일문학과 공지사항', 'https://kujap.korea.ac.kr/kujap/community/notice_under.do',             seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '서어서문학과'), seoul_basic, '서어서문학과 공지사항', 'https://spanish.korea.ac.kr/spa_kor/community/notice_under.do',         seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '언어학과'),     seoul_basic, '언어학과 공지사항',     'https://kling.korea.ac.kr/kling/commu/notice.do',                       seed_date),

            -- 생명과학대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '생명과학부'),       seoul_basic, '생명과학부 공지사항',       'https://ls.korea.ac.kr/ls/board/notice.do',                            seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '생명공학부'),       seoul_basic, '생명공학부 공지사항',       'https://bio.korea.ac.kr/bio/commu/notice.do',                          seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '식품공학과'),       seoul_basic, '식품공학과 공지사항',       'https://foodscience.korea.ac.kr/foodscience/news/notice.do',           seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '식품자원경제학과'), seoul_basic, '식품자원경제학과 공지사항', 'https://frecon.korea.ac.kr/frecon/news/notice.do',                     seed_date),

            -- 정경대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '정치외교학과'), seoul_basic, '정치외교학과 공지사항', 'https://politics.korea.ac.kr/kupolitics_kor/community/under_school.do', seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '경제학과'),     seoul_basic, '경제학과 공지사항',     'https://econ2.korea.ac.kr/econ/community/notice_under.do',              seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '통계학과'),     seoul_basic, '통계학과 공지사항',     'https://stat.korea.ac.kr/stat/community/notice_under.do',               seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '행정학과'),     seoul_basic, '행정학과 공지사항',     'https://kupa.korea.ac.kr/ko/news/notice.do',                            seed_date),

            -- 이과대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '수학과'),         seoul_basic, '수학과 공지사항',         'https://math.korea.ac.kr/math/intro/notice.do',                        seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '물리학과'),       seoul_basic, '물리학과 공지사항',       'https://physics.korea.ac.kr/physics/community/notice.do',              seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '화학과'),         seoul_basic, '화학과 공지사항',         'https://chem.korea.ac.kr/chemistry/events/notice.do',                  seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '지구환경과학과'), seoul_basic, '지구환경과학과 공지사항', 'https://ees.korea.ac.kr/eaes/news/notice.do',                           seed_date),

            -- 공과대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '산업경영공학부'),  seoul_basic, '산업경영공학부 공지사항',  'https://ie.korea.ac.kr/ie/news/notice.do',                             seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '융합에너지공학과'), seoul_basic, '융합에너지공학과 공지사항', 'https://ienergy.korea.ac.kr/iee/community/Notice.do',                  seed_date),

            -- 사범대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '교육학과'),   seoul_basic, '교육학과 공지사항',   'https://edu.korea.ac.kr/edu/board/notice.do',                           seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '체육교육과'), seoul_basic, '체육교육과 공지사항', 'https://phyedu.korea.ac.kr/phyedu/reference/notice.do',                  seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '가정교육과'), seoul_basic, '가정교육과 공지사항', 'https://homedu.korea.ac.kr/homedu/board/notice.do',                      seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '수학교육과'), seoul_basic, '수학교육과 공지사항', 'https://mathedu.korea.ac.kr/mathedu/community/notice.do',                seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '국어교육과'), seoul_basic, '국어교육과 공지사항', 'https://koredu.korea.ac.kr/koredu/notic.do',                              seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '영어교육과'), seoul_basic, '영어교육과 공지사항', 'https://eled.korea.ac.kr/eled/notice/notice.do',                          seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '지리교육과'), seoul_basic, '지리교육과 공지사항', 'https://geoedu.korea.ac.kr/geoedu/join/notice.do',                        seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '역사교육과'), seoul_basic, '역사교육과 공지사항', 'https://hisedu.korea.ac.kr/hisedu/comunity/professor.do',                 seed_date),

            -- 간호대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '간호학과'), seoul_basic, '간호학과 공지사항', 'https://nursing.korea.ac.kr/nursing/community/notice_under.do',            seed_date),

            -- 정보대학
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '컴퓨터학과'),   seoul_basic, '컴퓨터학과 공지사항',   'https://cs.korea.ac.kr/cs/board/notice_under.do',                      seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '데이터과학과'), seoul_basic, '데이터과학과 공지사항', 'https://datascience.korea.ac.kr/ds/board/notice_under.do',              seed_date),
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '인공지능학과'), seoul_basic, '인공지능학과 공지사항', 'https://ai-dept.korea.ac.kr/ai-dept/board/notice_under.do',             seed_date),

            -- 디자인조형학부
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '디자인조형학부'), seoul_basic, '디자인조형학부 공지사항', 'https://and.korea.ac.kr/kuand/reference/notice.do',                    seed_date),

            -- 자유전공학부
            (ku_id, ku_seoul_id, (SELECT id FROM department WHERE campus_id = ku_seoul_id AND name = '자유전공학부'), seoul_basic, '자유전공학부 공지사항', 'https://sis.korea.ac.kr/sis/join/notice.do',                             seed_date);
    END
$$;
