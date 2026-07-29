# library-api

A Spring Boot REST backend built as a learning project — a small book-lending
library (Author, Book, Member, Loan). See [REQUIREMENTS.md](REQUIREMENTS.md)
for the full phased roadmap and what's done so far, and
[DEVELOPER.md](DEVELOPER.md) for the architecture and design decisions
behind the code.

## Stack

- Spring Boot 4.1 / Java 25, packaged as a WAR
- Spring Data JPA + PostgreSQL
- Flyway for schema migrations
- Spring Security (form-based login, session-based)
- Bean Validation

## Prerequisites

- PostgreSQL running locally, with a `library_db` database
- Set your Postgres password in `src/main/resources/application.properties`
  (`spring.datasource.password`)

## Running locally

```bash
./mvnw spring-boot:run
```

On startup, Flyway applies the schema migrations in
`src/main/resources/db/migration`.

A test user is configured in `application.properties`
(`spring.security.user.*`) for exercising the form-login endpoints
(`/login`, `/logout`) until real `Member`-backed authentication is added.

## Build

```bash
./mvnw package        # produces a WAR
./mvnw test            # run tests
./mvnw package -DskipTests
```

## Sample data

- `sample-data.sql` — run manually with `psql` to seed authors/books
  (`psql -h localhost -U postgres -d library_db -f sample-data.sql`)
- `sample-authors.json` / `sample-books.json` — example request bodies for
  manual testing in Postman
