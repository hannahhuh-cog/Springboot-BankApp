# Spring Boot Dependency Upgrade

Procedure for upgrading dependencies in this Spring Boot banking application.
Follow every step in order. Do not skip steps. Run the verification build after
each `pom.xml` change before moving on.

---

## Pre-requisites

- Java 17+ and Maven 3.9+ available on `PATH`.
- A running MySQL instance **or** an H2/Testcontainers setup so that
  `mvn verify` can execute integration tests.
- Write access to the repo and permission to open a pull request.

---

## Step 1 -- Audit `pom.xml` Dependencies

1. Open `pom.xml` and identify the Spring Boot parent version
   (`spring-boot-starter-parent`). Record the current version.
2. Check [Spring Boot releases](https://spring.io/projects/spring-boot#support)
   for the latest patch in the current minor line **and** the latest stable
   minor/major release. Prefer the latest patch of the current line unless the
   task explicitly requests a major bump.
3. For **every** `<dependency>` entry, determine whether it is managed by the
   Spring Boot BOM (`spring-boot-dependencies`):
   - **BOM-managed** (no `<version>` tag): leave as-is. Never add an explicit
     version that overrides the BOM.
   - **Pinned version present**: compare the pinned version against the BOM
     value for that artifact. If the BOM already manages it, **remove** the
     explicit version. If the artifact is *not* in the BOM, check Maven Central
     for the latest stable release and update if appropriate.
4. Watch for **deprecated coordinates**. Common example in this repo:

   | Deprecated artifact              | Replacement                        |
   |----------------------------------|------------------------------------|
   | `mysql:mysql-connector-java`     | `com.mysql:mysql-connector-j`      |

   When a coordinate has been relocated, switch to the new GAV and remove the
   hardcoded version if the BOM manages it; otherwise pin the latest stable
   version.

5. Record every change in a table (artifact, old version, new version, reason)
   for the PR description.

---

## Step 2 -- Align Build Plugin Configuration

1. Read the `<java.version>` property in `pom.xml` (currently `17`).
2. Verify that `maven-compiler-plugin` `<source>` and `<target>` (or `<release>`)
   match that value. **This repo currently sets source/target to 1.8 -- fix it.**
   Preferred approach:

   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-compiler-plugin</artifactId>
       <!-- Let the BOM manage the version; remove an explicit <version> if the
            BOM provides one. Otherwise use the latest stable release. -->
       <configuration>
           <release>${java.version}</release>
       </configuration>
   </plugin>
   ```

   Using `<release>` instead of separate `<source>`/`<target>` is preferred for
   Java 9+. It sets both and also enforces API compatibility.

3. For `spring-boot-maven-plugin`, ensure no explicit version is set (the parent
   POM manages it).

---

## Step 3 -- Build Verification (run after EVERY `pom.xml` change)

```bash
mvn clean verify -DskipTests=false
```

- The build **must** succeed with zero test failures before proceeding.
- If it fails, diagnose and fix before moving to the next step. Common issues:
  - Compilation errors from Java-version mismatch.
  - Removed or relocated classes after a dependency upgrade (e.g., `javax.*` to
    `jakarta.*` in Spring Boot 3.x).
  - Test failures from changed default behavior in newer library versions.
- Re-run `mvn verify -DskipTests=false` after every fix.

---

## Step 4 -- Review `SecurityConfig.java` for Misconfigurations

After dependency changes, review
`src/main/java/com/example/bankapp/config/SecurityConfig.java` for security
issues that may have been introduced or masked by version changes:

### 4a. CSRF Protection

- For a server-rendered banking application (Thymeleaf), CSRF **must** be
  enabled. If the config contains `.csrf(csrf -> csrf.disable())`, remove
  it or replace with explicit CSRF configuration:

  ```java
  .csrf(Customizer.withDefaults())
  ```

  Then ensure every Thymeleaf `<form>` includes the CSRF token (Thymeleaf's
  Spring Security integration adds it automatically when CSRF is enabled).

### 4b. CORS Policy

- If a `.cors()` customizer is present, verify the allowed origins are not
  `"*"`. For a monolithic Thymeleaf app with no separate SPA front-end, CORS
  configuration is typically unnecessary -- remove it unless the project
  explicitly requires cross-origin API access.

### 4c. HTTPS / Transport Security

- For production deployments, enforce HTTPS:

  ```java
  .requiresChannel(channel -> channel.anyRequest().requiresSecure())
  ```

  If the app runs behind a TLS-terminating reverse proxy (Nginx, ALB), this
  may be handled externally. In that case, add a code comment noting the
  assumption and ensure `server.tomcat.remoteip.protocol-header=X-Forwarded-Proto`
  is set in `application.properties` so Spring correctly detects HTTPS.

### 4d. Security Headers

- Verify that security headers are not inadvertently weakened. The current
  config sets `frameOptions.sameOrigin()` which is acceptable for Thymeleaf
  pages that embed same-origin iframes; `DENY` is more restrictive if iframes
  are not needed.

### 4e. General Checks

- Confirm `formLogin` and `logout` are properly configured.
- Verify `authorizeHttpRequests` rules have not become overly permissive (e.g.,
  `permitAll()` on sensitive endpoints).
- Check that `PasswordEncoder` is BCrypt (or Argon2); never downgrade.

---

## Step 5 -- Add / Update JUnit 5 Regression Tests

For any changed behavior, add or update tests under
`src/test/java/com/example/bankapp/`.

### What to test

| Change area            | Test to add/update                                     |
|------------------------|--------------------------------------------------------|
| CSRF re-enabled        | `POST` without CSRF token returns `403 Forbidden`.     |
| Dependency relocated   | Smoke test that the application context loads.          |
| Security config change | Authenticated vs. unauthenticated access assertions.   |
| Java version bump      | Ensure existing tests still compile and pass.           |

### Example: CSRF enforcement test

```java
@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void postWithoutCsrfTokenShouldReturn403() throws Exception {
        mockMvc.perform(post("/register")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "test")
                        .param("password", "test"))
                .andExpect(status().isForbidden());
    }
}
```

### Guidelines

- Use JUnit 5 (`org.junit.jupiter.api.Test`).
- Use `@WebMvcTest` or `@SpringBootTest` as appropriate.
- Mock database dependencies with `@MockBean` to keep tests fast.
- Run `mvn verify -DskipTests=false` after adding tests to confirm they pass.

---

## Step 6 -- Structure the Pull Request

### Branch naming

```
devin/<timestamp>-spring-boot-dep-upgrade
```

### PR title

```
fix: upgrade Spring Boot dependencies and harden SecurityConfig
```

### PR body must include

1. **Summary** -- one-paragraph description of what changed and why.

2. **Dependency change table**

   | Artifact                          | Old Version | New Version | Reason                                    |
   |-----------------------------------|-------------|-------------|-------------------------------------------|
   | `spring-boot-starter-parent`      | X.Y.Z       | X.Y.Z'      | Latest patch / minor                      |
   | `mysql:mysql-connector-java`      | 8.0.33      | *(removed)* | Replaced by `com.mysql:mysql-connector-j` |
   | `com.mysql:mysql-connector-j`     | --          | BOM-managed | New GAV; version managed by BOM           |
   | `maven-compiler-plugin`           | 3.8.0       | BOM/latest  | Aligned source/target to Java 17          |

3. **Build verification output** -- paste the final `mvn verify` summary
   (`BUILD SUCCESS`, test count, etc.).

4. **Security review notes** -- list what was checked in SecurityConfig and any
   fixes applied (e.g., "Re-enabled CSRF protection").

5. **Manual review items** -- flag anything that needs human judgment:
   - Breaking API changes in upgraded libraries.
   - Thymeleaf templates that may need CSRF token adjustments.
   - Infrastructure config changes (e.g., `application.properties` for HTTPS).

---

## Quick-Reference Checklist

- [ ] Spring Boot parent version evaluated and updated if needed.
- [ ] Every dependency checked against BOM -- no unnecessary version overrides.
- [ ] Deprecated GAVs replaced (e.g., `mysql-connector-java` -> `mysql-connector-j`).
- [ ] `maven-compiler-plugin` source/target/release matches `java.version`.
- [ ] `mvn clean verify -DskipTests=false` passes.
- [ ] `SecurityConfig.java` reviewed for CSRF, CORS, HTTPS, headers.
- [ ] Regression tests added/updated for changed behavior.
- [ ] PR structured with dependency table, build output, and manual review items.
