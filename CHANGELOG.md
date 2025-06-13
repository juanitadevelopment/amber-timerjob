# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.0.0] - 2025-06-13

### Added
- Initial release of Amber TimerJob
- Core TimerJob abstract class for creating scheduled jobs
- ScheduleDirective record for flexible schedule configuration
- JobContext interface for job execution environment
- ScheduleExecutor utility for Timer integration
- SchedulingException for error handling

#### Scheduling Types Support
- **Interval-based scheduling**: `EVERY 15 MIN`, `EVERY 2 HR`, `EVERY 30 SEC`
- **Time-based scheduling**: `AT 09:30`, `AT 14:00 EVERY MON|FRI`
- **Date-based scheduling**: `AT 12:00 ON 15`, `AT 08:00 ON 12/25`
- **Cron-style scheduling**: `CRON 0 9 * * MON-FRI`, `CRON 30 14 1 * *`

#### Cron Expression Features
- Standard 5-field cron expressions (minute hour day month day-of-week)
- Support for special characters: `*` (wildcard), `,` (list), `-` (range), `/` (step)
- Month names support: JAN-DEC
- Day names support: SUN-SAT, 0-7 (both 0 and 7 represent Sunday)
- Automatic Sunday normalization (7 → 0)
- Accurate next execution time calculation
- Timezone-aware scheduling

#### Job Lifecycle Management
- Dynamic job suspension and resumption
- Graceful job cancellation
- Initialization and cleanup hooks
- Exception handling with continued execution
- Property-based configuration

#### Internationalization
- Full TimeZone support with `ZoneId`
- Locale support for internationalization
- Timezone-aware cron scheduling

#### Testing & Quality
- Comprehensive test suite with 90%+ code coverage
- Unit tests for all core components
- Integration tests for real-time job execution
- Edge case testing (leap years, timezone changes, etc.)
- JaCoCo code coverage reporting

### Dependencies
- Java 21+ (baseline requirement)
- SLF4J API 2.0.9 (logging)
- JUnit 5.10.0 (testing)
- Mockito 5.5.0 (testing)
- AssertJ 3.24.2 (testing)

### Documentation
- Comprehensive README with examples
- Javadoc API documentation
- Contributing guidelines
- Apache 2.0 license

[Unreleased]: https://github.com/teppan/amber-timerjob/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/teppan/amber-timerjob/releases/tag/v1.0.0 