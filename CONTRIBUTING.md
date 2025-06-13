# Contributing to Juanita Amber TimerJob

Thank you for considering contributing to Juanita Amber TimerJob! We welcome contributions from everyone.

## 🤝 How to Contribute

### Reporting Issues

Before creating an issue, please:

1. **Search existing issues** to avoid duplicates
2. **Use the issue template** if available
3. **Provide clear reproduction steps** for bugs
4. **Include system information** (Java version, OS, etc.)

### Suggesting Enhancements

We welcome enhancement suggestions! Please:

1. **Check if the feature already exists** or is planned
2. **Describe the use case** clearly
3. **Provide examples** of how the feature would be used
4. **Consider backward compatibility**

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch** from `main`
3. **Make your changes**
4. **Add tests** for new functionality
5. **Update documentation** if needed
6. **Ensure tests pass**
7. **Submit a pull request**

## 🛠️ Development Setup

### Prerequisites

- Java 21 or higher
- Maven 3.6+ or Gradle 7+
- Git

### Setting up the development environment

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/amber-timerjob.git
cd amber-timerjob

# Build the project
mvn clean compile

# Run tests
mvn test

# Generate test coverage report
mvn jacoco:report
```

### Project Structure

```
amber-timerjob/
├── src/
│   ├── main/java/net/teppan/amber/timerjob/
│   │   ├── TimerJob.java              # Abstract base class
│   │   ├── ScheduleDirective.java     # Schedule configuration
│   │   ├── JobContext.java            # Job execution context
│   │   ├── ScheduleExecutor.java      # Scheduling logic
│   │   ├── CronExpression.java        # Cron parsing and execution
│   │   └── SchedulingException.java   # Exception handling
│   └── test/java/net/teppan/amber/timerjob/
│       ├── CronExpressionTest.java
│       ├── ScheduleDirectiveTest.java
│       └── TimerJobCronIntegrationTest.java
├── examples/                          # Usage examples
├── docs/                              # Additional documentation
└── pom.xml                           # Maven configuration
```

## 🧪 Testing Guidelines

### Writing Tests

- **Unit tests** for individual components
- **Integration tests** for end-to-end scenarios
- **Test edge cases** (timezone changes, leap years, etc.)
- **Maintain high code coverage** (aim for 90%+)

### Test Categories

1. **Unit Tests**: Test individual methods and classes
2. **Integration Tests**: Test component interactions
3. **Performance Tests**: Ensure scheduling accuracy
4. **Edge Case Tests**: Handle boundary conditions

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=CronExpressionTest

# Run tests with coverage
mvn clean test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## 📝 Code Style

### Java Conventions

- **Use Java 21 features** where appropriate
- **Follow Oracle's Java conventions**
- **Use meaningful variable names**
- **Add Javadoc** for public APIs
- **Keep methods focused** and small

### Code Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line length**: 120 characters maximum
- **Braces**: K&R style
- **Imports**: Organize and remove unused

### Example

```java
/**
 * Calculates the next execution time for a cron expression.
 * 
 * @param cronExpression the cron expression to evaluate
 * @param timeZone the timezone for calculation
 * @return the next execution time
 * @throws SchedulingException if the expression is invalid
 */
public ZonedDateTime getNextExecutionTime(CronExpression cronExpression, ZoneId timeZone) {
    // Implementation here
}
```

## 📋 Commit Guidelines

### Commit Message Format

```
type(scope): description

[optional body]

[optional footer]
```

### Types

- **feat**: New feature
- **fix**: Bug fix
- **docs**: Documentation changes
- **style**: Code formatting (no logic changes)
- **refactor**: Code refactoring
- **test**: Adding or updating tests
- **chore**: Build process or auxiliary tool changes

### Examples

```
feat(cron): add support for year field in cron expressions
fix(schedule): handle timezone changes correctly
docs(readme): update installation instructions
test(cron): add edge case tests for leap years
```

## 🏷️ Versioning

We follow [Semantic Versioning](https://semver.org/):

- **MAJOR** version for incompatible API changes
- **MINOR** version for backward-compatible functionality
- **PATCH** version for backward-compatible bug fixes

## 📄 License

By contributing to Juanita Amber TimerJob, you agree that your contributions will be licensed under the Apache License 2.0.

## 🙋‍♀️ Getting Help

- **GitHub Issues**: For bugs and feature requests
- **GitHub Discussions**: For questions and general discussion
- **Documentation**: Check the [wiki](https://github.com/teppan/amber-timerjob/wiki)

## 🎯 Good First Issues

Look for issues labeled `good-first-issue` for beginner-friendly contributions:

- Documentation improvements
- Test coverage increases
- Example code additions
- Bug fixes with clear reproduction steps

## 📈 Project Roadmap

Check our [project roadmap](https://github.com/teppan/amber-timerjob/projects) to see what we're working on next.

---

Thank you for contributing to Juanita Amber TimerJob! 🚀 