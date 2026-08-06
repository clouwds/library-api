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
- [x] `GET /books` supports query params: filter by author, genre, publication-year range; plus pagination (`page`, `size`) and sorting (`sort`)
- [x] `POST /books` returns `201 Created` with a `Location` header pointing at the new resource
- [x] `GET /books` sets a custom response header (e.g. `X-Total-Count`) with the total match count
- [x] At least one endpoint reads a custom request header (e.g. `X-Client-Id`) and reflects/logs it
- [x] Bean Validation on all request bodies (`@Valid` + constraint annotations); invalid input returns `400` with field-level error messages
- [x] Global error handling via `@RestControllerAdvice` returning `ProblemDetail` (RFC 7807) for validation errors, 404s (not found), and 409s (conflicts)
- [x] Request/response DTOs — controllers never expose JPA entities directly

## Phase 2 — Persistence depth

- [x] `Member` entity (a library member/borrower)
- [x] `Loan` entity linking `Member` and `Book`, with borrow date / due date / return date
- [x] Derived-query repository methods on `LoanRepository`:
  - [x] `findByReturnDateIsNull()`
  - [x] `findByMemberId(Long memberId)`
  - [x] `findByDueDateBeforeAndReturnDateIsNull(LocalDate date)`
- [x] Custom `@Query` (JPQL or native), e.g.:
  - [x] Count of currently-borrowed books per member
  - [x] Most-borrowed book titles overall
  - [x] Books currently on loan by a given author (join `Loan` → `Book` → `Author`)
- [x] `GET /loans/overdue`
- [x] `GET /members/{id}/loans`
- [x] A `@Transactional` "borrow a book" operation that checks availability and updates state atomically (no double-borrowing)
- [x] Replace `ddl-auto=update` with Flyway migrations

## Phase 3 — Session-based security

- [x] Add Spring Security; form-based login/logout with `HttpSession`:
  - [x] Custom `SecurityConfig` (`@Configuration` + `SecurityFilterChain` bean) replacing the property-based zero-config defaults
  - [x] `authorizeHttpRequests(...)` explicitly deciding which endpoints are public vs require authentication (everything currently requires auth by default)
  - [x] `.formLogin(...)` configured explicitly, with a sensible `defaultSuccessUrl` (default redirect target `/` 404s, since this API has no root page)
  - [x] `.logout(...)` configured explicitly, confirming `/logout` invalidates the `HttpSession`
  - [x] Verify the full session lifecycle: login sets a session cookie, a protected endpoint is reachable while the session is valid, logout invalidates it and the same endpoint requires login again
- [x] `Member` (or a separate `User`) entity has a role: `MEMBER` / `LIBRARIAN`:
  - [x] `Role` enum (`MEMBER`/`LIBRARIAN`) added to `Member`, mapped via `@Enumerated(EnumType.STRING)`
  - [x] `Member` needs an actual login identifier to be authenticatable against — `firstName`/`lastName` can collide and aren't meant for login; add a unique field (`email` or `username`) plus a `password` field
- [x] Passwords hashed with BCrypt:
  - [x] `PasswordEncoder` bean in `SecurityConfig` (declare and inject as the `PasswordEncoder` interface, backed by `BCryptPasswordEncoder` — keeps the algorithm swappable without touching every consumer)
  - [x] Hash passwords everywhere a `Member`'s password is set (create, update, patch) before persisting — never store it plain
  - [x] Never expose `password` in `MemberResponse` (or any response DTO)
- [x] Custom `UserDetailsService`/`AuthenticationProvider` backed by `Member`, replacing the property-based test user so real login authenticates against the database:
  - [x] `UserDetailsService` implementation that loads a `Member` by its login identifier via `MemberRepository`
  - [x] Map `Member.role` to a Spring Security `GrantedAuthority` (`ROLE_MEMBER`/`ROLE_LIBRARIAN`)
  - [x] Wire it into `SecurityConfig` so the `AuthenticationManager` actually uses it
  - [x] Remove `spring.security.user.*` from `application.properties` once real login against the `Member` table works
  - [x] Verify: login with real `Member` credentials from the database succeeds; the old property-based test user credentials no longer work
- [x] CSRF protection left enabled; demonstrate the token round-trip (e.g. a documented curl sequence or a test)
- [x] Method-level security: only `LIBRARIAN` can `DELETE /books/{id}`, only the owning `MEMBER` (or a `LIBRARIAN`) can see their own loans (depends on the DB-backed roles above actually being in effect)

## Phase 4 — Stateless JWT security

- [x] Add a JWT library (e.g. `jjwt`) and a signing key/secret in `application.properties` (not committed in plaintext for a real project, but fine to note the tradeoff here)
- [x] `POST /auth/login` issuing a JWT access token:
  - [x] Accepts email/password, authenticates via the existing `AuthenticationProvider`/`AuthenticationManager` (reuse it — don't re-implement credential checking)
  - [x] On success, issue a signed access token with claims: subject (member id or email), role, issued-at, short expiry (e.g. 15 min)
  - [x] Response body returns token and token type (e.g. {"accessToken": "...", "tokenType": "..."})
  - [x] Verify: `POST /auth/login` with valid `Member` credentials returns an access token; wrong credentials return `401`
- [x] `SessionCreationPolicy.STATELESS`; custom JWT authentication filter:
  - [x] `.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))` in `SecurityConfig`
  - [x] Decide: does JWT auth replace the Phase 3 session/form-login setup entirely, or coexist with it? Document the choice
  - [x] A custom `OncePerRequestFilter` (e.g. `JwtAuthenticationFilter`) that reads the `Authorization: Bearer <token>` header, validates signature + expiry, and builds an `Authentication` (reusing `MemberPrincipal` or similar) placed into `SecurityContextHolder`
  - [x] Register the filter in the chain before `UsernamePasswordAuthenticationFilter`
  - [x] Verify: existing `@PreAuthorize` checks (owning-member-or-librarian, `hasRole('LIBRARIAN')`) still work unchanged against JWT-derived authentication — they shouldn't need to know or care where the `Authentication` came from
- [x] Decide and document token storage (`Authorization` header vs httpOnly cookie) and why — weigh XSS exposure (header/localStorage on a real frontend) against reintroducing CSRF concerns (cookie)
- [ ] `POST /auth/refresh` to get a new access token, plus issuing the refresh token itself at login:
  - [ ] Issue a separate, longer-lived refresh token at login (either a JWT with its own expiry, or an opaque token persisted server-side — decide and note why)
  - [ ] Accepts the refresh token, validates it (signature/expiry, plus a lookup against the persisted store if using opaque tokens)
  - [ ] Issues a new access token
  - [ ] Decide: does the refresh token rotate (single-use, reissued each time — more secure) or stay valid until its own expiry (simpler)? Document the choice
  - [ ] Verify: `POST /auth/login` returns both tokens; an expired or tampered refresh token sent to `/auth/refresh` is rejected; a valid one returns a fresh access token
- [ ] `POST /auth/logout` (token invalidation strategy — short expiry vs blacklist):
  - [ ] Since JWTs are stateless, logout can't truly invalidate an already-issued, still-valid token unless you track revocation somewhere
  - [ ] Decide between: (a) rely on short access-token expiry and simply stop honoring the refresh token (no extra state), or (b) maintain a revocation list (DB/Redis) of revoked token IDs (`jti` claim) checked per request (adds back some state, but is the realistic production approach)
  - [ ] Document why you picked one over the other for this project
  - [ ] Verify: after logout, the old access token (if using approach b) or the refresh token (approach a) can no longer be used

## Phase 5 — Cross-cutting concerns

- [ ] Custom error page/handler so unmapped routes and errors don't fall through to the Whitelabel Error Page:
  - [ ] A custom browser-facing (HTML) error page — e.g. a static `error/4xx.html`/`error/5xx.html` under Spring Boot's default error-page resource location, or a custom `ErrorController` — so a browser hitting an unmapped route (or the login flow ending up somewhere unexpected) shows a clean page, not the Whitelabel default
  - [ ] Confirm API clients (`Accept: application/json`) still get a structured JSON error body for the same unmapped-route/error cases, not HTML — verify Spring Boot's content-negotiation-aware default `/error` behavior, extending `HttpExceptionHandler` only if it doesn't already do this
  - [ ] A catch-all `@ExceptionHandler(Exception.class)` in `HttpExceptionHandler` for genuinely unexpected/unhandled exceptions, returning a generic `500` `ProblemDetail` instead of leaking a stack trace or falling through to the Whitelabel page
  - [ ] Verify both paths: `GET` an unmapped route in a browser → custom page, not Whitelabel; same request via curl/Postman with `Accept: application/json` → clean JSON, not HTML
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
