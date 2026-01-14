# AZ-104 Simulator Documentation

Welcome to the complete documentation for the AZ-104 Exam Simulator. This guide covers everything from high-level architecture to troubleshooting specific issues.

## 📖 Documentation Index

### Getting Started

1. **[Overview](./01-overview.md)** - Start here!
   - What the simulator does
   - Key features
   - What it's NOT
   - Typical user journey
   - Quick stats

### Understanding the System

2. **[Architecture](./02-architecture.md)** - How it all works
   - High-level components
   - Controllers, services, repositories
   - Localization (i18n) mechanism
   - Question bank management
   - Technology stack
   - Design decisions

3. **[Database](./03-database.md)** - Data model deep dive
   - Complete schema documentation
   - All tables explained
   - Relationships and constraints
   - How no-duplication works
   - Migration history
   - ER diagram

4. **[User Flows](./04-flows.md)** - Step-by-step workflows
   - Practice mode flow
   - Exam mode flow
   - Attempt history & review
   - Language switching
   - State machine diagrams
   - Edge cases handled

### Practical Guides

5. **[Local Development](./05-local-dev.md)** - Run it yourself
   - Prerequisites
   - Quick start
   - Configuration options
   - H2 Console access
   - Running tests
   - Building JARs
   - Docker workflow
   - Gradle tasks
   - IDE setup

6. **[Admin & Question Bank](./06-admin-and-question-bank.md)** - Content management
   - Question structure
   - Question types (SINGLE, MULTI, YESNO)
   - JSON format examples
   - Import/export process
   - Quality guidelines
   - Best practices
   - Bulk operations

### Problem Solving

7. **[Troubleshooting](./07-troubleshooting.md)** - Fix common issues
   - Encoding problems (�)
   - Question duplication
   - Order/navigation bugs
   - Database issues
   - Performance problems
   - Session/cookie issues
   - Build failures

8. **[Security Notes](./08-security-notes.md)** - Secure deployment
   - Security model overview
   - Authentication status
   - Input validation
   - XSS protection
   - Docker hardening
   - Data privacy
   - Threat model
   - Security checklist

### Visual Reference

9. **[Diagrams](./diagrams.md)** - Visual system documentation
   - Component diagram
   - ER diagram (database schema)
   - Sequence diagrams (all flows)
   - State machine (attempt lifecycle)

---

## 🚀 Quick Navigation by Role

### I'm a Developer

**First time?**
1. [Overview](./01-overview.md) - Understand what you're building
2. [Architecture](./02-architecture.md) - Learn the structure
3. [Local Dev](./05-local-dev.md) - Get it running
4. [Database](./03-database.md) - Understand the data model

**Working on features?**
- [Flows](./04-flows.md) - See how users interact
- [Diagrams](./diagrams.md) - Visual sequence flows
- [Troubleshooting](./07-troubleshooting.md) - Debug issues

### I'm a DevOps Engineer

**Deploying?**
1. [Security Notes](./08-security-notes.md) - Harden before deployment
2. [Local Dev](./05-local-dev.md#docker-workflow) - Docker commands
3. [Troubleshooting](./07-troubleshooting.md#production-issues) - Production issues

**See also**: `DOCKER.md` in root for container-specific docs.

### I'm a Content Manager

**Managing questions?**
1. [Admin & Question Bank](./06-admin-and-question-bank.md) - Everything about questions
2. [Troubleshooting](./07-troubleshooting.md#encoding--localization-issues) - Fix encoding issues

### I'm a Student/User

**Using the simulator?**
- [Overview](./01-overview.md#typical-user-journey) - How to use it
- [Flows](./04-flows.md) - Understand the modes

---

## 📝 Documentation Standards

This documentation follows these principles:

- **Human-First**: Written for developers, not robots
- **No Placeholders**: Every section is complete (no TODOs)
- **Code-Aligned**: Reflects actual codebase, not aspirations
- **Mermaid Diagrams**: Visual learners welcome
- **Practical Examples**: Real commands you can copy-paste
- **UTF-8**: Because accents matter (é, ñ, ü)

---

## 🔗 External Links

- **Main README**: [../README.md](../README.md)
- **Docker Docs**: [../DOCKER.md](../DOCKER.md)
- **Build Config**: [../build.gradle](../build.gradle)
- **Application Config**: [../src/main/resources/application.yml](../src/main/resources/application.yml)

---

## 🤔 Still Have Questions?

1. **Search this documentation** - Use Ctrl+F
2. **Check diagrams** - [diagrams.md](./diagrams.md)
3. **Review troubleshooting** - [07-troubleshooting.md](./07-troubleshooting.md)
4. **Read the code** - Comments are your friend
5. **Ask the team** - Open an issue on GitHub

---

## 📚 Recommended Reading Order

**For Onboarding (1-2 hours)**:
1. Overview (15 min)
2. Architecture (20 min)
3. Local Dev - Quick Start section (10 min)
4. Skim Diagrams (10 min)
5. Try running locally (30 min)

**For Deep Understanding (4-6 hours)**:
1. All of the above
2. Database (30 min)
3. Flows (45 min)
4. Admin & Question Bank (30 min)
5. Security Notes (30 min)
6. Full Diagrams study (45 min)
7. Read key source files (60-120 min)

**For Troubleshooting (as needed)**:
- Jump directly to [Troubleshooting](./07-troubleshooting.md)
- Search for your specific error
- Check relevant diagram for context

---

## 🎯 Learning Paths

### Path 1: Frontend Developer
→ Overview → Flows → Templates (in code) → Troubleshooting

### Path 2: Backend Developer
→ Overview → Architecture → Database → Services (in code) → Security

### Path 3: Full-Stack Developer
→ Overview → Architecture → Database → Flows → Local Dev → Everything else

### Path 4: DevOps/SRE
→ Security → DOCKER.md → Local Dev (Docker) → Troubleshooting (Production)

### Path 5: Content Creator
→ Overview → Admin & Question Bank → Troubleshooting (Encoding)

---

**Remember**: Good documentation is like good code - it evolves. If you find something unclear, improve it and commit the change. Future you (and your teammates) will thank you. 🙏

Happy learning! 🎓

