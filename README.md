# CampusNavi Backend

대학생들을 위한 AI기반 맞춤정보 제공 서비스 - 백엔드

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Build | Gradle (Kotlin DSL) |
| Database | PostgreSQL |
| Cache | Redis |
| Auth | Spring Security + JWT |
| Migration | Flyway |
| Docs | SpringDoc OpenAPI (Swagger) |

## 주요 기능

- **공식정보 수집**: 스케줄러 기반 대학 공식 정보 자동 크롤링, 관리자 API로 수동 크롤링 
- **공식정보 가공**: 크롤링 Commit Event 발생시 AI 메타 정보 생성, 수동 크롤링시 이벤트 미발생 -> 관리자 API로 배치 생성
- **회원 관리**: JWT 기반 인증, Admin 자동 부트스트랩
- **커뮤니티**: 게시판, 댓글 기능
- **태그**: 회원 관심사와 공식정보 및 커뮤니티 연결
- **대학 정보**: 캠퍼스, 학부 정보 관리

## 프로젝트 구조

```
src/main/java/com/campusnavi/backend/
├── auth/              - JWT 기반 인증, Spring Security
├── member/            - 사용자 계정 관리
├── university/        - 대학, 캠퍼스, 학부 정보
├── official/          - 공식정보 크롤링, AI 메타 처리
├── community/         - 커뮤니티 게시판, 댓글
├── tag/               - 태그 관리
├── infra/             - S3, 크롤링, FastAPI 연동
└── global/            - 공통 설정, 예외 처리, 초기화
```

## 로컬 환경 실행

### 사전 요구 사항

- Java 21
- Docker & Docker Compose

### 1. 환경 변수 설정

```bash
cp .env.example .env
# .env 에 환경변수 입력

cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
# application-local.yml 에 환경변수 입력
```

#### Admin 초기 계정 설정 (선택사항)

애플리케이션 시작 시 자동으로 Admin 계정을 생성합니다.

`.env` 파일에 다음 변수들을 설정하면 해당 정보로 Admin이 생성됩니다:

```bash
ADMIN_BOOTSTRAP_ENABLED=true        # 로컬: true (기본), 프로덕션: false
ADMIN_USERNAME=admin                # Admin 로그인 username
ADMIN_PASSWORD=password123          # Admin 로그인 password (평문, 앱 시작 시 인코딩됨)
ADMIN_EMAIL=admin@example.com       # Admin 이메일
ADMIN_NICKNAME=관리자               # Admin 닉네임
ADMIN_UNIVERSITY_ID=1               # Admin이 속한 대학 ID (DB에 존재해야 함)
```

- 대학교에 Admin이 이미 존재하면 재생성하지 않습니다.

### 2. 인프라 실행 (DB, Redis)

```bash
docker-compose up -d
```

### 3. 애플리케이션 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## API 명세

애플리케이션 실행 후 아래 주소에서 확인

- Swagger UI: `http://localhost:8080/swagger-ui`
- API Docs: `http://localhost:8080/api-docs`

## 개발 가이드

### 데이터베이스 마이그레이션

Flyway를 사용하여 관리됩니다. 새로운 마이그레이션 파일은 `src/main/resources/db/migration/` 디렉토리에 `V<version>__<description>.sql` 형식으로 추가합니다.

- 예시: `V00__add_new_column.sql`
- 자동 실행: 애플리케이션 시작 시 대기 중인 마이그레이션이 자동으로 실행됩니다.
