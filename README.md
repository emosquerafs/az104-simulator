# AZ-104 Exam Simulator

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

> **Practice for the Microsoft Azure Administrator (AZ-104) certification exam with realistic questions and exam simulations.**

## 🎯 What Is This?

The AZ-104 Exam Simulator is a web-based practice tool that helps you prepare for the Microsoft Azure Administrator certification exam. Built with Spring Boot and designed for simplicity, it offers two learning modes:

- **Practice Mode**: Study at your own pace with immediate feedback and explanations
- **Exam Mode**: Simulate the real exam with time limits and no hints

## ✨ Key Features

- ✅ **Bilingual Support**: Full English and Spanish interface and questions
- ✅ **No Question Duplication**: Guaranteed unique questions per session via database constraints
- ✅ **Comprehensive History**: Review all past attempts with detailed analytics
- ✅ **Domain-Based Practice**: Filter by Azure domains (Compute, Networking, Storage, etc.)
- ✅ **Multiple Question Types**: Single choice, multiple choice, and Yes/No questions
- ✅ **Docker Ready**: Production-ready containerization with security hardening
- ✅ **Zero Setup**: Runs with embedded H2 database (or file-based for persistence)

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Gradle (or use included wrapper)
- Docker (optional)

### Run Locally

```bash
# Clone the repository
git clone <repository-url>
cd Az104Simulator

# Run with Gradle
./gradlew bootRun

# Open browser
open http://localhost:8080
```

### Run with Docker

```bash
# Build image
docker build -t az104-simulator:latest .

# Run container
docker run -d --name az104-simulator -p 8080:8080 az104-simulator:latest

# Or use Docker Compose
docker compose up -d
```

### Run with Makefile

```bash
# Show all commands
make help

# Build and run with security hardening
make all

# View logs
make logs

# Stop
make stop
```

## 📚 Documentation

Comprehensive documentation is available in the `/docs` folder:

| Document | Description |
|----------|-------------|
| [**01-overview.md**](./docs/01-overview.md) | What the simulator does, features, and typical user journey |
| [**02-architecture.md**](./docs/02-architecture.md) | System architecture, components, and design decisions |
| [**03-database.md**](./docs/03-database.md) | Database schema, tables, relationships, and migrations |
| [**04-flows.md**](./docs/04-flows.md) | Detailed user workflows with sequence diagrams |
| [**05-local-dev.md**](./docs/05-local-dev.md) | Local development setup, testing, and troubleshooting |
| [**06-admin-and-question-bank.md**](./docs/06-admin-and-question-bank.md) | Managing questions, import/export, and quality control |
| [**07-troubleshooting.md**](./docs/07-troubleshooting.md) | Common issues and solutions |
| [**08-security-notes.md**](./docs/08-security-notes.md) | Security posture, hardening, and best practices |
| [**diagrams.md**](./docs/diagrams.md) | All Mermaid diagrams (ER, sequence, component) |

### Quick Links

- **New to the project?** Start with [Overview](./docs/01-overview.md)
- **Want to understand how it works?** Check [Architecture](./docs/02-architecture.md)
- **Need to run it locally?** See [Local Development](./docs/05-local-dev.md)
- **Having issues?** Browse [Troubleshooting](./docs/07-troubleshooting.md)
- **Want visual diagrams?** View [Diagrams](./docs/diagrams.md)

## 🐳 Docker

The project includes production-ready Docker configuration with DevSecOps best practices:

- ✅ Multi-stage build (builder + runtime)
- ✅ Non-root user (UID/GID 1001)
- ✅ Read-only filesystem support
- ✅ Security hardening (dropped capabilities, no-new-privileges)
- ✅ Resource limits (memory, CPU, PIDs)
- ✅ Health checks
- ✅ Optimized layer caching

See [DOCKER.md](./DOCKER.md) for detailed Docker documentation.

## 🗄️ Database

The simulator uses H2 database with two modes:

**In-Memory** (default, data lost on restart):
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:az104db
```

**File-Based** (persistent):
```yaml
spring:
  datasource:
    url: jdbc:h2:file:./data/az104db
```

Access H2 Console at: http://localhost:8080/h2-console

**JDBC URL**: `jdbc:h2:mem:az104db` (or `file:./data/az104db`)  
**Username**: `sa`  
**Password**: (empty)

See [Database Documentation](./docs/03-database.md) for schema details.

## 📝 Managing Questions

### Import Questions

```bash
# Prepare questions.json file (see docs/06-admin-and-question-bank.md for format)
curl -X POST http://localhost:8080/admin/import \
  -H "Content-Type: multipart/form-data" \
  -F "file=@questions.json"
```

### Export Questions

```bash
curl http://localhost:8080/admin/export -o questions_backup.json
```

See [Admin & Question Bank](./docs/06-admin-and-question-bank.md) for detailed guide.

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests "ExamSessionIntegrationTest"

# With coverage
./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

## 🔧 Technology Stack

| Component | Technology |
|-----------|-----------|
| **Backend** | Spring Boot 3.4.1 |
| **Language** | Java 21 |
| **Build Tool** | Gradle 8.x |
| **Database** | H2 (embedded) |
| **Migrations** | Flyway |
| **Template Engine** | Thymeleaf |
| **Frontend** | Vanilla JavaScript + CSS |
| **Container** | Docker + Docker Compose |

## 🌍 Internationalization

The simulator supports English and Spanish:

- **UI**: `messages_en.properties` / `messages_es.properties`
- **Questions**: Bilingual database columns (`stem_en`, `stem_es`, etc.)
- **Switching**: Language dropdown in header (persisted via cookie)

See [Architecture - Localization](./docs/02-architecture.md#localization-i18n) for details.

## 🔒 Security

**Current Status**: Designed for local/trusted use. No authentication by default.

**Hardening Available**:
- Non-root Docker user
- Read-only filesystem
- Dropped capabilities
- Resource limits
- Security scanning (Trivy, Docker Scout)

See [Security Notes](./docs/08-security-notes.md) for comprehensive security documentation.

## 📊 Project Structure

```
Az104Simulator/
├── docs/                          # Complete documentation
│   ├── 01-overview.md
│   ├── 02-architecture.md
│   ├── 03-database.md
│   ├── 04-flows.md
│   ├── 05-local-dev.md
│   ├── 06-admin-and-question-bank.md
│   ├── 07-troubleshooting.md
│   ├── 08-security-notes.md
│   └── diagrams.md
├── src/
│   ├── main/
│   │   ├── java/co/singularit/az104simulator/
│   │   │   ├── controller/        # Web controllers
│   │   │   ├── service/           # Business logic
│   │   │   ├── repository/        # Data access
│   │   │   ├── domain/            # JPA entities
│   │   │   ├── dto/               # Data transfer objects
│   │   │   └── config/            # Configuration
│   │   └── resources/
│   │       ├── db/migration/      # Flyway migrations
│   │       ├── templates/         # Thymeleaf HTML
│   │       ├── static/            # CSS, JS
│   │       ├── messages_*.properties  # i18n
│   │       └── application.yml    # Config
│   └── test/                      # Tests
├── Dockerfile                     # Production container
├── Dockerfile.distroless          # Alternative (minimal)
├── docker-compose.yml             # Local orchestration
├── Makefile                       # Build commands
├── build.gradle                   # Gradle build
└── README.md                      # This file
```

## 🤝 Contributing

Contributions welcome! Please:

1. Read the [Architecture](./docs/02-architecture.md) to understand the system
2. Check [Troubleshooting](./docs/07-troubleshooting.md) for common issues
3. Follow existing code style
4. Write tests for new features
5. Update documentation

### Adding Questions

See [Admin & Question Bank](./docs/06-admin-and-question-bank.md) for:
- Question JSON format
- Quality guidelines
- Validation rules
- Import/export process

## 📜 License

This project is licensed under the MIT License - see [LICENSE](LICENSE) file for details.

## ⚠️ Disclaimer

This is an **educational tool** for exam preparation. It is:

- **NOT** affiliated with Microsoft
- **NOT** official Microsoft content
- **NOT** a guarantee of passing the certification
- **NOT** real exam dumps

All questions are original educational content created for learning purposes.

## 🙏 Acknowledgments

- Built with [Spring Boot](https://spring.io/projects/spring-boot)
- Containerization best practices from [Docker Security](https://docs.docker.com/develop/security-best-practices/)
- Inspired by the Azure certification community

## 📞 Support

- **Documentation**: See `/docs` folder
- **Issues**: [GitHub Issues](https://github.com/singularit/az104simulator/issues)
- **Security**: See [Security Notes](./docs/08-security-notes.md#security-contact)

---

**Made with ❤️ for the Azure certification community. Good luck with your AZ-104 exam! 🎓**

---

### Quick Commands Reference

```bash
# Development
./gradlew bootRun                 # Run app
./gradlew test                    # Run tests
./gradlew bootJar                 # Build JAR

# Docker
docker build -t az104-simulator . # Build image
docker compose up -d               # Start with Compose
make all                           # Build + run (hardened)

# Database
http://localhost:8080/h2-console   # H2 Console

# Admin
curl http://localhost:8080/admin/export -o questions.json
curl -X POST http://localhost:8080/admin/import -F "file=@questions.json"
```

**Start here**: [Overview](./docs/01-overview.md) → [Local Dev](./docs/05-local-dev.md) → Build something awesome! 🚀

