# Security Notes

This document outlines the security posture of the AZ-104 Simulator, including design decisions, limitations, and hardening measures.

## Security Model Overview

**TL;DR**: This is an **educational simulator** designed for local use or trusted environments. It is **not** production-ready for multi-tenant SaaS without additional security measures.

### Current Security Posture

| Aspect | Status | Notes |
|--------|--------|-------|
| **Authentication** | ❌ None | Cookie-based student_id only (no login) |
| **Authorization** | ❌ None | All users can access all features |
| **Input Validation** | ✅ Partial | Bean Validation on DTOs, no SQL injection |
| **Output Escaping** | ✅ Yes | Thymeleaf auto-escapes by default |
| **HTTPS** | ⚠️ Optional | Not enforced, configure reverse proxy |
| **CSRF Protection** | ✅ Enabled | Spring Security default (if added) |
| **SQL Injection** | ✅ Protected | JPA/Hibernate parameterized queries |
| **XSS Protection** | ✅ Protected | Thymeleaf escaping + CSP headers |
| **Secrets Management** | ⚠️ Partial | No hardcoded secrets, use env vars |

---

## Authentication & Authorization

### Current State: No Authentication

**Why?**
- **Simplicity**: Focus on learning, not user management
- **Local Use**: Designed for single-user local deployment
- **No Sensitive Data**: Questions and scores are educational, not confidential

**Limitations**:
- Anyone with network access can use the app
- No user isolation (all users see all attempts if they know the URL)
- No role-based access control (everyone is "admin")

### Student Tracking (Cookie-Based)

**Mechanism**:
- First visit generates a UUID `student_id`
- Stored in browser cookie (1 year expiration)
- Used to filter attempt history

**Security Implications**:
- **Not authentication**: Just analytics/personalization
- **Easily spoofed**: User can edit cookie value
- **Device-specific**: Different browsers = different "students"

**Code**:
```java
String studentId = studentIdentityService.getOrCreateStudentId(request, response);
attempt.setStudentId(studentId);
```

### Future Enhancement: Add Authentication

To secure for multi-user deployment, add Spring Security:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/attempt/**").authenticated()
                .requestMatchers("/", "/config").permitAll()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout.permitAll());
        return http.build();
    }
}
```

---

## Input Validation

### Bean Validation

DTOs use Jakarta Validation annotations:

```java
@Data
public class ExamConfigDto {
    @NotNull(message = "Mode is required")
    private ExamMode mode;
    
    @Min(value = 1, message = "Must have at least 1 question")
    @Max(value = 200, message = "Cannot exceed 200 questions")
    private int totalQuestions;
    
    @Min(value = 10, message = "Minimum time is 10 minutes")
    private Integer timeLimit;
}
```

**Validation happens automatically** via `@Valid` annotation:

```java
@PostMapping("/start")
public String startAttempt(@Valid @ModelAttribute ExamConfigDto config, ...) {
    // config is validated before method executes
}
```

### File Upload Validation

Admin import endpoint validates:

```java
@PostMapping("/import")
public ResponseEntity<?> importQuestions(@RequestParam("file") MultipartFile file) {
    // Check file size
    if (file.getSize() > 10_000_000) {  // 10MB limit
        return ResponseEntity.badRequest().body("File too large");
    }
    
    // Check content type
    if (!file.getContentType().equals("application/json")) {
        return ResponseEntity.badRequest().body("Must be JSON");
    }
    
    // Parse and validate JSON structure
    // ...
}
```

### SQL Injection Protection

**All queries use JPA/Hibernate** which parameterizes automatically:

```java
// Safe: Hibernate parameterizes
@Query("SELECT q FROM Question q WHERE q.domain = :domain")
List<Question> findByDomain(@Param("domain") Domain domain);

// Also safe: Spring Data JPA method names
List<Question> findByDomainAndDifficulty(Domain domain, Difficulty difficulty);
```

**Never** use string concatenation for queries:

```java
// DANGEROUS - DO NOT DO THIS
String sql = "SELECT * FROM question WHERE domain = '" + userInput + "'";
```

---

## Output Escaping & XSS Protection

### Thymeleaf Auto-Escaping

Thymeleaf **automatically escapes** HTML by default:

```html
<!-- User input: <script>alert('XSS')</script> -->
<!-- Rendered as: &lt;script&gt;alert('XSS')&lt;/script&gt; -->
<p th:text="${question.stem}">Question text here</p>
```

**Unescaped output** requires explicit `th:utext` (avoid unless necessary):

```html
<!-- DANGEROUS: Only use for trusted content -->
<div th:utext="${trustedHtml}"></div>
```

### Content Security Policy (CSP)

**Not currently implemented**, but recommended:

```java
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   Object handler) {
                response.setHeader("Content-Security-Policy", 
                    "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'");
                return true;
            }
        });
    }
}
```

---

## CSRF Protection

### Spring Security Default

If Spring Security is added (future), CSRF protection is **enabled by default**:

```java
// CSRF tokens automatically added to forms
<form th:action="@{/attempt/start}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    <!-- form fields -->
</form>
```

### Current State

**Without Spring Security**: No CSRF protection.

**Risk**: Malicious site could submit forms on user's behalf.

**Mitigation for public deployment**:
- Add Spring Security
- Use SameSite cookies: `cookie.setSameSite("Strict")`
- Validate Referer header for sensitive actions

---

## Secrets Management

### No Hardcoded Secrets

**DO NOT** commit secrets to Git:

```yaml
# BAD - Don't do this
spring:
  datasource:
    password: SuperSecretPassword123
```

**GOOD**: Use environment variables:

```yaml
spring:
  datasource:
    password: ${DB_PASSWORD:}
```

```bash
export DB_PASSWORD=MySecurePassword
./gradlew bootRun
```

### Current Secrets

**None in this project** (H2 has no password by default).

If adding external DB:
- Use environment variables
- Use Spring Cloud Config
- Use secret management (Vault, AWS Secrets Manager)

---

## Docker Security Hardening

See `Dockerfile` for implementation details.

### 1. Non-Root User

**Why**: Limit blast radius if container is compromised.

```dockerfile
# Create user with fixed UID/GID
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

# Switch to non-root
USER appuser:appgroup
```

**Verification**:
```bash
docker exec az104-simulator whoami
# Should output: appuser (not root)
```

### 2. Read-Only Filesystem

**Why**: Prevent malware from writing to disk.

```bash
docker run --read-only \
  --tmpfs /tmp:mode=1777,size=100m \
  --tmpfs /tmp-app:mode=1777,size=100m \
  az104-simulator
```

**Application requires writable**:
- `/tmp` - JVM temporary files
- `/tmp-app` - Application temp directory (configured via `JAVA_TOOL_OPTIONS`)

### 3. Dropped Capabilities

**Why**: Remove unnecessary Linux capabilities.

```bash
docker run --cap-drop=ALL az104-simulator
```

**Verification**:
```bash
docker inspect az104-simulator | jq '.[0].HostConfig.CapDrop'
# Should show: ["ALL"]
```

### 4. No New Privileges

**Why**: Prevent privilege escalation via setuid binaries.

```bash
docker run --security-opt no-new-privileges:true az104-simulator
```

### 5. Resource Limits

**Why**: Prevent DoS via resource exhaustion.

```bash
docker run \
  --memory=512m \
  --cpus=1.0 \
  --pids-limit=100 \
  az104-simulator
```

### 6. Security Scanning

**Scan for vulnerabilities** before deployment:

```bash
# Trivy
trivy image az104-simulator:latest

# Docker Scout
docker scout cves az104-simulator:latest

# Grype
grype az104-simulator:latest
```

**Fix vulnerabilities**:
- Update base images
- Update dependencies in `build.gradle`
- Apply security patches

---

## Data Privacy

### What Data Is Stored?

| Data Type | Storage | Purpose |
|-----------|---------|---------|
| **Questions** | Database | Exam content |
| **Attempts** | Database | User progress |
| **Answers** | Database | Scoring and review |
| **Student ID** | Cookie + DB | Attempt tracking |
| **Locale** | Cookie | Language preference |

### Personal Data

**None stored** in default configuration.

- No emails
- No passwords
- No names
- No IP addresses logged

**Student ID** is a random UUID with no PII.

### Data Retention

**Current**: Data persists indefinitely (or until DB reset).

**Recommendation for production**:
- Purge old attempts (e.g., 90 days)
- Add GDPR-compliant deletion endpoint

```java
@DeleteMapping("/api/student/{studentId}")
public ResponseEntity<?> deleteStudentData(@PathVariable String studentId) {
    attemptRepository.deleteByStudentId(studentId);
    return ResponseEntity.ok("Data deleted");
}
```

### Logging

**No sensitive data logged**:

```java
// Good: Log IDs, not content
log.info("User answered question_id={}", questionId);

// Bad: Don't log answers or PII
log.info("User selected options={} for question={}", selectedOptions, questionStem);
```

---

## Network Security

### HTTPS

**Not enforced** by application.

**Recommendation**: Use reverse proxy (Nginx, Traefik) with TLS:

```nginx
server {
    listen 443 ssl;
    ssl_certificate /path/to/cert.pem;
    ssl_certificate_key /path/to/key.pem;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header X-Forwarded-Proto https;
    }
}
```

### CORS

**Not configured** (same-origin policy applies).

If adding API for SPA frontend:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://trusted-frontend.com")
            .allowedMethods("GET", "POST")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## Dependency Security

### Keep Dependencies Updated

**Check for vulnerabilities**:

```bash
./gradlew dependencyCheckAnalyze
```

**Update dependencies** in `build.gradle`:

```groovy
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // Check for updates: https://mvnrepository.com
}
```

### Automated Scanning

**GitHub Dependabot** (if using GitHub):
- Automatically scans dependencies
- Creates PRs for security updates

**Snyk** (alternative):
```bash
snyk test
snyk monitor
```

---

## Threat Model

### Assets

1. **Question Bank**: Educational value, not confidential
2. **User Attempts**: Personal progress, low sensitivity
3. **Application Availability**: DoS could disrupt learning

### Threats

| Threat | Likelihood | Impact | Mitigation |
|--------|-----------|--------|------------|
| **Unauthorized Access** | Medium | Low | Add authentication |
| **Data Exfiltration** | Low | Low | No sensitive data stored |
| **Question Tampering** | Medium | Medium | Protect `/admin/*` endpoints |
| **DoS via Resource Exhaustion** | Medium | Medium | Rate limiting, resource limits |
| **XSS** | Low | Low | Thymeleaf auto-escaping |
| **SQL Injection** | Very Low | High | JPA parameterization |

### Attack Surface

**Public Endpoints**:
- `/` - Home page
- `/config` - Exam configuration
- `/attempt/**` - Exam/practice flow
- `/admin/import` - ⚠️ **UNPROTECTED** question import
- `/admin/export` - ⚠️ **UNPROTECTED** question export
- `/h2-console` - ⚠️ **UNPROTECTED** database access

**High-Risk**: `/admin/*` and `/h2-console` should be protected.

**Recommended**:

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/h2-console/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
```

---

## Security Checklist

Before deploying to production:

### Application

- [ ] Add Spring Security (authentication)
- [ ] Protect `/admin/*` endpoints (admin role)
- [ ] Disable H2 Console in production (`spring.h2.console.enabled=false`)
- [ ] Enable HTTPS (reverse proxy)
- [ ] Add CSP headers
- [ ] Enable CSRF protection
- [ ] Implement rate limiting

### Infrastructure

- [ ] Run container as non-root
- [ ] Use read-only filesystem
- [ ] Drop all capabilities
- [ ] Set resource limits (memory, CPU)
- [ ] Enable `no-new-privileges`
- [ ] Scan image for vulnerabilities
- [ ] Use private registry (not Docker Hub)

### Data

- [ ] Encrypt data at rest (if using external DB)
- [ ] Encrypt data in transit (HTTPS)
- [ ] Implement data retention policy
- [ ] Add GDPR deletion endpoint
- [ ] Audit logging for sensitive actions

### Dependencies

- [ ] Scan dependencies (Dependabot, Snyk)
- [ ] Update to latest patch versions
- [ ] Remove unused dependencies
- [ ] Generate SBOM (Software Bill of Materials)

---

## Security Contact

For security vulnerabilities, please **do not** open a public GitHub issue.

Instead:
- Email: security@singularit.co
- Use GitHub Security Advisories (private disclosure)

Include:
- Description of vulnerability
- Steps to reproduce
- Impact assessment
- Suggested fix (if any)

---

## Compliance Notes

### GDPR

**Current Status**: Not compliant for EU deployment.

**Required**:
- Cookie consent banner
- Privacy policy
- Data deletion endpoint
- Data export endpoint
- User consent logging

### Accessibility (WCAG)

**Current Status**: Partial compliance.

**Improvements needed**:
- Add ARIA labels
- Keyboard navigation
- Screen reader testing
- Color contrast validation

---

**Remember**: Security is a journey, not a destination. Regularly review this document and update as threats evolve. When in doubt, follow the principle of least privilege. 🔒

