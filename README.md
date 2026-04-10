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
