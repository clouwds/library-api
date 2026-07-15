# Library API — Learning Project Requirements

Goal: a Spring Boot REST backend, simple enough to finish, that deliberately
exercises every core Spring REST/Security/Ops concept. Work through the
phases in order — each one assumes the previous is done. Domain: a small
book-lending library (Author, Book, Member, Loan).

Stack: Spring Boot, Java, Maven, PostgreSQL (`localhost:5432`).

---

## Phase 0 — Setup

- [x] New Spring Boot project (Spring Initializr: Web, Data JPA, PostgreSQL Driver, Validation)
- [x] `application.properties` pointing at a local Postgres DB
- [x] `Author` and `Book` entities (`Book` has a many-to-one to `Author`)
- [x] App starts, Hibernate creates the schema (`ddl-auto=update` for now)

## Phase 1 — Core CRUD & REST mechanics

- [x] Full CRUD for `Book`: `GET /books/{id}`, `GET /books`, `POST /books`, `PUT /books/{id}`, `PATCH /books/{id}`, `DELETE /books/{id}`
- [x] Full CRUD for `Author`
- [ ] `GET /books` supports query params: filter by author, genre, publication-year range; plus pagination (`page`, `size`) and sorting (`sort`)
- [x] `POST /books` returns `201 Created` with a `Location` header pointing at the new resource
- [x] `GET /books` sets a custom response header (e.g. `X-Total-Count`) with the total match count
- [x] At least one endpoint reads a custom request header (e.g. `X-Client-Id`) and reflects/logs it
- [x] Bean Validation on all request bodies (`@Valid` + constraint annotations); invalid input returns `400` with field-level error messages
- [x] Global error handling via `@RestControllerAdvice` returning `ProblemDetail` (RFC 7807) for validation errors, 404s (not found), and 409s (conflicts)
- [x] Request/response DTOs — controllers never expose JPA entities directly

## Phase 2 — Persistence depth

- [ ] `Member` entity (a library member/borrower)
- [ ] `Loan` entity linking `Member` and `Book`, with borrow date / due date / return date
- [ ] At least one derived-query repository method (e.g. `findByReturnDateIsNull`)
- [ ] At least one custom `@Query` (JPQL or native)
- [ ] A `@Transactional` "borrow a book" operation that checks availability and updates state atomically (no double-borrowing)
- [ ] Replace `ddl-auto=update` with Flyway migrations

## Phase 3 — Session-based security

- [ ] Add Spring Security; form-based login/logout with `HttpSession`
- [ ] `Member` (or a separate `User`) entity has a role: `MEMBER` / `LIBRARIAN`
- [ ] Passwords hashed with BCrypt
- [ ] CSRF protection left enabled; demonstrate the token round-trip (e.g. a documented curl sequence or a test)
- [ ] Method-level security: only `LIBRARIAN` can `DELETE /books/{id}`, only the owning `MEMBER` (or a `LIBRARIAN`) can see their own loans

## Phase 4 — Stateless JWT security

- [ ] `POST /auth/login` issuing a JWT access token (+ refresh token)
- [ ] `SessionCreationPolicy.STATELESS`; custom JWT authentication filter
- [ ] `POST /auth/refresh` to get a new access token
- [ ] Decide and document token storage (Authorization header vs httpOnly cookie) and why
- [ ] `POST /auth/logout` (token invalidation strategy — short expiry vs blacklist)

## Phase 5 — Cross-cutting concerns

- [ ] `@Aspect` that logs execution time of service-layer methods
- [ ] Spring Boot Actuator enabled (`health`, `info`, `metrics`); non-trivial endpoints secured/restricted to `LIBRARIAN`
- [ ] CORS configured for a specific allowed origin (simulate a frontend)
- [ ] Security response headers configured explicitly: CSP, `X-Frame-Options`, HSTS, etc. via Spring Security's headers DSL
- [ ] OpenAPI/Swagger UI wired up (springdoc-openapi) documenting all endpoints

## Phase 6 — Stretch (optional)

- [ ] `MockMvc`/`WebTestClient` tests for controllers
- [ ] Testcontainers-backed integration tests against real Postgres
- [ ] `docker-compose.yml` for app + Postgres
- [ ] Response caching (`@Cacheable`) on a read-heavy endpoint
- [ ] Simple rate limiting on the auth endpoints
