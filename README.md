# AZ-104 Exam Simulator

A comprehensive, plug-and-play exam simulator for Microsoft Azure Administrator (AZ-104) certification preparation.

## Overview

This application provides a realistic exam simulation experience with two modes:
- **Practice Mode**: Study at your own pace with immediate feedback
- **Exam Mode**: Simulate the real exam with time limits and review screens

## Features

- ✅ **150+ Original Questions** (expandable to 500+ by editing JSON)
- ✅ **Multiple Question Types**: Single choice, Multiple choice, Yes/No
- ✅ **5 Domain Coverage**: Identity & Governance, Storage, Compute, Networking, Monitor & Maintain
- ✅ **Smart Randomization**: Configurable distribution across domains and difficulties
- ✅ **Timed Exams**: Configurable time limits with countdown timer
- ✅ **Mark for Review**: Flag questions to revisit later
- ✅ **Detailed Results**: Score breakdown by domain with explanations
- ✅ **Admin Tools**: Import/Export question banks via JSON

## Technology Stack

- **Backend**: Spring Boot 3.4.1, Java 21
- **Database**: H2 (in-memory)
- **Migration**: Flyway
- **Frontend**: Thymeleaf, HTML5, CSS3, Vanilla JavaScript
- **Build Tool**: Gradle 8.11.1

## Prerequisites

- Java 21 or higher
- No external services required (all self-contained)

## Quick Start

### 1. Clone or download this project

```bash
cd Az104Simulator
```

### 2. Run the application

**Using Gradle Wrapper (recommended):**

```bash
./gradlew bootRun
```

**On Windows:**

```cmd
gradlew.bat bootRun
```

### 3. Access the application

Open your browser and navigate to:
```
http://localhost:8080
```

## Project Structure

```
Az104Simulator/
├── src/
│   └── main/
│       ├── java/co/singularit/az104simulator/
│       │   ├── Az104SimulatorApplication.java
│       │   ├── controller/
│       │   │   ├── HomeController.java
│       │   │   ├── ExamController.java
│       │   │   └── AdminController.java
│       │   ├── domain/
│       │   │   ├── Question.java
│       │   │   ├── OptionItem.java
│       │   │   ├── Attempt.java
│       │   │   ├── AttemptAnswer.java
│       │   │   └── [Enums]
│       │   ├── dto/
│       │   ├── repository/
│       │   ├── service/
│       │   └── db/migration/
│       │       └── V2__seed_questions.java
│       └── resources/
│           ├── application.yml
│           ├── db/migration/
│           │   └── V1__schema.sql
│           ├── seed/
│           │   └── questions.json
│           ├── static/css/
│           │   └── style.css
│           └── templates/
│               ├── home.html
│               ├── config.html
│               ├── exam.html
│               ├── review.html
│               └── results.html
├── build.gradle
├── settings.gradle
└── README.md
```

## Usage Guide

### Practice Mode

1. Click **"Start Practice"** on the home page
2. Configure:
   - Number of questions
   - Select specific domains (optional)
   - Enable/disable immediate explanations
3. Answer questions at your own pace
4. See explanations after answering (if enabled)
5. Submit when ready to see results

### Exam Mode

1. Click **"Start Exam"** on the home page
2. Configure:
   - Number of questions (default: 50)
   - Time limit in minutes (default: 100)
   - Domain distribution percentages
3. Answer all questions under time pressure
4. Use "Mark for Review" to flag uncertain questions
5. Click "Review All" to see your progress
6. Submit the exam to see results

### Navigation

- **Sidebar**: Click any question number to jump directly to that question
- **Previous/Next**: Navigate sequentially through questions
- **Mark**: Flag questions for later review
- **Review Screen**: See all questions with their status before submitting

### Results

After submitting, you'll see:
- **Overall Score**: Percentage and pass/fail status (70% passing)
- **Domain Breakdown**: Performance in each domain area
- **Detailed Review**: Each question with:
  - Your answer vs. correct answer
  - Explanation for the correct answer
  - Visual indicators for correct/incorrect

## Expanding the Question Bank

### Current Bank

The application ships with ~150 questions. To expand to 500+:

### Method 1: Edit JSON Directly

1. Edit `src/main/resources/seed/questions.json`
2. Add more questions following the existing format:

```json
{
  "domain": "COMPUTE",
  "difficulty": "MEDIUM",
  "qtype": "SINGLE",
  "stem": "Your question text here",
  "explanation": "Detailed explanation here",
  "options": [
    {"label": "A", "text": "Option A", "isCorrect": false},
    {"label": "B", "text": "Option B", "isCorrect": true},
    {"label": "C", "text": "Option C", "isCorrect": false},
    {"label": "D", "text": "Option D", "isCorrect": false}
  ],
  "tags": ["vm", "compute", "scaling"]
}
```

3. Restart the application (Flyway will reload on startup)

### Method 2: Use Admin Import/Export

**Export current bank:**
```
GET http://localhost:8080/admin/export
```
Downloads `questions.json`

**Import expanded bank:**
```
POST http://localhost:8080/admin/import
Content-Type: multipart/form-data
file: questions.json
```

**Using curl:**
```bash
# Export
curl http://localhost:8080/admin/export -o questions.json

# Import
curl -X POST http://localhost:8080/admin/import \
  -F "file=@questions.json"
```

### Question Format Guidelines

- **Domain**: One of `IDENTITY_GOVERNANCE`, `STORAGE`, `COMPUTE`, `NETWORKING`, `MONITOR_MAINTAIN`
- **Difficulty**: `EASY`, `MEDIUM`, `HARD`
- **Question Type**:
  - `SINGLE`: One correct answer
  - `MULTI`: Multiple correct answers
  - `YESNO`: Yes/No question
- **Stem**: The question text (can include HTML for formatting)
- **Explanation**: Clear, technical explanation (2-4 sentences)
- **Options**: 2-4 choices with one or more correct
- **Tags**: Keywords for future filtering/search

## Configuration

### Database

H2 in-memory database (resets on restart):
- Console: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:az104db`
- Username: `sa`
- Password: (empty)

### Application Settings

Edit `src/main/resources/application.yml`:

```yaml
server:
  port: 8080  # Change port if needed

spring:
  flyway:
    enabled: true  # Disable to prevent auto-migration
  h2:
    console:
      enabled: true  # Disable in production
```

## Building for Deployment

### Create executable JAR

```bash
./gradlew clean build
```

The JAR will be in `build/libs/az104-simulator-1.0.0.jar`

### Run the JAR

```bash
java -jar build/libs/az104-simulator-1.0.0.jar
```

### Docker (Optional)

Create `Dockerfile`:

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/az104-simulator-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:

```bash
./gradlew clean build
docker build -t az104-simulator .
docker run -p 8080:8080 az104-simulator
```

## Troubleshooting

### Application won't start

**Port already in use:**
```
Change port in application.yml or:
java -jar app.jar --server.port=8081
```

**Java version mismatch:**
```
Verify Java 21: java -version
Update JAVA_HOME if needed
```

### Questions not loading

**Check Flyway migrations:**
```
Look for migration errors in logs
Verify questions.json is valid JSON
```

**Validate JSON:**
```bash
# Use jq or online validator
cat src/main/resources/seed/questions.json | jq .
```

### Database issues

**Clear H2 database:**
Database is in-memory, just restart the application.

## Development

### IDE Setup

**IntelliJ IDEA:**
1. Open `build.gradle` as project
2. Enable Lombok annotation processing
3. Set Java SDK to 21

**VS Code:**
1. Install "Extension Pack for Java"
2. Open folder
3. Gradle tasks will auto-detect

### Hot Reload

Spring Boot DevTools is included for development:
- Templates auto-reload on save
- Static resources (CSS) auto-reload
- Java changes require rebuild

### Adding New Features

**New question types (e.g., MATCHING, ORDERING):**
1. Update `QuestionType` enum
2. Modify `exam.html` template for UI
3. Update scoring logic in `ScoringService`
4. Add questions to JSON with new type

**Custom domains:**
1. Add to `Domain` enum with display name
2. Update question distribution logic
3. Add questions for new domain

## Performance

- **Startup**: ~5-10 seconds (includes question loading)
- **Memory**: ~200-300 MB RAM
- **Database**: In-memory (resets on restart)
- **Concurrent Users**: Single-user design (local study tool)

## Security Notes

- **No authentication**: Designed for local use only
- **Admin endpoints**: No protection - do not expose publicly
- **H2 Console**: Disable in production environments
- **Data persistence**: All data lost on restart (in-memory DB)

## License

This project is for educational purposes only. All questions are original and not copied from official Microsoft exams.

## Contributing

To contribute questions:
1. Export current bank via `/admin/export`
2. Add your questions following the format
3. Test locally with import
4. Submit JSON with your additions

## Credits

Built with:
- Spring Boot
- Thymeleaf
- H2 Database
- Flyway
- Jackson
- Lombok

---

**Good luck with your AZ-104 certification! 🎓**
