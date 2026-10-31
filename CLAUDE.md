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
- **Async email**: `DailyContentEmailPublisher` publishes to RabbitMQ; `DailyContentEmailConsumer` consumes and sends via `MailService` (Gmail SMTP).
- **Schedulers**: `DailyContentScheduler`, `DailyContentIndexScheduler` — check `@Scheduled` usage.

## Infrastructure

- **MySQL** (JPA, `hibernate.dialect=MySQL8Dialect`, `ddl-auto=update`)
- **Redis** (Spring Data Redis)
- **RabbitMQ** (Spring AMQP, retry config in `application.properties` + `RetryConfig.kt`)
- **Gmail SMTP** (Spring Mail, port 587, STARTTLS)

All connection params are environment variables (see `application.properties`). No defaults — app fails fast without them.

## Tests

- Integration tests use Testcontainers (MySQL). Base support class: `IntegrationTestSupport`.
- Study tests in `src/test/kotlin/maeilkanji/maeilkanji/study/Study.kt` are disabled (marked `@Disabled` per commit `b44b452`).
- `MaeilKanjiApplicationTests` — context load test.

## Env vars required to run

`MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE`, `MYSQL_USER`, `MYSQL_PASSWORD`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `SERVICE_BASE_URL`, `ADMIN_EMAIL`.
