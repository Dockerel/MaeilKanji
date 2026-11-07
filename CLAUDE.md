# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
./gradlew build          # compile + test
./gradlew test           # run all tests (Testcontainers spins up MySQL)
./gradlew test --tests "maeilkanji.maeilkanji.study.Study"   # single test class
./gradlew bootRun        # run app (requires MYSQL_HOST, REDIS_HOST, RABBITMQ_HOST, SMTP_* env vars)
```

## Architecture

Spring Boot 3 + Kotlin + JPA. Layered package structure under `maeilkanji.maeilkanji`:

```
presentation/     → controllers (REST @RestController, Thymeleaf view @Controller), ApiControllerAdvice
business/         → service, scheduler, publisher/consumer (RabbitMQ), domain enums
infra/            → JPA entities, JpaRepository interfaces, repository implementations, mappers, MailServiceImpl
```

- **Repository pattern**: `business/repository/*.kt` defines interfaces; `infra/repository/*RepositoryImpl.kt` implements them using JPA. `infra/repository/*JpaRepository.kt` are Spring Data JPA interfaces.
- **DTO flow**: request DTOs in `presentation/controller/api/request/`, service request/response types in `business/service/request/` and `business/service/response/`, service-level DTOs in `business/service/dto/`.
- **Async email**: `DailyContentEmailPublisher` publishes to RabbitMQ; `DailyContentEmailConsumer` consumes and sends via `MailService` (Resend SMTP, sender `MAIL_FROM`).
- **Schedulers**: `DailyContentScheduler`, `DailyContentIndexScheduler` — check `@Scheduled` usage.
- **Auto content generation**: `DailyContentGenerationScheduler` (00:00 KST) → `DailyContentGenerationService` creates one new question per level via `DailyContentGenerator` (`infra/service/ClaudeDailyContentGenerator`, Claude API `claude-opus-5-5`, JSON structured output). System prompt `resources/prompt/daily_content_generation.md` mirrors `.claude/commands/create_question.md` — keep both in sync. `GeneratedContentValidator` checks rules + duplicate kanji pairs; failures retry up to 3× with the reason fed back. Skipped when `ANTHROPIC_API_KEY` is unset.
- **Gamification** (no login): mail links carry `?memberId=`; `POST /api/v1/members/streak` and `GET /me?memberId=` issue the `MK_MEMBER` cookie (`MemberSessionCookie`), which identifies the member for `/me`, `/me/edit`, `PUT /api/v1/profile/me` and "my rank" on `/ranking`. Public profiles live at `/u/{nickname}`.
  - `study_day` table = one row per study date (heatmap, 누적 학습일). Badges (`Badge`) are computed from stats, not stored. Avatars/themes (`ProfileDecoration.kt`) unlock by `member.maxStreak`.
  - `RankingService` ranks all members in memory (current streak uses `StreakPolicy.current`, which is 0 once a day is skipped).

## Infrastructure

- **MySQL** (JPA, `hibernate.dialect=MySQL8Dialect`, `ddl-auto=update`)
- **Redis** (Spring Data Redis)
- **RabbitMQ** (Spring AMQP, retry config in `application.properties` + `RetryConfig.kt`)
- **Resend SMTP** (Spring Mail, `smtp.resend.com:587`, STARTTLS; username `resend`, password = API key)

All connection params are environment variables (see `application.properties`). No defaults — app fails fast without them.

## Tests

- Integration tests use Testcontainers (MySQL). Base support class: `IntegrationTestSupport`.
- Study tests in `src/test/kotlin/maeilkanji/maeilkanji/study/Study.kt` are disabled (marked `@Disabled` per commit `b44b452`).
- `MaeilKanjiApplicationTests` — context load test.
- Service unit tests (`business/service/*Test.kt`): Kotest `BehaviorSpec` + MockK, `IsolationMode.InstancePerLeaf` so each leaf gets fresh mocks.
- REST controller slice tests (`presentation/controller/api/*Test.kt`): `@WebMvcTest` + Kotest Spring extension. Service mock beans come from a nested `@TestConfiguration` (no springmockk). Constructor injection is enabled by `io.kotest.provided.ProjectConfig`.
- Kotest is pinned to 5.9.x because Kotest 6 requires Kotlin 2.x.
- Shared test data lives in `src/test/kotlin/maeilkanji/maeilkanji/fixture/Fixtures.kt`.

## Env vars required to run

`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `MAIL_FROM`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `SERVICE_BASE_URL`, `ADMIN_EMAIL`.

Optional: `ANTHROPIC_API_KEY` (enables midnight question generation).
