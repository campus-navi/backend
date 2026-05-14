# CampusNavi Backend

대학생들을 위한 AI 기반 맞춤정보 제공 서비스 - 백엔드

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle (Kotlin DSL) |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM / Query | Spring Data JPA, QueryDSL |
| Migration | Flyway |
| Auth | Spring Security + JWT (jjwt) |
| Storage | AWS S3 (운영) / MinIO (로컬) |
| Mail | Gmail SMTP |
| Crawling | Jsoup |
| HTTP Client | Apache HttpClient 5 (FastAPI 연동) |
| Docs | SpringDoc OpenAPI (Swagger) |
| Test | JUnit 5, Mockito, Testcontainers (PostgreSQL) |
| Timezone | JVM/Jackson 모두 Asia/Seoul 고정 |

## 주요 기능

- **공식정보 수집**: Jsoup 기반 스케줄러 크롤링 + 어드민 시드 크롤링 API, 실패 기록 및 재시도 스케줄러
- **공식정보 AI 메타 처리**: 크롤링 커밋 이벤트로 FastAPI 비동기 호출, 수동 수집분은 어드민 배치 API로 처리
- **피드**: 메인화면 카드뉴스(최신 9 + 추천 5), 마감임박 공지 미리보기/전체 조회
- **추천**: 회원 관심사·학적 기반 스코어링, 추천 스냅샷 사전 계산 스케줄러
- **공식정보 상세**: 본문/첨부 이미지 리스트, 스크랩, 첨부파일 다운로드 추적, 알림 토글
- **회원 관리**: JWT 인증, Admin 자동 부트스트랩, 내 정보 조회
- **커뮤니티**: 게시판, 댓글, 좋아요, 스크랩
- **태그**: 회원 관심사·공식정보·커뮤니티 공통 태깅, FastAPI 내부용 조회 API
- **대학 정보**: 대학·캠퍼스·단과대·학부 계층 관리

## 프로젝트 구조

```
src/main/java/com/campusnavi/backend/
├── auth/              - JWT 발급/검증, 로그인·로그아웃
├── member/            - 회원 계정, 관심사, 학적 정보
├── university/        - 대학·캠퍼스·단과대·학부 계층
├── official/
│   ├── source/        - 크롤링 대상 공식 소스 정의
│   ├── crawler/       - Jsoup 크롤러, 스케줄러, 실패 기록·재시도
│   ├── ai/            - FastAPI 연동 AI 메타 처리, 이벤트 리스너, 재시도 스케줄러
│   └── post/          - 공식정보 게시글/스크랩/첨부 다운로드/알림 토글 + 추천 스냅샷
├── feed/              - 메인 카드뉴스·마감임박 공지 피드
├── community/         - 커뮤니티 게시판, 댓글
├── tag/               - 태그 관리, FastAPI 내부용 API
├── infra/
│   ├── ai/            - FastAPI HTTP 클라이언트, 헬스체크
│   ├── storage/       - S3/MinIO 업로드·Presigned URL
│   ├── email/         - SMTP/SES 메일 발송
│   └── redis/         - Redis 설정
└── global/            - 공통 설정, 보안, 예외, 응답, 초기화(Admin Bootstrap)
```

데이터베이스 마이그레이션은 `src/main/resources/db/migration/` 에 Flyway 규칙으로 관리합니다.

## 로컬 환경 실행

### 사전 요구 사항

- Java 21
- Docker & Docker Compose

### 1. 환경 변수 설정

```bash
cp .env.example .env
# .env 에 환경변수 입력

cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# 필요 시 application-local.yml 의 값을 조정
```

#### Admin 초기 계정 설정 (선택사항)

애플리케이션 시작 시 자동으로 Admin 계정을 생성합니다.

`.env` 파일에 다음 변수들을 설정하면 해당 정보로 Admin이 생성됩니다.

```bash
ADMIN_BOOTSTRAP_ENABLED=true        # 로컬: true (기본), 프로덕션: false
ADMIN_USERNAME=admin                # Admin 로그인 username
ADMIN_PASSWORD=password123          # Admin 로그인 password (평문, 앱 시작 시 인코딩됨)
ADMIN_EMAIL=admin@example.com       # Admin 이메일
ADMIN_NICKNAME=관리자               # Admin 닉네임
ADMIN_UNIVERSITY_ID=1               # Admin이 속한 대학 ID (DB에 존재해야 함)
```

- 해당 대학교에 Admin이 이미 존재하면 재생성하지 않습니다.

### 2. 인프라 실행 (PostgreSQL · Redis · MinIO)

```bash
docker-compose up -d
```

- `postgres` (5432): 애플리케이션 RDB
- `redis` (6379): 추천 스냅샷 슬롯 등 캐시
- `minio` (9000) / 콘솔 (9001): 로컬 S3 호환 스토리지, `campus-navi-local` 버킷 자동 생성

### 3. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## API 명세

애플리케이션 실행 후 아래 주소에서 확인할 수 있습니다.

- Swagger UI: `http://localhost:8080/api/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/api/api-docs`

## 개발 가이드

### 데이터베이스 마이그레이션

Flyway로 관리됩니다. 새로운 마이그레이션 파일은 `src/main/resources/db/migration/` 디렉토리에 `V<version>__<description>.sql` 형식으로 추가합니다.

- 예시: `V37__add_new_column.sql`
- 자동 실행: 애플리케이션 시작 시 대기 중인 마이그레이션이 자동으로 실행됩니다.

### 테스트

```bash
./gradlew test
```

- 리포지토리 슬라이스 테스트는 Testcontainers 기반 PostgreSQL을 사용합니다. Docker 데몬이 실행 중이어야 합니다.
- 테스트 JVM 타임존은 `Asia/Seoul` 로 고정되어 있습니다.

### 추천 알고리즘 설정

`application.yml` 의 `recommend.feed` 하위 값으로 가중치, 후보·결과 수, 최신성 윈도우 등을 조정할 수 있습니다.

```yaml
recommend:
  feed:
    w1: 0.30             # 학과 평균 조회수 가중치
    w2: 0.30             # 조회자 중 학번/학년 매칭 가중치
    w3: 0.40             # 관심새(태그) 매칭 가중치
    w2-admission: 0.4    # 학적 매칭 중 소속 비중
    w2-grade: 0.6        # 학적 매칭 중 학년 비중
    view-cap: 30         # w1계산시 최대 조회수
    candidate-limit: 200 # 후보군 limit
    result-limit: 5      # 추천탭 limit
    freshness-days: 14   # 날짜 기준 필터링(신선도)
```