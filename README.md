# sanavi ai-api

`backend-springboot`와 `ai-fastapi`(온프레미스) 사이에 있는 중계 서버. AI 분석 요청을 받아 FastAPI로 넘기고, 결과를 `ai_db`(MariaDB)에 저장·조회하는 역할만 전담함. 실제 LLM 추론은 하지 않음 — FastAPI가 장애나도 이 서버와 회원/게시판 같은 핵심 서비스는 안 죽게 막아주는 "장애 격리 벽" 역할이 핵심.

## 어떤 순서로 동작하나

1. `backend-springboot`가 로그인 유저 검증(구독 여부·ai_count) 후 `POST /api/analysis`로 요청 중계 (헤더 `X-User-Id`로 유저 식별, 비로그인은 헤더 없이도 호출 가능)
2. ai-api가 `ai-fastapi`에 분석 요청을 보내고, `taskId`를 받는 즉시 `ai_db`에 초기 레코드(`deleted=1`, `base_score=0`)를 만들고 그대로 반환
3. 클라이언트는 `GET /api/analysis/{taskId}`로 반복 조회 — ai-api는 먼저 `ai_db`에 이미 저장된 결과가 있는지 보고, 없으면 FastAPI를 폴링
4. FastAPI가 `COMPLETED`를 주면 그 순간 결과(승인율·체크리스트·주의사항·판례)를 `ai_db`에 저장(중복 저장 방지 포함)하고 클라이언트에 전달
5. Redis가 만료돼서 FastAPI가 결과를 못 주는 상황이면 `ai_db`에서 그대로 복원해서 응답 (`loadFromDb` 폴백)

FastAPI에 직접 쓰지 않는 이유: `main_db`(회원·게시판 등)와 `ai_db`(분석 결과)를 물리적으로 분리해서, AI 서버가 불안정해도 핵심 서비스 DB에는 영향이 안 가게 하려는 설계.

## 장애 격리 (Circuit Breaker)

`ai-fastapi`(온프레미스)는 GPU 연산 + LLM 응답 대기 때문에 응답이 느리고 장애 가능성도 높음. 여기 문제가 그대로 ai-api나 backend-springboot까지 전파되지 않도록 Resilience4j를 붙임.

- 최근 10건 중 50% 이상 실패 → `OPEN`(호출 자체를 즉시 차단하고 에러 반환)
- 30초 대기 → `HALF_OPEN`(요청 3건만 흘려보내 회복 여부 확인)
- 3건 연속 성공 → `CLOSED`(정상 복귀)
- `4xx`(클라이언트 입력 문제)는 실패로 안 세고, `5xx`·타임아웃·연결 끊김만 장애로 기록 (`RestClientConfig`, `application.yml`)

요청용(`fastApiRequestClient`)과 폴링용(`fastApiPollClient`) RestClient를 분리해뒀음 — 분석 요청은 최대 120초까지 기다려주지만, 폴링은 Redis 값만 읽는 거라 5초 넘게 걸리면 FastAPI 자체 문제로 보고 빠르게 실패시킴.

## 동기 / 비동기 모드 (`analysis.async`)

- `true`(기본값, 운영): `taskId`만 즉시 반환하고 스레드를 바로 반환 — 클라이언트가 폴링
- `false`(부하테스트 전용): FastAPI가 끝날 때까지 스레드를 2초 간격으로 블로킹 — Tomcat 스레드 풀이 고갈되는 걸 재현하기 위한 스위치. nGrinder로 동시 40명 부하를 주면 이 모드에서 TPS가 0으로 수렴하는 걸 실측 확인함 → 비동기로 가야 하는 근거를 수치로 남겨둔 것. 운영 환경에서는 절대 켜면 안 됨.

## API 엔드포인트

| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/analysis` | 분석 요청 중계 (`X-User-Id` 헤더로 유저 식별, 비로그인 가능) |
| GET | `/api/analysis/{taskId}` | 결과 조회 (DB 우선 조회 → 없으면 FastAPI 폴링) |
| POST | `/api/analysis/chat` | 분석 결과 기반 후속 질의 — DB 저장 없이 FastAPI에 그대로 중계(stateless) |
| GET | `/api/analysis/history/{userId}` | 유저별 분석 이력 최신순 조회 |
| DELETE | `/api/analysis/{taskId}?userId=` | 논리 삭제 (`deleted=0`), userId 불일치 시 404 |
| GET | `/api/admin/analysis` | 관리자 — 전체 분석이력 목록 (키워드/승인율 필터, 페이지네이션) |
| GET | `/api/admin/analysis/stats` | 관리자 — 전체/검색결과 평균 승인율 |
| GET | `/api/admin/analysis/trend?range=daily\|monthly` | 관리자 — 분석횟수 추이 |
| GET | `/api/admin/analysis/ranking?by=disease\|job&limit=` | 관리자 — 질병/직업 TOP 랭킹 |
| DELETE | `/api/admin/analysis/{taskId}` | 관리자 — 강제삭제, 소유자 무관 |
| POST | `/api/admin/analysis/count-for-users` | 관리자 — backend-springboot가 main_db로 추린 userId 목록을 받아 기간 내 분석건수만 집계 (다중조건 통계용 2단계 조회) |

> **주의**: `AdminAnalysisController`는 자체적으로 관리자 권한을 검사하지 않음 — `backend-springboot`의 관리자 프록시를 거쳐서만 호출되는 걸 전제로 함. 지금은 이 서버뿐 아니라 `backend-springboot`의 `SecurityConfig`도 `anyRequest().permitAll()` 상태라, role_admin 권한 규칙이 켜지기 전까지는 이 엔드포인트들이 사실상 누구나 호출 가능한 상태임.

## DB — `ai_db` (MariaDB, main_db와 물리적 FK 없음)

`analysis_result`(PK는 UUID인 `taskId`, `user_id`는 논리적 연결이라 FK 제약 없음) + `analysis_result_chat`/`_check`/`_warning`/`_metacontent` 4개 하위 테이블. 전 테이블 논리삭제(`deleted=1` 정상 / `0` 삭제) 원칙 동일 적용. 상세 컬럼은 워크스페이스 루트 `CLAUDE.md`의 "DB 스키마 > ai_db" 참고.

## 로깅

`LoggingAspect`(AOP) + `S3LogAppender`로 요청 로그를 S3에 NDJSON으로 적재. `MDCFilter`가 요청마다 `traceId`를 MDC에 심어서, 위 서비스 로직의 모든 로그(`FastAPI 분석 요청 완료`, `Circuit Breaker OPEN` 등)에 `traceId`가 같이 찍히게 함 — 장애 시 backend-springboot ↔ ai-api ↔ ai-fastapi 로그를 traceId 기준으로 엮어서 추적 가능.

## 실행 준비

- `.env`에 `AI_DB_HOST` / `AI_DB_USERNAME` / `AI_DB_PASSWORD`(ai_db 접속 정보), `FASTAPI_URL`(기본 `http://localhost:8000`) 필요
- `REDIS_HOST` / `REDIS_PORT`는 옵션(기본 `localhost:6379`) — RefreshToken 저장용이라 이 값이 없어도 분석 API 자체는 정상 동작함
- 로컬에서 FastAPI(`ai-fastapi`)가 떠있어야 분석 요청이 실제로 처리됨 (안 떠있으면 Circuit Breaker가 곧 OPEN으로 전환)

```bash
./gradlew bootRun
# 또는
./gradlew bootJar && java -jar build/libs/*.jar
```

기본 포트는 `8081`.

## 배포

`Dockerfile`(멀티스테이지: Gradle 빌드 → JRE 실행)로 이미지를 만들고, `main` 브랜치에 푸시되면 GitHub Actions(`.github/workflows/docker-publish.yml`)가 Docker Hub(`umleeho/sanavi-ai-api`)에 `latest` + 커밋 해시 태그로 자동 푸시함. EC2 쪽은 Watchtower가 2분 주기로 이미지 변경을 감지해 자동 재기동 — SSH로 직접 배포하지 않음.
