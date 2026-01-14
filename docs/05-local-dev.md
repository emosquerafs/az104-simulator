# Local Development

This guide will get you up and running with the AZ-104 Simulator on your local machine.

## Prerequisites

You need these installed:

| Tool | Minimum Version | Check Command |
|------|----------------|---------------|
| **Java (JDK)** | 21+ | `java -version` |
| **Gradle** | 8.x (or use wrapper) | `gradle --version` |
| **Git** | Any recent | `git --version` |
| **Docker** (optional) | Any recent | `docker --version` |

### Installing Java 21

**macOS** (Homebrew):
```bash
brew install openjdk@21
```

**Linux** (Ubuntu/Debian):
```bash
sudo apt update
sudo apt install openjdk-21-jdk
```

**Windows**:
Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/#java21).

## Quick Start

### 1. Clone the Repository

```bash
git clone <repository-url>
cd Az104Simulator
```

### 2. Run with Gradle

```bash
./gradlew bootRun
```

**Windows**:
```cmd
gradlew.bat bootRun
```

### 3. Open Browser

Navigate to: **http://localhost:8080**

You should see the home page with question statistics.

---

## Configuration

### Application Properties

**File**: `src/main/resources/application.yml`

Key settings:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:az104db              # In-memory database
    # url: jdbc:h2:file:./data/az104db    # File-based (persistent)
    username: sa
    password:
  
  h2:
    console:
      enabled: true                        # Enable H2 Console
      path: /h2-console
  
  jpa:
    hibernate:
      ddl-auto: validate                   # Flyway handles schema
    show-sql: false                        # Set true to see SQL logs
```

### Environment Variables

You can override properties via env vars:

```bash
export SPRING_PROFILES_ACTIVE=dev
export APP_LOCALE_DEFAULT=es
./gradlew bootRun
```

Or pass as Java system properties:

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

---

## Database Access

### H2 Console

Access the built-in database console:

1. Run the application: `./gradlew bootRun`
2. Open browser: **http://localhost:8080/h2-console**
3. Enter connection details:
   - **JDBC URL**: `jdbc:h2:mem:az104db` (or `file:./data/az104db` if using file mode)
   - **Username**: `sa`
   - **Password**: (leave empty)
4. Click **Connect**

You can now run SQL queries directly:

```sql
SELECT * FROM question;
SELECT * FROM attempt WHERE is_completed = true;
SELECT * FROM exam_session_question WHERE session_id = '<your-session-id>';
```

### Switching to File-Based Database

To persist data across restarts, edit `application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/az104db;AUTO_SERVER=TRUE
```

This creates a `data/` directory with `az104db.mv.db` file.

**Protip**: Add `data/` to `.gitignore` to avoid committing database files.

---

## Running Tests

### All Tests

```bash
./gradlew test
```

### Specific Test Class

```bash
./gradlew test --tests "ExamSessionIntegrationTest"
```

### With Coverage

```bash
./gradlew test jacocoTestReport
```

Coverage report: `build/reports/jacoco/test/html/index.html`

### Test Configuration

**File**: `src/test/resources/application-test.yml`

Tests use in-memory H2 and reset between runs.

---

## Building

### Build JAR

```bash
./gradlew bootJar
```

Output: `build/libs/Az104Simulator-1.0.0.jar`

### Run the JAR

```bash
java -jar build/libs/Az104Simulator-1.0.0.jar
```

### Clean Build

```bash
./gradlew clean build
```

---

## Docker Workflow

### Build Image

```bash
docker build -t az104-simulator:latest .
```

### Run Container

```bash
docker run -d \
  --name az104-simulator \
  -p 8080:8080 \
  az104-simulator:latest
```

### Access Application

**App**: http://localhost:8080

**H2 Console**: http://localhost:8080/h2-console

### Stop Container

```bash
docker stop az104-simulator
docker rm az104-simulator
```

### Using Docker Compose

```bash
# Start
docker compose up -d

# View logs
docker compose logs -f

# Stop
docker compose down
```

### Using Makefile

```bash
# Build and run
make all

# Run with hardening
make run-hardened

# View logs
make logs

# Stop
make stop
```

See `Makefile` for all available commands.

---

## Database Migrations

### How Flyway Works

1. On startup, Flyway scans `src/main/resources/db/migration/`
2. Applies migrations in version order: `V1__`, `V2__`, `V3__`, etc.
3. Records applied migrations in `flyway_schema_history` table
4. Never re-applies already-applied migrations

### Adding a New Migration

1. Create file: `src/main/resources/db/migration/V9__your_description.sql`
2. Write SQL:
   ```sql
   ALTER TABLE question ADD COLUMN new_field VARCHAR(100);
   ```
3. Restart app → Flyway applies it automatically

### Checking Migration Status

Query the history table:

```sql
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

### Resetting the Database

**In-memory mode**: Just restart the app (data lost).

**File mode**: Delete the database file:

```bash
rm -rf data/
./gradlew bootRun
```

Flyway will recreate schema from scratch.

**Warning**: This deletes all data (questions, attempts, sessions).

---

## Importing Questions

### Via Admin Endpoint

1. Prepare `questions.json` file (see format below)
2. Run application
3. POST to `/admin/import`:

```bash
curl -X POST http://localhost:8080/admin/import \
  -H "Content-Type: multipart/form-data" \
  -F "file=@questions.json"
```

### Question JSON Format

```json
[
  {
    "domain": "COMPUTE",
    "difficulty": "MEDIUM",
    "qtype": "SINGLE",
    "stem": "What is a Virtual Machine?",
    "explanation": "A VM is...",
    "tags": ["VMs", "Compute"],
    "options": [
      {
        "label": "A",
        "text": "A physical server",
        "isCorrect": false
      },
      {
        "label": "B",
        "text": "A virtualized compute resource",
        "isCorrect": true
      }
    ]
  }
]
```

### Bilingual Questions

Use nested objects for `stem`, `explanation`, and option `text`:

```json
{
  "domain": "STORAGE",
  "difficulty": "EASY",
  "qtype": "SINGLE",
  "stem": {
    "en": "What is Blob Storage?",
    "es": "¿Qué es Blob Storage?"
  },
  "explanation": {
    "en": "Blob storage is object storage for unstructured data like images, videos, and backups.",
    "es": "Blob storage es almacenamiento de objetos para datos no estructurados como imágenes, videos y copias de seguridad."
  },
  "options": [
    {
      "label": "A",
      "text": {
        "en": "A database",
        "es": "Una base de datos"
      },
      "isCorrect": false
    },
    {
      "label": "B",
      "text": {
        "en": "Object storage",
        "es": "Almacenamiento de objetos"
      },
      "isCorrect": true
    }
  ],
  "tags": ["Storage", "Blobs"]
}
```

### Exporting Questions

Download all questions as JSON:

```bash
curl http://localhost:8080/admin/export -o questions.json
```

---

## Troubleshooting

### Port 8080 Already in Use

**Error**: "Port 8080 is already in use"

**Solution**: Change port in `application.yml`:

```yaml
server:
  port: 8081
```

Or via environment variable:

```bash
SERVER_PORT=8081 ./gradlew bootRun
```

### Flyway Migration Failed

**Error**: "Migration checksum mismatch"

**Cause**: You edited an already-applied migration.

**Solution**:
- **Never** edit applied migrations
- Create a new migration (e.g., `V9__fix_previous.sql`)
- Or reset database (loses data):
  ```bash
  rm -rf data/
  ```

### Gradle Build Fails

**Error**: "Could not resolve dependencies"

**Solution**:
```bash
./gradlew clean build --refresh-dependencies
```

### H2 Console Not Working

**Symptom**: 404 error at `/h2-console`

**Solution**: Verify `application.yml`:

```yaml
spring:
  h2:
    console:
      enabled: true
```

Restart app.

### Question Text Shows "�" (Mojibake)

**Cause**: UTF-8 encoding issue

**Solution**:
1. Verify `application.yml`:
   ```yaml
   spring:
     messages:
       encoding: UTF-8
   ```
2. Check file encoding: Files must be UTF-8
3. For JSON import, ensure:
   ```bash
   file -I questions.json
   # Should show: charset=utf-8
   ```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

1. **Import Project**: File → Open → Select `build.gradle`
2. **Trust Gradle**: Click "Trust Project"
3. **Enable Annotation Processing**: Settings → Build → Compiler → Annotation Processors → Enable
4. **Set JDK**: Project Structure → Project → SDK → 21
5. **Run Configuration**:
   - Main class: `co.singularit.az104simulator.Az104SimulatorApplication`
   - Working directory: `$MODULE_WORKING_DIR$`

### VS Code

1. Install extensions:
   - "Extension Pack for Java" by Microsoft
   - "Spring Boot Extension Pack" by VMware
2. Open project folder
3. Run/Debug from Spring Boot Dashboard

### Eclipse

1. Import → Existing Gradle Project
2. Select project root
3. Finish
4. Right-click project → Run As → Spring Boot App

---

## Hot Reload (DevTools)

Spring Boot DevTools enables automatic restart on code changes.

**Already included** in `build.gradle`:

```groovy
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### How It Works

1. Change a `.java` file
2. Save
3. DevTools detects change
4. Restarts app automatically (fast restart, not full JVM restart)

### Disable DevTools

Set in `application.yml`:

```yaml
spring:
  devtools:
    restart:
      enabled: false
```

---

## Useful Gradle Tasks

| Command | Description |
|---------|-------------|
| `./gradlew bootRun` | Run application |
| `./gradlew build` | Build JAR (with tests) |
| `./gradlew bootJar` | Build executable JAR |
| `./gradlew test` | Run tests |
| `./gradlew clean` | Delete build directory |
| `./gradlew dependencies` | Show dependency tree |
| `./gradlew tasks` | List all available tasks |

---

## Environment Profiles

### Dev Profile

**File**: `src/main/resources/application-dev.yml`

Override for development:

```yaml
spring:
  jpa:
    show-sql: true
  h2:
    console:
      enabled: true
logging:
  level:
    co.singularit.az104simulator: DEBUG
```

**Activate**:
```bash
export SPRING_PROFILES_ACTIVE=dev
./gradlew bootRun
```

### Test Profile

**File**: `src/test/resources/application-test.yml`

Automatically used during tests.

---

## Logs

### Viewing Logs

**Console**: Logs print to stdout by default.

**File**: Configure in `application.yml`:

```yaml
logging:
  file:
    name: app.log
  level:
    co.singularit.az104simulator: DEBUG
    org.springframework.web: INFO
```

### Log Levels

| Level | Usage |
|-------|-------|
| `TRACE` | Very detailed (HTTP requests, SQL) |
| `DEBUG` | Debugging info |
| `INFO` | Normal operations |
| `WARN` | Potential issues |
| `ERROR` | Actual errors |

---

## Next Steps

- Read [Admin & Question Bank](./06-admin-and-question-bank.md) to manage questions
- Check [Troubleshooting](./07-troubleshooting.md) for common issues
- Review [Security Notes](./08-security-notes.md) before deploying

---

**Protip**: Use `make dev` (from Makefile) for the complete development workflow: build → run → tail logs.

