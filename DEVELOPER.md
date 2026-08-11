# Developer Documentation

This document explains how `library-api` is put together and *why* — the
design decisions behind the code, not just what the code does. For "how do
I run this," see [README.md](README.md). For the phased learning roadmap,
see [REQUIREMENTS.md](REQUIREMENTS.md).

## Package structure

```
config/          SecurityConfig
controller/      REST endpoints — thin, no business logic
service/         Business logic, transaction boundaries, entity<->DTO mapping
repository/      Spring Data JPA repositories
specification/   JPA Specification builders for dynamic query filters
model/           JPA entities
dto/             Request/response contracts — never the same type as an entity
exception/       Custom exceptions + the global @RestControllerAdvice handler
```

Each package has exactly one job. `dto` is split out from `model`
specifically so the API contract can diverge from the persistence model —
see below.

One controller placement worth knowing since it's not obvious from the URL
alone: `GET /members/{id}/loans` lives in `LoanController`, not
`MemberController`, even though the path is nested under `/members`. It
returns `Loan`-shaped data through `LoanService`, the same as every other
endpoint in that controller — keeping it there means one controller maps
to one response shape and one service, rather than `MemberController`
needing to depend on `LoanService` too (and every future "loans nested
under X" endpoint forcing the same dependency onto yet another
controller). URL-path-matches-controller-class is a weaker convention than
keeping one controller's endpoints cohesive around a single resource type.

## Domain model

`Author` ← `Book` ← `Loan` → `Member`. Relationships are deliberately
**unidirectional** (`Book` has an `Author`, but `Author` has no `List<Book>`;
`Loan` has a `Book` and a `Member`, neither has a `List<Loan>`). Two reasons:
we never needed the reverse navigation, and a bidirectional relationship on
an entity that's ever serialized directly (a mistake this project
specifically avoids — see below) risks infinite JSON recursion.

All entities use `@GeneratedValue(strategy = GenerationType.SEQUENCE)`, not
`IDENTITY`. On Postgres, `SEQUENCE` lets Hibernate pre-allocate a block of
IDs (`nextval()` before the `INSERT`) instead of having to insert first and
read back an auto-increment value — more efficient, and it enables Hibernate
to batch inserts. `IDENTITY` doesn't support batching at all.

`Book.version` (`@Version`) backs optimistic locking for the borrow
operation — see "Concurrency" below.

Duplicate-book detection (`POST /books` rejecting a repeat ISBN) is done via
`BookRepository.existsByIsbn`, a direct DB query — **not** via
`Book.equals()`/`hashCode()`. An earlier version of `Book` did override both
(all-fields equality) specifically to support this; it was removed once
`existsByIsbn` replaced it, since entity `equals()`/`hashCode()` based on
mutable fields is a known JPA footgun (breaks if the object's fields change
after being placed in a `Set`/used as a map key), and checking one column
via SQL is both simpler and doesn't require loading every row into memory
to compare.

## DTOs, never entities

Every controller returns a DTO (`BookResponse`, `AuthorResponse`, etc.),
never a JPA entity. Reasons this is enforced everywhere, not just where
convenient:
- **API contract stability** — the entity's shape can change (a new column,
  a renamed field) without silently changing what clients receive.
- **No mass assignment** — a request DTO only exposes the fields a client is
  actually allowed to set. `BookRequest` has no `id` field at all, so a
  client can't even attempt to set one.
- **Framework details don't leak** — entities carry JPA proxies, lazy
  collections, and (before this was fixed) things like `Book.equals()`
  being based on mutable fields. None of that belongs in an HTTP response.

Create/update DTOs (`BookRequest`) are shared between `POST` and `PUT`,
since both send the full resource and validate identically. `PATCH` DTOs
(`BookPatchRequest`) are separate — every field is nullable with no
`@NotBlank`/`@NotNull`, since only fields actually present in the request
should be applied; a shared DTO would force PATCH to either drop validation
or write conditional-validation logic, both worse than one extra small
class.

Relations are referenced by id (`BookRequest.authorId: Long`,
`LoanRequest.memberId`/`bookId`), never by a nested object
(`BookRequest.author: Author`, which is what an earlier version did). The
distinction that matters: *creating* a new entity legitimately has no id
yet (the server assigns it), but *referencing* an existing entity from
another resource is a different operation entirely — the client needs to
say which existing row it means, and an id is the only reliable way to say
that. A nested object also invited a real bug: `Author` only exposed
`setName()`, no `setId()`, so a nested `author` in a `Book` payload could
never actually bind an id at all.

## REST conventions

- **`PUT` vs `PATCH`**: `PUT` replaces the full resource (all fields
  required); `PATCH` only touches fields present in the request body.
- **`Location` header**: every `POST` that creates a resource returns `201`
  with `Location` pointing at it, built via
  `ServletUriComponentsBuilder.fromCurrentRequest()`. This only applies to
  endpoints that create an addressable resource — an action/command
  endpoint (e.g. a future `POST /auth/login`) wouldn't get one.
- **Filtering**: built via `Specification<Book>` (see `BookSpecifications`),
  composed conditionally with `.and(...)` per query param present —
  `Specification.unrestricted()` is the empty starting point (this is the
  Spring Data JPA 4.x replacement for the older `where(null)` idiom, which
  now throws on `null`).
- **Sorting**: `sort=field,direction` is parsed against a **whitelist**
  (`BookService.SORTABLE_FIELDS`) mapping a public-facing name to the real
  JPA property path (`authorName` → `author.name`) — never passes raw
  client input straight into `Sort.by(...)`. An unrecognized field throws
  `InvalidRequestException` → `400`, not a silent fallback.
- **Pagination**: `page`/`size` build a `Pageable`, but the raw
  `Page<T>`/`PageImpl` is never returned directly — Spring itself warns
  `PageImpl`'s JSON shape isn't guaranteed stable across versions. Responses
  go through `PagedResponse<T>` (`content`, `page`, `size`, `totalElements`,
  `totalPages`) instead.
- **`@Valid` lives on the controller parameter, never the service method.**
  Spring MVC only wires up its automatic `400` + field-level error response
  (`MethodArgumentNotValidException`) for `@Valid` on a controller
  parameter — putting it on a service method does nothing without also
  adding `@Validated` at the class level and accepting a different
  exception type (`ConstraintViolationException`) that isn't mapped to
  `400` by default. Once validated at the controller boundary, the service
  layer can trust the DTO's shape is already correct and focus purely on
  business-rule checks Bean Validation can't express (an ISBN not already
  taken, a referenced author actually existing).
- **`PATCH /loans/{id}/return` is an action endpoint, not a classic partial
  update.** It takes no request body at all — compare to
  `BookPatchRequest`, which sends only the fields to overwrite. `PATCH`
  still fits semantically (it modifies part of an existing resource's state
  without replacing it), it's just triggered by the URL/verb rather than by
  body content — "mark this loan returned" is a state transition, not a
  field-by-field edit.

## Exception handling

`HttpExceptionHandler` (`@RestControllerAdvice`) centralizes every error
response as a `ProblemDetail` (RFC 7807):

| Exception | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `ConflictException` | 409 |
| `InvalidRequestException` | 400 |
| `MethodArgumentNotValidException` (Bean Validation) | 400 |

Every service method that loads an entity by id uses `orElseThrow(...)`
into one of these — never `orElse(null)` followed by a silent no-op, which
was an actual bug fixed during Phase 1 review (an update/patch to a
nonexistent id used to return `200` having done nothing).

Every `delete` method checks `existsById(id)` and throws
`ResourceNotFoundException` **before** calling `deleteById(id)`, rather than
just calling `deleteById` directly. `deleteById` on a nonexistent id throws
Spring Data's `EmptyResultDataAccessException`, which isn't mapped by
`HttpExceptionHandler` — left unchecked, deleting something that's already
gone would surface as an unhandled `500` instead of a clean `404`.

**Unmapped routes (a genuine 404) are static HTML/JSON, not a custom
`ErrorController`.** `src/main/resources/static/error/4xx.html` and
`5xx.html` are enough on their own — Spring Boot's auto-configured
`BasicErrorController` already resolves `error/<status>`/`error/<4xx|5xx>`
by convention and already content-negotiates correctly (`Accept: text/html`
gets the static page, `Accept: application/json` gets a structured JSON
body), with zero Java code. A custom `ErrorController` would only earn its
keep for genuinely dynamic error content, which this project doesn't need
— this is an API-first backend where a browser hitting an unmapped route
is an edge case, not the primary flow.

That static-page approach needed one non-obvious fix to actually work:
Spring Security secures the *internal forward* to `/error` too, not just
the original request. Without `.requestMatchers("/error").permitAll()` in
`SecurityConfig`, an unmapped route would get forwarded to `/error`
internally, that forwarded request would fail `.anyRequest().authenticated()`
same as any other unlisted path, and — since there's no
`AuthenticationEntryPoint` configured anymore (no `formLogin`, pure
stateless JWT) — Spring Security's default `Http403ForbiddenEntryPoint`
would reject it with a bare `403` before `BasicErrorController` ever ran.
Confirmed via `logging.level.org.springframework.security=DEBUG`: the log
showed `Securing GET /error` immediately followed by the entry point
rejecting it. Adding `/error` to the permitted paths doesn't weaken
anything else — every other route's rule is unchanged, and an anonymous
request to a route that's genuinely protected (not just unmapped) still
correctly returns `403` rather than leaking whether it exists.

## Concurrency: the borrow operation

`LoanService.borrowBookOptimistic` is the one wired to `POST /loans`. It's
`@Transactional` (Spring's own annotation, not the JTA
`jakarta.transaction.Transactional` — the Spring one supports
`propagation`/`rollbackFor`/etc., the JTA one doesn't), and relies on
`Book.version` — a concurrent conflicting update fails at commit time with
`ObjectOptimisticLockingFailureException`.

That exception is caught in `LoanController.createLoan`, **around the call**
to `borrowBookOptimistic(...)`, not inside the service method. This matters:
`@Transactional` is implemented via a proxy, and the actual commit (where a
version conflict would be discovered) happens when the proxied method call
returns to its caller — which is the controller, not somewhere inside the
service method itself. A `try/catch` inside the service method would not be
in scope when the exception is actually thrown.

`LoanService.borrowBookPessimistic` (backed by
`BookRepository.findByIdForUpdate`, `@Lock(PESSIMISTIC_WRITE)` →
`SELECT ... FOR UPDATE`) exists purely as a side-by-side comparison — it's
never called from any controller.

`findByIdForUpdate` is its own method rather than `@Lock` being added to a
redeclared `findById`. `@Lock` can technically be attached to any query
method including a plain derived `findById` — but doing that would apply
the pessimistic lock to *every* caller of `findById` throughout the app
(including plain reads like `GET /books/{id}`), not just the borrow flow.
A separately-named method keeps the locking scoped to where it's actually
needed.

`LoanService.createLoan(Book, Member)` is `private`. It's only ever called
from within `borrowBookOptimistic`/`borrowBookPessimistic`, after the
availability check has already passed — making it `public` would let
something create a `Loan` directly, bypassing that check entirely.

## Database migrations

Schema is managed by Flyway (`src/main/resources/db/migration`), not
`ddl-auto`. `spring.jpa.hibernate.ddl-auto=validate` means Hibernate still
checks on startup that entities match the schema and fails loudly if they
don't, but never generates or executes DDL itself.

Non-obvious version-specific gotcha: on Spring Boot 4.1, `flyway-core` +
`flyway-database-postgresql` alone are **not** enough — Boot 4.x moved
Flyway's autoconfiguration out of `spring-boot-autoconfigure` entirely into
a dedicated `spring-boot-starter-flyway` module. Without it, Flyway never
runs, silently — `ddl-auto=validate` then fails because the schema was
never created. (Confirmed by inspecting `spring-boot-autoconfigure-4.1.0.jar`
directly — it contains zero Flyway classes.)

This turned out to be part of a broader pattern in Boot 4.x, not a one-off:
security autoconfiguration was moved out of `spring-boot-autoconfigure`
into its own `spring-boot-security` module too (see "Security" below).
Worth remembering for later phases — Actuator and OpenAPI (Phase 5) are
exactly the kind of feature that could plausibly have moved the same way
on this version.

`V1__init.sql` is a single baseline migration, not a replay of this
project's actual incremental history — it was built by dropping the (by
then disposable, sample-data-only) dev database and writing the full
target schema fresh, including fixing a gap `ddl-auto=update` had silently
left: `book.isbn` was declared `@Column(unique = true)` on the entity but
was never actually enforced unique in the live database.

## Security (in progress)

Spring Security is enabled with a custom `SecurityConfig`
(`@EnableWebSecurity` + a `SecurityFilterChain` bean — required once you
define your own bean named `springSecurityFilterChain`/similar; Boot's
auto-`@EnableWebSecurity` is conditional on *no* such bean existing yet).

The filter chain `@Bean` method is named `securityFilterChain`, not
`springSecurityFilterChain` — that exact literal name is reserved by
Spring Security's own internal `WebSecurityConfiguration`, which registers
its combined `FilterChainProxy` under it. An earlier version of this config
named its own bean method `springSecurityFilterChain` too, which caused a
bean-definition-collision startup failure; `securityFilterChain` (the name
used in Spring Security's own reference docs/examples) avoids it.

The chain uses `formLogin`, not `httpBasic`. They were compared directly:
Basic Auth has no login page — the browser's own native credentials dialog
handles it, and once entered, the browser silently **caches and resends**
those credentials as an `Authorization` header on every subsequent request
until the browser process fully closes. That caching lives entirely in the
browser, independent of any server-side session — a page reload, a hard
refresh, even restarting the Spring Boot app doesn't clear it, which made
Basic Auth confusing to test against session-based auth specifically. Form
login was more consistent with the "session-based" premise of this phase.

Current state (Phase 3, session-based — superseded by Phase 4, below):
- `GET /api/books/**`, `GET /api/authors/**`, `POST /api/members` are
  public; everything else requires authentication.
- Authentication is backed by the `Member` table (`MemberDetailsService`),
  with role-based (`MEMBER`/`LIBRARIAN`) and ownership-based method
  security (`@PreAuthorize`) in place — see `REQUIREMENTS.md` Phase 3.

`.logout(logout -> logout.invalidateHttpSession(true))` is written out
explicitly even though `true` is already `LogoutConfigurer`'s default —
confirmed empirically by temporarily removing the line entirely and
re-testing the full login/logout cycle via `curl`, which behaved
identically either way. It's kept because "configured explicitly" was
about deliberately writing the code (documenting the decision, and having
the hook already in place for future customization), not about changing
runtime behavior that already happens by default.

CSRF was enabled by default through the rest of Phase 3, with a dedicated
round-trip test proving it (`CsrfRoundTripTest`) — worth knowing while
reading that test: the CSRF token **rotates after a successful login**. A
token fetched from `/login` before authenticating is no longer valid for
a subsequent `/logout` call in the same session; a fresh one has to be
re-fetched post-login. CSRF is disabled again in Phase 4 — see below for
why that's correct rather than a regression.

### Stateless JWT (Phase 4)

`.formLogin(...)` was removed entirely rather than kept alongside JWT —
the two don't meaningfully coexist under this config. Form login's whole
point is establishing a session; `SessionCreationPolicy.STATELESS` (set
once JWT was introduced) tells Spring Security never to create or rely on
one. Keeping both configured together wouldn't be two features coexisting,
it'd be one mechanism (form login) built entirely around a resource
(`HttpSession`) that the other explicitly forbids using. JWT — via a
custom `JwtFilter`, a `OncePerRequestFilter` registered before
`UsernamePasswordAuthenticationFilter` — is now the only authentication
path.

Tokens are carried via the `Authorization: Bearer <token>` header, not an
httpOnly cookie. Two reasons: it matches how this project is actually
tested throughout (`curl`, Postman, `MockMvc` all set headers directly; a
cookie would mean managing a `Set-Cookie` response and a cookie jar in
every test tool instead), and it's what makes disabling CSRF
(`http.csrf(AbstractHttpConfigurer::disable)`) correct rather than
reckless — CSRF exploits rely on a browser *automatically* attaching a
cookie to a forged cross-site request, and a header is only ever attached
by client code that explicitly does so. The honest tradeoff being
accepted: a header-based token is typically kept in browser memory or
`localStorage` by a real frontend, both readable by injected JS (XSS) in
a way an httpOnly cookie isn't. This project has no browser-based client
consuming the API yet, so that exposure doesn't currently apply — but
it's the real cost of this choice if one is added later.

**Refresh tokens are opaque and persisted server-side, not a second JWT.**
This looks like it fights the "stateless" premise above, and it does —
deliberately. Access tokens stay pure stateless JWTs specifically because
their short lifespan (15 min) makes that safe: a compromised one heals
itself quickly no matter what anyone does. A refresh token doesn't have
that luxury — it's long-lived and high-value, so the ability to kill one
immediately (a stolen device, a user hitting logout) matters more than
staying stateless. A signed JWT refresh token can't be revoked before its
own expiry at all: the server has no record it exists, so there's nothing
to invalidate. An opaque random string in a DB table, keyed to the
`Member` with issued/expiry/revoked columns, can be deleted or flagged the
moment it needs to stop working. This is also the industry-standard split
(access tokens stateless, refresh tokens tracked), not a one-off choice
for this project.

**Refresh tokens rotate — single-use, reissued on every refresh, not valid
until their own expiry.** Once a refresh token is opaque and persisted
anyway, rotation is nearly free: reusing the same server-side record to
mark a token "used" and swap in its replacement needs no new
infrastructure. The security benefit is concrete: if a refresh token is
ever stolen and used by an attacker, the legitimate client's *next*
refresh attempt will fail (its token was already consumed), which is a
detectable signal that something is wrong — a non-rotating token gives no
such signal and just stays quietly valid for whoever holds it until it
naturally expires. This also directly sets up the
`POST /auth/logout` decision (see `REQUIREMENTS.md`): with rotation
already tracking refresh tokens server-side, logout has real state to
revoke rather than needing a separate mechanism bolted on afterward.

A consumed `RefreshToken` row is marked with a `used`/`revoked` flag, not
deleted. Deleting was the first instinct — it avoids the table filling up
with spent rows — but it throws away exactly the thing rotation exists to
provide: the difference between "this token never existed" and "this
token existed and was already used once." That second case is the actual
attack signal (a stolen token being replayed after the legitimate client
already rotated it); collapsing both into "not found" erases it. Deleting
also doesn't solve the growth problem it was chosen for — flagged rows and
expired-but-unrotated rows both still accumulate either way, so this isn't
trading a real fix for a security cost, just declining a fake one. Cleanup
of old rows stays the same known, deferred simplification noted in
`REQUIREMENTS.md` regardless of which approach was picked here.

**`POST /auth/logout` only revokes the refresh token — it does not kill the
current access token early.** It reuses the exact mechanism rotation
already provides: the presented refresh token is validated and marked
`used`, the same as a normal refresh, just without issuing a replacement.
The consequence is a deliberate one: an access token that's already been
issued keeps working for whatever remains of its 15-minute lifespan after
logout, since nothing about it changes. Actually invalidating it early
would mean a `jti`-based revocation list checked on every request (option
b in `REQUIREMENTS.md`) — real infrastructure, and a per-request lookup
that undoes the reason access tokens are stateless JWTs in the first
place. That cost isn't justified here: this is a library catalog/loan
system, not a banking or otherwise high-stakes application, so a short,
bounded window where a just-logged-out access token remains technically
valid is an acceptable tradeoff against not adding that infrastructure.

### Security response headers

These headers are defense-in-depth: even if the app logic and other
security controls are correct, they instruct the *browser* to restrict
what it does with the response, closing off classes of attack that live
entirely on the client side.

- **CSP (Content-Security-Policy)** restricts which sources scripts,
  styles, images, etc. can be loaded from. It doesn't stop XSS from being
  injected, but it limits what an injected script can actually do (e.g. it
  can't load or execute from an attacker's domain if the policy doesn't
  allow it).
- **X-Frame-Options** stops the page being loaded inside an `<iframe>` on
  another site — prevents clickjacking, where an attacker overlays your
  page invisibly and tricks a user into clicking something they didn't
  mean to.
- **HSTS (Strict-Transport-Security)** tells the browser "always use HTTPS
  for this domain, for the next `max-age` seconds — never fall back to
  plain HTTP, even if the user types `http://` or clicks an `http://`
  link." Without it, the *first* request to a domain can go out over plain
  HTTP before any redirect to HTTPS happens, and that one request is
  interceptable/rewritable by a man-in-the-middle (SSL-stripping). HSTS
  closes that gap by making the browser upgrade to HTTPS itself, before
  any request is sent — but only after it's seen the header once over an
  already-HTTPS connection, which is why it's meaningless over plain HTTP
  in local dev.

Confirmed via `curl -i` against a running instance (no header config
written yet) which of these Spring Security already sets by default:
`X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`,
`X-XSS-Protection: 0`, and `Cache-Control`/`Pragma`/`Expires`
(no-cache). Two are different:
- **HSTS** is technically also on by default (`HstsHeaderWriter`), but it
  only writes the header when the request is already over HTTPS
  (`request.isSecure()`) — on plain HTTP in local dev it never appears,
  not because it's disabled but because the condition for it isn't met.
  No new code is needed for the header to exist; code is only needed to
  customize its values (`max-age`, `includeSubDomains`, `preload`).
- **CSP** is genuinely absent by default — no `Content-Security-Policy`
  header shows up at all without explicit
  `.headers(headers -> headers.contentSecurityPolicy(...))` config.

### CORS

CORS config belongs in `SecurityConfig`, wired into `HttpSecurity` via
`.cors(...)` and a `CorsConfigurationSource` bean — not a `WebMvcConfigurer`
`addCorsMappings()` override, and not `@CrossOrigin` on controllers.
Spring Security's filter chain runs before a request ever reaches
DispatcherServlet/MVC, and a CORS preflight (`OPTIONS`) request is
unauthenticated by nature — `WebMvcConfigurer`-level CORS handling only
applies once a request reaches a mapped handler, so Security's
`anyRequest().authenticated()` would reject the preflight first and the
browser would never see the `Access-Control-Allow-*` headers. Configuring
CORS on `HttpSecurity` instead makes Security itself aware of and permit
the preflight before its auth rules apply.

## Testing

No automated test suite yet (`MockMvc`/`WebTestClient`/Testcontainers are
Phase 6 stretch goals). Verification so far has been manual: Postman
against `sample-authors.json`/`sample-books.json`, plus `sample-data.sql`
for reseeding the dev database directly via `psql`.
