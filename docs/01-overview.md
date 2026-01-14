# Overview

Welcome to the **AZ-104 Exam Simulator**, your practice companion for the Microsoft Azure Administrator certification exam. This is a Spring Boot web application designed to help you learn through realistic practice sessions and timed exam simulations.

## What Does This Simulator Do?

This application lets you:

- **Practice at Your Own Pace**: Study questions with immediate feedback and explanations
- **Simulate Real Exams**: Take timed exams that mimic the actual certification experience
- **Review Your Performance**: Analyze your attempts question-by-question with detailed explanations
- **Track Your Progress**: Access a complete history of all your practice sessions and exams
- **Learn in Your Language**: Switch between English and Spanish for both UI and question content

## Key Features

- ✅ **Two Learning Modes**: Practice (untimed, instant feedback) and Exam (timed, no hints)
- ✅ **Bilingual Support**: Full i18n for English and Spanish
- ✅ **No Question Duplication**: Each exam session guarantees unique questions through database constraints
- ✅ **Persistent Sessions**: Resume your attempts or review past exams anytime
- ✅ **Detailed Explanations**: Every question includes comprehensive explanations in both languages
- ✅ **Domain-Based Practice**: Filter questions by Azure domain (Compute, Networking, Storage, etc.)
- ✅ **Realistic Question Types**: Single choice, multiple choice, and Yes/No scenarios
- ✅ **Mark for Review**: Flag questions during exam mode for later review
- ✅ **Score Analytics**: Breakdown by domain, difficulty, and overall performance

## What This Is NOT

Let's be clear about what we intentionally don't do:

- **Not Real Exam Dumps**: All questions are original educational content, not leaked exam material
- **Not Certification Guarantee**: This is a practice tool, not a shortcut to passing
- **Not a Commercial Product**: Built for learning, not profit
- **Not Microsoft Official**: This is an independent educational project

## Typical User Journey

Here's how most people use the simulator:

1. **Land on Home Page**: You see total questions available and breakdowns by domain
2. **Choose Your Mode**: Click "Start Practice" for learning or "Start Exam" for testing
3. **Configure Your Session**: Select number of questions, domains, time limits, etc.
4. **Answer Questions**: Navigate through questions, mark tricky ones for review
5. **Review Your Answers** (Practice mode): Get instant feedback and explanations after each question
6. **Submit and Score** (Exam mode): Complete the exam and see your results
7. **Analyze Performance**: Review question-by-question breakdown with correct answers and explanations
8. **Check History**: Access all past attempts to track improvement over time

## Quick Stats

- **Database**: H2 (in-memory or file-based, configurable)
- **Frontend**: Server-side Thymeleaf templates with vanilla JavaScript
- **Backend**: Spring Boot 3.4.1 with Java 21
- **Build Tool**: Gradle
- **Migrations**: Flyway for versioned schema changes
- **Localization**: Spring MessageSource with properties files

## Who Should Use This?

- Azure certification candidates preparing for AZ-104
- DevOps engineers learning Azure administration
- IT professionals transitioning to cloud platforms
- Students practicing exam-taking strategies
- Anyone wanting to self-assess Azure knowledge

## Next Steps

- Read [Architecture](./02-architecture.md) to understand how the system works
- Check [Database](./03-database.md) to see the data model
- Review [Flows](./04-flows.md) for detailed user workflows
- Jump to [Local Development](./05-local-dev.md) to run it yourself

---

**Pro Tip**: Start with Practice mode to build confidence, then move to Exam mode when you're consistently scoring above 70%. The real AZ-104 exam requires ~700/1000 points to pass (roughly 70%).

