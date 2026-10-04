# SimpleUserManagement

A minimal full-stack user management system built with Spring Boot. It provides username/password registration and login, Google OAuth2 sign-in with a register-first flow, an administrative panel for account management, and automatic database schema creation on startup.

Credits to GLM 5.3 flash for assisting in project guidance.

live demo: https://consultants-hardcover-evaluated-gmc.trycloudflare.com 
Hosted at my home NAS server, optimally 24/7 but may subject to indefinite downtimes.

#### admin account: 
username: admin
password: 12345678

## Technology Stack

| Layer      | Technology                                              |
|------------|----------------------------------------------------------|
| Backend    | Java 21, Spring Boot 4.1 (WebMVC, Spring Security 7)     |
| Frontend   | Thymeleaf server-rendered pages styled with Tailwind CSS |
| Database   | MySQL 8 (accessed through Spring `JdbcClient`)           |
| Auth       | Spring Security form login + OAuth2 client (Google)      |
| Build      | Maven (wrapper included), Docker                         |

## Features

### Users

- Register a local account with username, nickname, email, and password (BCrypt-hashed, minimum 8 characters enforced server-side).
- Sign in with Google. A Google account that has never been seen before is redirected to a pre-filled registration page (email and avatar carried over from the Google profile) instead of being silently auto-registered; completing that step creates the account, which is then linked to the Google identity for all future logins.
- View the personal profile page: user ID, nickname, email, role, login method, avatar, account creation date, last login, and last logoff.
- Log off. Logging off invalidates the session and records the logoff time.

### Administrators

- Log in through a separate, distinct login screen at `/admin/login`. Admin status is a flag on the user row, so both local and Google users can be administrators.
- View a dashboard listing every account with all fields: ID, username, nickname, email, password hash, role, admin flag, enabled flag, OAuth2 provider and handle, creation date, last login, and last logoff.
- Edit the username, nickname, email, and password of any account. Immutable fields (ID, OAuth2 handle, timestamps) are displayed read-only.
- Delete non-admin accounts. Deleting administrators, including oneself, is refused; admin rights must be revoked through the edit screen first.

### Security measures

- BCrypt password hashing (random per-password salt embedded in the hash).
- CSRF protection on all state-changing POST endpoints (Thymeleaf forms carry tokens automatically).
- All database access uses parameterized statements via `JdbcClient` (no string-concatenated SQL).
- Two independent security filter chains: the user area and the admin area have separate login pages, sessions, and logout endpoints.
- Automatic `CREATE TABLE IF NOT EXISTS` on startup plus a guarded idempotent migration for schema changes on existing databases.

## Endpoints and Page Transitions

### Routing table

| Method | Path                          | Access            | Description                                                          |
|--------|-------------------------------|-------------------|----------------------------------------------------------------------|
| GET    | `/`                           | Public            | Home page. Shows login/register links, or a link to the profile.     |
| GET    | `/login`                      | Public            | User login page (form + Google button).                              |
| POST   | `/login`                      | Public            | Login processing (handled by Spring Security).                       |
| GET    | `/register`                   | Public            | Registration page. Pre-filled when arriving from an OAuth2 callback. |
| POST   | `/register`                   | Public            | Account creation (local or OAuth2-linked).                           |
| GET    | `/oauth2/authorization/google`| Public            | Starts the Google OAuth2 flow (redirect to Google).                  |
| GET    | `/login/oauth2/code/google`   | Public            | OAuth2 callback (handled by Spring Security).                        |
| GET    | `/me`                         | Authenticated     | Personal profile page.                                               |
| POST   | `/logout`                     | Authenticated     | Logoff; stamps `last_logoff` and invalidates the session.            |
| GET    | `/admin/login`                | Public            | Administrator login screen.                                          |
| POST   | `/admin/login`                | Public            | Admin login processing.                                              |
| GET    | `/admin`                      | Role `ADMIN`      | Dashboard: all users, all fields.                                    |
| GET    | `/admin/users/{id}`           | Role `ADMIN`      | Edit form for one user.                                              |
| POST   | `/admin/users/{id}`           | Role `ADMIN`      | Save edits (username, nickname, email, password).                    |
| POST   | `/admin/users/{id}/delete`    | Role `ADMIN`      | Delete a user (admins are protected).                                |
| POST   | `/admin/logout`               | Role `ADMIN`      | Admin logoff.                                                        |
| GET    | `/css/app.css`                | Public            | Stylesheet (dummy placeholder; replace with Tailwind CLI output).    |

### Flow graph

```
                     +------------------+
                     |      home /      |
                     +--------+---------+
                        not logged in
              +----------------+----------------+
              v                                 v
        +---------+                       +-----------+
        | /login  |                       | /register |
        +----+----+                       +-----+-----+
             |  form login                      |  local sign-up
             |    or                            v
             |                          POST /register
             |                                  |
             +----+ "Continue with Google"     |
                  v                            |
        accounts.google.com (consent)          |
                  |                            |
        /login/oauth2/code/google                |
                  |                            |
        handle known in DB?                      |
           yes  \              \ no             |
                 v               \               |
                /me          /register?oauth=google&email=...
                             |  (pre-filled; password hidden)
                             +----> POST /register
                                        |
                                        v
                              back to /login?registered
                                        |
                                        v
                                    +--------+
                                    |  /me   |<--- logged-in users
                                    +---+----+
                                        | logoff
                                        v
                                 /login?logout

  Admins additionally:
        /admin/login ---> /admin (dashboard) ---> /admin/users/{id} (edit)
                                             ---> POST delete
                                             ---> /admin/logout
```

### Behavior notes

- Redirect query parameters: `?registered` after successful sign-up, `?error` after a failed login, `?logout` after logoff, `?saved` and `?deleted` and `?delete-denied` after admin actions.
- Any unknown URL or unhandled error renders the styled `error.html` page for browser clients; API clients receive the standard JSON error body.
- Requests from unauthenticated users to protected URLs are redirected to the matching login screen before any error handling applies.

## Getting Started

### Prerequisites

- JDK 21 (Temurin recommended)
- MySQL 8 running locally or remotely
- A Google Cloud project with OAuth2 credentials (optional, only for Google sign-in)
- Docker (optional, for containerized deployment)

### Database preparation

Create a dedicated database and user (the application creates its own tables on first boot):

```sql
CREATE DATABASE sumdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'sumdb_admin'@'localhost' IDENTIFIED BY 'CHANGE_ME_PASSWORD';
GRANT ALL PRIVILEGES ON sumdb.* TO 'sumdb_admin'@'localhost';
FLUSH PRIVILEGES;
```

### Import into Eclipse

1. `File` > `Import...` > `Maven` > `Existing Maven Projects`.
2. Browse to the project root directory (the folder containing `pom.xml`) and click `Finish`.
3. Verify the JRE: `Window` > `Preferences` > `Java` > `Installed JREs` must contain a Java 21 JDK; set it in `Project` > `Properties` > `Java Build Path` if needed.
4. Let m2e resolve dependencies, then run `SimpleUserManagementApplication` as a Java application, or run `mvnw spring-boot:run` from the project directory.

### Import into IntelliJ IDEA

1. `File` > `Open...` and select the project root directory (or `New` > `Project from Existing Sources...` and choose "Import project from external model" > Maven).
2. Open `pom.xml` and enable "Trust project" / "Enable auto-import" when prompted.
3. Set the SDK: `File` > `Project Structure` > `Project` > SDK: select a Java 21 JDK.
4. Run the main class `com.mjdev.SimpleUserManagement.SimpleUserManagementApplication`, or use the Maven panel: `Plugins` > `spring-boot` > `spring-boot:run`.

### Google OAuth2 credentials

1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), create an OAuth 2.0 Client ID of type "Web application".
2. Set the authorized redirect URI to `http://localhost:8080/login/oauth2/code/google` (adjust host/port for deployments).
3. Copy the client ID and client secret into `application.properties`.

## Configuration

Development configuration lives in `src/main/resources/application.properties`. Release builds (`-Prelease`) exclude it from the jar, so the runtime configuration must be provided externally (see the Docker section).

```properties
spring.application.name=SimpleUserManagement
server.port=8080

# Database
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/sumdb
spring.datasource.username=sumdb_admin
spring.datasource.password=CHANGE_ME_PASSWORD

# Run schema.sql on every boot (required for MySQL; the script is idempotent)
spring.sql.init.mode=always

# Google OAuth2
spring.security.oauth2.client.registration.google.client-id=CHANGE_ME_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=CHANGE_ME_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

To grant administrator rights to an account:

```sql
UPDATE users SET `admin` = 1 WHERE username = 'your_username';
```

## Building

The project ships with the Maven wrapper, so no local Maven installation is required.

Development build (includes `application.properties`, compiles tests):

```bash
./mvnw clean package
```

Release build (strips configuration from the jar, stable artifact name):

```bash
./mvnw clean package -Prelease
```

The artifact is produced at `target/simpleusermanagement.jar`. A release jar contains no database credentials and will not start without an external configuration file.

Running the test suite (requires a reachable MySQL instance configured in `application.properties`; tests create and remove only their own `junit`-prefixed rows):

```bash
./mvnw test
```

## Docker Deployment

The Dockerfile is a two-stage build: stage one compiles the release jar with Maven, stage two copies it onto a slim JRE base image and runs it as a non-root user. The container expects its configuration at `/app/config/application.properties`, which is supplied through a volume mount.

### Build strategies

The compiled jar is architecture-neutral; only the JRE base image architecture matters. Choose one of the following.

1. Native build (image matches the host architecture, e.g. building on an x86 server for x86, or directly on an ARM NAS):

```bash
sudo docker build -t simpleusermanagement:latest .
```

2. Cross compilation via the `FROM --platform` directive. Pin the runtime base image to a foreign architecture; the jar needs no recompilation. Requires QEMU binfmt support on the builder:

```bash
sudo docker run --privileged --rm tonistiigi/binfmt --install all
sudo docker build --build-arg TARGETPLATFORM=linux/arm64 -t simpleusermanagement:arm64 .
```

with the runtime stage expressed as `FROM --platform=$TARGETPLATFORM eclipse-temurin:21-jre` if you want a single Dockerfile to serve both targets.

3. Multi-platform builds with buildx (the recommended approach; builds both architectures in parallel using per-platform base images):

```bash
sudo docker buildx create --use
sudo docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t simpleusermanagement:latest \
  --push .
```

(`--push` publishes to a registry; for a single-platform local load, use `--platform linux/arm64 --load .`.)

### Create the container

```bash
sudo docker container create \
  --name sumapp \
  --network bridge \
  -p 8080:8080 \
  -v /volume1/docker/simpleusermanagement/config/:/app/config:ro \
  --restart unless-stopped \
  simpleusermanagement:latest

sudo docker container start sumapp
```

Parameter notes:

- `--network bridge` is the Docker default and may be omitted. Use `--network host` if the application must reach MySQL on the host's `localhost` without extra network configuration.
- `-p 8080:8080` publishes container port 8080 on host port 8080. Change the left side to remap (e.g. `-p 9090:8080`), and keep `server.port` consistent inside the container.
- The volume maps the host configuration folder to `/app/config`. The `:ro` suffix makes it read-only inside the container. A release jar refuses to start without this file.
- `--restart unless-stopped` brings the container back after host reboots and crashes.

### Runtime configuration template

Place this file at `/volume1/docker/simpleusermanagement/config/application.properties` (host side) and fill in the values:

```properties
spring.application.name=SimpleUserManagement
server.port=8080

spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://YOUR_DB_HOST:3306/sumdb
spring.datasource.username=YOUR_DB_USER
spring.datasource.password=YOUR_DB_PASSWORD

spring.sql.init.mode=always

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=openid,profile,email
```

Important: `localhost` in `spring.datasource.url` refers to the container itself, not the host machine. Use the host LAN address, a DNS name, or `--network host` accordingly. The Google redirect URI must also match the publicly reachable address of the deployment.

### Logs and maintenance

```bash
sudo docker logs -f sumapp              # follow application logs
sudo docker restart sumapp              # restart after config changes
sudo docker rm -f sumapp                # remove before recreating with new settings
```

## Project Structure

```
src/main/java/com/mjdev/SimpleUserManagement/
    config/       SecurityConfig (two filter chains), schema migration runner
    controller/   WebController (public/user pages), AdminController
    model/        User record and form/display records
    repository/   UserRepository (JdbcClient, parameterized SQL)
    security/     UserDetailsService, login/logoff handlers, OAuth2 user service
    service/      UserService (registration and admin edits)
src/main/resources/
    schema.sql    CREATE TABLE IF NOT EXISTS users
    templates/    index, login, register, me, admin/*, error
    static/css/   app.css (replace with Tailwind CLI output)
src/test/java/    BaseIntegrationTest, WebFlowTests, AdminTests (MockMvc)
```

## Roadmap

### Input validation and hardening

- **Server-side field length limits.** The database columns are finite (`username` VARCHAR(50), etc.) but the forms only constrain lengths in the browser via `maxlength`. A direct POST bypasses this and would cause truncated writes or database errors. The plan is bean-validation annotations (`@Size`, `@Pattern`) on the form records, matching column sizes exactly, plus a servlet-level cap on POST body size.
- **Character allow-listing.** Usernames currently accept arbitrary text. Restricting them to a safe alphabet (`[A-Za-z0-9_.-]`) removes a class of downstream problems (log injection via crafted usernames, filesystem or URL confusion if usernames ever appear in paths).
- **Stored XSS review.** Thymeleaf `th:text` escapes output by default, which currently covers user-supplied fields such as nickname and avatar URL. A dedicated sanitization pass (allow-list of URL schemes for `avatar_url`, rejecting `javascript:` URIs) would make that invariant explicit rather than incidental.
- **SQL injection beyond parameterization.** Queries are already fully parameterized; the remaining improvements are structural: a production database user without DDL rights (schema creation/migration moves behind a separate deployment step), an explicit allow-list of sortable/filterable columns before any dynamic ORDER BY is ever introduced, and keeping the `schema.sql` surface reviewed so no future statement interpolates user input.
- **Rate limiting and lockout.** Throttle login attempts per IP and per account, with temporary lockout after repeated failures, to blunt credential stuffing.
- **Password policy strengthening.** Beyond the current 8-character minimum: breach-list checking (k-anonymity HIBP API) or a composition heuristic.

### Architecture and operations

- **`@ControllerAdvice` global exception handling** with structured logging of unexpected exceptions, so the error page and the logs stay in sync.
- **Spring Boot Actuator** with health and info endpoints, and a Docker `HEALTHCHECK` wired to it.
- **TLS termination guidance** (reverse proxy or `server.ssl` configuration) and security headers (Content-Security-Policy, HSTS, X-Frame-Options).
- **Dashboard pagination and search.** `findAll()` currently loads every user; add keyset pagination and a filter box once the table grows.
- **Configuration secret management.** Move the Google client secret and DB password out of plain properties files (environment variables, Docker secrets, or a vault).
- **CI pipeline** running `./mvnw test` and a container build on every push; publish the image to a registry so NAS deployments pull instead of building.
- **Mail integration.** The `spring-boot-starter-mail` dependency is already present; use it for email verification on local registration and a password-reset flow (the biggest functional gap compared to production systems).
- **Account linking UX.** Let a logged-in local user attach a Google identity from their profile page, instead of relying on the email-match heuristic during OAuth registration.
- **Role management in the admin UI.** The `role` column exists but is unused; surface it as an editable field and consider splitting `admin` into a proper role table if finer-grained permissions arrive.
- **E2E coverage of the Google flow.** The OAuth round-trip is only manually testable today; a Testcontainers-based test with a mock OIDC provider would close that gap.

## License

Unspecified. All rights reserved by the author.
