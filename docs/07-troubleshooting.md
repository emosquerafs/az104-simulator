# Troubleshooting

This document covers common issues, their causes, and solutions. If you encounter a problem not listed here, check the [GitHub Issues](https://github.com/singularit/az104simulator/issues) or open a new one.

---

## Encoding & Localization Issues

### Accents Show as "�" or Weird Characters

**Symptoms**:
- Spanish text displays: "¿Qué es Azure?" as "¿QuÃ© es Azure?"
- Characters like é, ñ, ü render as �, �, �

**Root Cause**:
- File encoding mismatch (ISO-8859-1 vs UTF-8)
- Browser not interpreting UTF-8
- Database character set issues

**Solutions**:

1. **Verify application.yml**:
   ```yaml
   spring:
     messages:
       encoding: UTF-8
   ```

2. **Check file encoding**:
   ```bash
   file -I src/main/resources/messages_es.properties
   # Should show: charset=utf-8
   ```

3. **Convert if needed**:
   ```bash
   iconv -f ISO-8859-1 -t UTF-8 messages_es.properties > messages_es_utf8.properties
   mv messages_es_utf8.properties messages_es.properties
   ```

4. **Ensure UTF-8 in Gradle**:
   ```groovy
   tasks.withType(JavaCompile) {
       options.encoding = 'UTF-8'
   }
   ```

5. **Force UTF-8 in JVM**:
   ```bash
   java -Dfile.encoding=UTF-8 -jar app.jar
   ```

6. **Browser meta tag** (already in templates):
   ```html
   <meta charset="UTF-8">
   ```

**Prevention**:
- Always save files as UTF-8 without BOM
- Use UTF-8 aware editors (VS Code, IntelliJ)
- Add `.editorconfig`:
  ```ini
  [*.{java,properties,yml,html}]
  charset = utf-8
  ```

---

## Question Duplication Issues

### Same Question Appears Twice in Session

**Symptoms**:
- User sees identical question at positions 5 and 12
- Session has fewer unique questions than requested

**Root Cause**:
- Bug in question selection logic (should be fixed in V5+ migrations)
- Database constraint not enforced

**Solutions**:

1. **Verify migration V5 applied**:
   ```sql
   SELECT * FROM flyway_schema_history WHERE version = '5';
   ```

2. **Check for unique constraint**:
   ```sql
   SELECT constraint_name, constraint_type 
   FROM information_schema.table_constraints 
   WHERE table_name = 'exam_session_question';
   -- Should show: unique_session_question (UNIQUE)
   ```

3. **Manually remove duplicates**:
   ```sql
   DELETE FROM exam_session_question 
   WHERE id NOT IN (
     SELECT MIN(id) 
     FROM exam_session_question 
     GROUP BY session_id, question_id
   );
   ```

4. **Restart session**:
   - Go back to home
   - Start new exam/practice
   - Should select unique questions now

**Prevention**:
- Ensure Flyway migrations run on startup
- Don't manually modify `exam_session_question` table

---

## Question Order & Navigation Issues

### Questions Change on Page Refresh

**Symptoms**:
- User at "Question 3" refreshes page
- Sees different question text but same index

**Root Cause**:
- Missing `position` field in `attempt_answer` (fixed in V6)
- Unstable ordering query

**Solutions**:

1. **Verify migration V6 applied**:
   ```sql
   SELECT * FROM flyway_schema_history WHERE version = '6';
   ```

2. **Check position column exists**:
   ```sql
   SELECT position FROM attempt_answer LIMIT 1;
   -- Should NOT error
   ```

3. **Verify unique constraint**:
   ```sql
   SHOW INDEXES FROM attempt_answer;
   -- Should show: ux_attempt_answer_attempt_position
   ```

4. **Check service code** uses ordered query:
   ```java
   // Correct:
   findByAttemptOrderByPositionAsc(attempt)
   
   // Wrong:
   findByAttempt(attempt)  // unstable order
   ```

**Prevention**:
- Always use `position` for ordering
- Never rely on `id` order (auto-increment is not guaranteed stable)

---

## Review Screen Issues

### Last Question Shows as Unanswered

**Symptoms**:
- User answers all 50 questions
- Review screen shows: "Answered: 49/50"
- Last question appears unanswered

**Root Cause**:
- Answer not saved before navigation
- JavaScript not calling API before redirect
- Race condition between save and navigate

**Solutions**:

1. **Check JavaScript** in `exam.html`:
   ```javascript
   // Ensure answer is saved before navigation
   async function nextQuestion() {
       await saveAnswer();  // Wait for save
       window.location.href = nextUrl;
   }
   ```

2. **Verify answer saved** (H2 Console):
   ```sql
   SELECT question_id, selected_option_ids_json, answered_at 
   FROM attempt_answer 
   WHERE attempt_id = '<your-attempt-id>' 
   ORDER BY position;
   -- Check if last row has answered_at populated
   ```

3. **Add logging** to debug:
   ```java
   @PostMapping("/{attemptId}/answer")
   public ResponseEntity<?> submitAnswer(...) {
       log.info("Saving answer for attempt={}, question={}, options={}", 
           attemptId, answer.getQuestionId(), answer.getSelectedOptionIds());
       // ...
   }
   ```

4. **Force save on review**:
   - Before clicking "Review All", click "Mark for Review"
   - This triggers a save API call
   - Then click "Review All"

**Prevention**:
- Use `await` for async API calls
- Add "Saving..." indicator before navigation
- Implement optimistic locking (save on every interaction)

---

## Database Issues

### H2 Console Shows "Table Not Found"

**Symptoms**:
- Query `SELECT * FROM question` fails
- Error: "Table QUESTION not found"

**Root Causes**:
1. Wrong JDBC URL (in-memory vs file)
2. Flyway migrations didn't run
3. Wrong database mode

**Solutions**:

1. **Verify JDBC URL matches**:
   ```yaml
   # application.yml
   spring:
     datasource:
       url: jdbc:h2:mem:az104db
   ```
   
   ```
   # H2 Console
   JDBC URL: jdbc:h2:mem:az104db
   ```

2. **Check Flyway ran**:
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   -- Should show V1, V3, V4, V5, V6, V7, V8
   ```

3. **If in-memory**: Database resets on restart
   - Start app: `./gradlew bootRun`
   - Keep app running while using H2 Console

4. **Switch to file mode** (persistent):
   ```yaml
   spring:
     datasource:
       url: jdbc:h2:file:./data/az104db;AUTO_SERVER=TRUE
   ```

5. **Check schema creation**:
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate  # Should be validate, not create
   ```

**Prevention**:
- Document which JDBC URL you're using
- Use file mode for development (persistent)
- Check `flyway_schema_history` after first run

---

### Flyway Migration Fails

**Symptoms**:
- App fails to start
- Error: "Migration checksum mismatch"

**Root Cause**:
- Edited an already-applied migration
- Corrupted `flyway_schema_history`

**Solutions**:

1. **Never edit applied migrations**
   - Create new migration instead: `V9__fix_issue.sql`

2. **Reset Flyway** (loses data):
   ```sql
   DROP TABLE flyway_schema_history;
   ```
   Then restart app.

3. **Reset entire database**:
   ```bash
   rm -rf data/  # File mode
   # Or just restart for in-memory mode
   ```

4. **Repair Flyway** (advanced):
   ```bash
   ./gradlew flywayRepair
   ```

**Prevention**:
- Version control migrations
- Never modify applied migrations
- Test migrations in dev before prod

---

## Performance Issues

### Application Slow to Start

**Symptoms**:
- App takes 30+ seconds to start
- High CPU during startup

**Root Causes**:
1. Large question bank (thousands of questions)
2. Hibernate loading all entities eagerly
3. Slow migration (complex SQL)

**Solutions**:

1. **Check migration time**:
   ```sql
   SELECT version, description, execution_time 
   FROM flyway_schema_history 
   ORDER BY installed_rank;
   ```

2. **Optimize entity fetching**:
   ```java
   // Change from EAGER to LAZY
   @OneToMany(fetch = FetchType.LAZY)
   private List<OptionItem> options;
   ```

3. **Add indexes** (V8 should have these):
   ```sql
   CREATE INDEX idx_question_domain ON question(domain);
   CREATE INDEX idx_attempt_completed ON attempt(is_completed);
   ```

4. **Reduce logging**:
   ```yaml
   logging:
     level:
       org.hibernate: WARN
       org.springframework: INFO
   ```

5. **Increase JVM heap**:
   ```bash
   JAVA_OPTS="-Xmx1g" ./gradlew bootRun
   ```

**Prevention**:
- Use lazy loading where possible
- Index frequently queried columns
- Profile with JProfiler/YourKit if needed

---

### Queries Are Slow

**Symptoms**:
- Loading question takes 5+ seconds
- Review screen hangs

**Root Cause**:
- Missing indexes
- N+1 query problem
- Large result sets

**Solutions**:

1. **Enable SQL logging**:
   ```yaml
   spring:
     jpa:
       show-sql: true
   ```
   Look for repeated queries (N+1 problem).

2. **Add @Query with JOIN FETCH**:
   ```java
   @Query("SELECT q FROM Question q LEFT JOIN FETCH q.options WHERE q.id = :id")
   Optional<Question> findByIdWithOptions(@Param("id") Long id);
   ```

3. **Check indexes exist**:
   ```sql
   SHOW INDEXES FROM attempt_answer;
   -- Should see: idx_attempt_answer_attempt_id, idx_attempt_answer_attempt_position
   ```

4. **Limit result sets**:
   ```java
   // Don't load all questions at once
   List<Question> findTop50ByDomain(Domain domain);
   ```

**Prevention**:
- Use `JOIN FETCH` for common queries
- Profile queries in dev
- Add indexes for foreign keys

---

## Session & Cookie Issues

### Language Doesn't Persist

**Symptoms**:
- User selects "English"
- Page reloads in Spanish

**Root Cause**:
- Cookie not set
- Browser blocking cookies
- Wrong cookie domain

**Solutions**:

1. **Check cookie in browser**:
   - Open DevTools → Application → Cookies
   - Look for `lang=en` or `lang=es`

2. **Verify cookie code**:
   ```java
   Cookie cookie = new Cookie("lang", locale);
   cookie.setMaxAge(365 * 24 * 60 * 60); // 1 year
   cookie.setPath("/");
   response.addCookie(cookie);
   ```

3. **Check browser settings**:
   - Ensure cookies enabled
   - Not in incognito mode (cookies lost on close)

4. **Verify LocaleInterceptor**:
   ```java
   @Override
   public boolean preHandle(HttpServletRequest request, ...) {
       String lang = getCookieValue(request, "lang");
       if (lang != null) {
           LocaleContextHolder.setLocale(new Locale(lang));
       }
       return true;
   }
   ```

**Prevention**:
- Test in multiple browsers
- Document cookie requirements
- Provide fallback (query param `?lang=en`)

---

### Student ID Not Tracking

**Symptoms**:
- Attempt history shows attempts from different "students"
- Can't find past attempts

**Root Cause**:
- Cookie lost (cleared or expired)
- Different browser/device
- Incognito mode

**Solutions**:

1. **Check student_id cookie**:
   - DevTools → Application → Cookies
   - Look for `student_id=<uuid>`

2. **Manually set student_id**:
   - Set cookie in browser console:
     ```javascript
     document.cookie = "student_id=test-user-123; path=/; max-age=31536000";
     ```

3. **Filter attempts by date**:
   ```sql
   SELECT * FROM attempt 
   WHERE started_at > '2024-01-01' 
   ORDER BY started_at DESC;
   ```

**Current Limitation**:
- No authentication means student_id is device-specific
- Clearing cookies loses tracking

**Prevention**:
- Document that history is cookie-based
- Consider adding authentication (future feature)

---

## Build & Deployment Issues

### Gradle Build Fails

**Symptoms**:
- `./gradlew build` errors
- Dependency resolution failure

**Solutions**:

1. **Refresh dependencies**:
   ```bash
   ./gradlew clean build --refresh-dependencies
   ```

2. **Check Java version**:
   ```bash
   java -version
   # Should be Java 21
   ```

3. **Clear Gradle cache**:
   ```bash
   rm -rf ~/.gradle/caches
   ./gradlew build
   ```

4. **Check proxy settings** (if behind firewall):
   ```properties
   # gradle.properties
   systemProp.http.proxyHost=proxy.example.com
   systemProp.http.proxyPort=8080
   ```

---

### Docker Build Fails

**Symptoms**:
- `docker build` fails
- Package version errors

**Solutions**:

1. **Use latest base image**:
   ```dockerfile
   FROM eclipse-temurin:21.0.5_11-jdk-alpine
   ```

2. **Clear Docker cache**:
   ```bash
   docker build --no-cache -t az104-simulator .
   ```

3. **Check Docker daemon running**:
   ```bash
   docker info
   ```

4. **Verify Dockerfile syntax**:
   - Use Docker extension in VS Code
   - Run `hadolint Dockerfile`

---

## Production Issues

### Application Crashes with OOM

**Symptoms**:
- App crashes with "OutOfMemoryError"
- High memory usage

**Solutions**:

1. **Increase heap size**:
   ```bash
   java -Xmx2g -jar app.jar
   ```

2. **Use container-aware JVM**:
   ```bash
   java -XX:MaxRAMPercentage=75.0 -jar app.jar
   ```

3. **Profile memory**:
   ```bash
   java -XX:+HeapDumpOnOutOfMemoryError -jar app.jar
   ```

4. **Check for memory leaks**:
   - Review entity fetch strategies
   - Close streams and connections
   - Use pagination for large result sets

---

### Questions Don't Load

**Symptoms**:
- Home page shows "0 questions"
- Config page empty

**Root Cause**:
- No questions in database
- Migration failed

**Solutions**:

1. **Check question count**:
   ```sql
   SELECT COUNT(*) FROM question;
   ```

2. **Import questions**:
   ```bash
   curl -X POST http://localhost:8080/admin/import \
     -F "file=@questions.json"
   ```

3. **Check migrations**:
   ```sql
   SELECT * FROM flyway_schema_history;
   ```

---

## Getting Help

If none of these solutions work:

1. **Check logs**:
   ```bash
   tail -f app.log
   ```

2. **Enable debug logging**:
   ```yaml
   logging:
     level:
       co.singularit.az104simulator: DEBUG
   ```

3. **Search GitHub Issues**: [github.com/singularit/az104simulator/issues](https://github.com/singularit/az104simulator/issues)

4. **Open new issue** with:
   - Stack trace
   - Steps to reproduce
   - Environment (OS, Java version, etc.)
   - Relevant logs

---

**Remember**: 90% of issues are encoding problems, missing migrations, or JDBC URL mismatches. Check those first before diving deep. 🔍

