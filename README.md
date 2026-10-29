# Spring Rate Limit Showcase

A deliberately small Spring Boot project that demonstrates production-oriented Java API engineering rather than business-domain complexity.

The focus is on **clean rate limiting, testability, configuration, consistent API errors, static code-quality gates, and CI**. It is intended as a GitHub portfolio project for Java/Spring backend roles.

## What this repository demonstrates

- Spring Boot 4.1.2 with Java 21
- REST endpoints with Spring MVC
- Token-bucket rate limiting with Bucket4j
- Per-client isolation through a dedicated `ClientKeyResolver`
- Bounded in-memory client bucket storage with Caffeine
- Externalized and validated rate-limit configuration
- HTTP `429 Too Many Requests` with `Retry-After`
- Structured RFC-style `ProblemDetail` error responses
- Central `@RestControllerAdvice` exception handling for rate limits, validation, and malformed JSON
- Bean Validation for request DTOs
- JUnit 5 unit and Spring MVC integration tests
- Maven Checkstyle quality gate
- GitHub Actions CI on pushes and pull requests
- Actuator health endpoint

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.1.2 / Spring MVC |
| Rate limiting | Bucket4j 8.21.0 |
| Client bucket cache | Caffeine |
| Validation | Jakarta Bean Validation |
| Tests | JUnit 5, AssertJ, Spring MockMvc |
| Static quality | Checkstyle 14.3.0 via Maven Checkstyle Plugin 3.6.0 |
| Build | Maven |
| CI | GitHub Actions |

## Architecture

```text
HTTP request
    |
    v
RateLimitInterceptor
    |
    +--> ClientKeyResolver
    |
    +--> RateLimiter interface
            |
            v
       Bucket4jRateLimiter
            |
            +--> Caffeine cache: client key -> Bucket4j bucket
    |
    +--> allowed --------> Controller
    |
    +--> rejected -------> RateLimitExceededException
                              |
                              v
                         ApiExceptionHandler
                              |
                              v
                         HTTP 429 ProblemDetail
```

The Spring MVC interceptor only coordinates HTTP concerns. The limiter itself is hidden behind the `RateLimiter` interface, which keeps the design replaceable and easy to test.

## Rate-limiting behavior

Default configuration in `src/main/resources/application.yml`:

```yaml
app:
  rate-limit:
    capacity: 5
    refill-tokens: 5
    refill-period: 1m
    max-tracked-clients: 10_000
    expire-after-access: 15m
```

This means each tracked client can consume five requests before tokens must be replenished. Client buckets that are not used for 15 minutes are evicted, and the cache is bounded to 10,000 tracked clients.

For this showcase, `X-Client-Id` acts as a simple stand-in for an authenticated API client identity. If it is absent, the remote IP address is used. In a real authenticated service, the key should normally come from a trusted principal, API key, tenant ID, or another server-verified identity rather than a freely chosen request header.

Successful responses expose:

```text
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 4
```

Rejected requests additionally return:

```text
HTTP/1.1 429 Too Many Requests
Retry-After: 12
```

and a structured `ProblemDetail` body.

## Prerequisites

You need:

- Git
- JDK 21
- Maven 3.9 or newer
- VS Code

### Windows: install Java 21

In PowerShell:

```powershell
winget install --id EclipseAdoptium.Temurin.21.JDK
```

Open a new terminal afterwards and verify:

```powershell
java -version
javac -version
```

Both commands should report Java 21.

### Windows: install Maven

Download the current Maven 3.9.x binary ZIP from the official Apache Maven website and extract it, for example to:

```text
C:\Tools\apache-maven-3.9.x
```

Then add this folder to your user or system `Path`:

```text
C:\Tools\apache-maven-3.9.x\bin
```

Open a new PowerShell window and verify:

```powershell
mvn -version
```

The output should show Maven and Java 21.

## VS Code setup

The repository already contains `.vscode/extensions.json`. VS Code should offer to install the recommended extensions automatically.

You can also install them manually:

```powershell
code --install-extension vscjava.vscode-java-pack
code --install-extension vmware.vscode-boot-dev-pack
```

These provide Java language support, debugging, JUnit integration, Maven tooling, Spring Boot configuration support, Spring Initializr, and the Spring Boot Dashboard.

Open the project folder itself in VS Code:

```powershell
code .
```

When VS Code asks whether it should import or update the Maven project, accept it.

## Run the application

From the repository root:

```powershell
mvn spring-boot:run
```

The API starts on port `8080`.

Health check:

```powershell
curl.exe http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## Try the API

### Ping endpoint

```powershell
curl.exe -i -H "X-Client-Id: demo-user" http://localhost:8080/api/v1/ping
```

Call it repeatedly with the same `X-Client-Id`. After the configured capacity is exhausted, the API returns `429 Too Many Requests`.

Use a different client ID to demonstrate client isolation:

```powershell
curl.exe -i -H "X-Client-Id: another-user" http://localhost:8080/api/v1/ping
```

### Validated echo endpoint

```powershell
curl.exe -i `
  -X POST `
  -H "Content-Type: application/json" `
  -H "X-Client-Id: echo-user" `
  -d '{"message":"hello portfolio"}' `
  http://localhost:8080/api/v1/echo
```

A blank message produces a structured HTTP 400 response from `ApiExceptionHandler`.

## Tests

Run all tests:

```powershell
mvn test
```

The suite contains two layers:

1. **Unit tests** for the limiter and client-key resolver, including bucket exhaustion and client isolation.
2. **Integration tests** with `MockMvc`, covering real Spring MVC request handling, rate-limit headers, HTTP 429 responses, `Retry-After`, `ProblemDetail`, and validation errors.

The integration tests deliberately override the rate-limit configuration so they are deterministic and fast.

## Checkstyle and the full quality gate

Run Checkstyle directly:

```powershell
mvn checkstyle:check
```

Run the same verification used by CI:

```powershell
mvn verify
```

`mvn verify` runs the JUnit test suite and fails the build if the Checkstyle rules are violated. Maven Enforcer also rejects builds that are not using Java 21 or Maven 3.9+.

The custom rules live in:

```text
config/checkstyle/checkstyle.xml
```

They enforce, among other things, naming conventions, import hygiene, braces, whitespace, line length, and common Java correctness rules.

## GitHub Actions

`.github/workflows/ci.yml` runs on pushes and pull requests targeting `main`.

The workflow:

1. checks out the repository,
2. installs Eclipse Temurin Java 21,
3. enables Maven dependency caching,
4. executes `mvn verify`.

A pull request therefore cannot pass CI while tests or Checkstyle are failing.

## Suggested Git and GitHub workflow

### 1. Initialize the repository

From the project directory:

```powershell
git init -b main
git add .
git commit -m "feat: add Spring Boot rate limiting showcase"
```

### 2. Create a GitHub repository

Create a new **public** repository named:

```text
spring-rate-limit-showcase
```

Do not initialize it with another README, `.gitignore`, or license because those files already exist locally.

Then connect and push:

```powershell
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/spring-rate-limit-showcase.git
git push -u origin main
```

### 3. Use short-lived branches for future improvements

Example:

```powershell
git switch -c feat/redis-rate-limit-storage
```

After implementing and verifying the change:

```powershell
mvn verify
git add .
git commit -m "feat: add distributed Redis rate limit storage"
git push -u origin feat/redis-rate-limit-storage
```

Open a pull request into `main`, let CI run, and merge only when it is green.

Recommended commit prefixes:

```text
feat:     new behavior
fix:      bug fix
test:     test-only changes
refactor: internal redesign without behavior change
ci:       GitHub Actions / pipeline changes
docs:     documentation
chore:    maintenance
```

## Design decisions worth discussing in an interview

### Why an interceptor instead of putting logic in the controller?

Rate limiting is a cross-cutting HTTP concern. Keeping it in an interceptor prevents controller duplication while still allowing exceptions to be handled through Spring MVC's exception-resolution infrastructure.

### Why a `RateLimiter` interface?

The web layer does not depend directly on Bucket4j. The implementation can later be replaced with a Redis-backed or gateway-based limiter without rewriting controllers or the interceptor.

### Why Caffeine instead of `ConcurrentHashMap`?

A plain map of client IDs to buckets can grow forever. The bounded, expiring Caffeine cache explicitly addresses that memory-risk tradeoff for the single-instance demo.

### Why is this not automatically safe for a multi-instance deployment?

The bucket state is local to one JVM. With multiple application replicas, the same client could hit different instances and receive a separate bucket on each instance. A production distributed deployment should use shared state such as Redis, a Bucket4j distributed backend, or rate limiting at the API gateway.

### Why externalized configuration?

Capacity, refill behavior, and cache limits are operational concerns. `@ConfigurationProperties` keeps them type-safe, validated, and changeable without modifying Java source code.

## Production extensions

Good follow-up features, without bloating the initial showcase, would be:

- Redis-backed distributed rate limiting
- authenticated client or tenant identity
- Micrometer counters for allowed and rejected requests
- configurable rate-limit policies per endpoint or subscription tier
- OpenAPI documentation
- container image / Dockerfile
- Testcontainers for a distributed-storage integration test

## Repository structure

```text
.
├── .github/workflows/ci.yml
├── .vscode/
├── config/checkstyle/checkstyle.xml
├── src/
│   ├── main/
│   │   ├── java/dev/portfolio/ratelimit/
│   │   │   ├── api/
│   │   │   ├── config/
│   │   │   ├── exception/
│   │   │   ├── ratelimit/
│   │   │   └── web/
│   │   └── resources/application.yml
│   └── test/java/dev/portfolio/ratelimit/
├── .editorconfig
├── .gitignore
├── .java-version
├── LICENSE
├── pom.xml
└── README.md
```

## License

MIT
