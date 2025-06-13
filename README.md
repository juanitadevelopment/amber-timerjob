# Juanita Amber TimerJob

[![Maven Central](https://img.shields.io/maven-central/v/net.teppan.amber/amber-timerjob.svg)](https://search.maven.org/artifact/net.teppan.amber/amber-timerjob)
[![Java Version](https://img.shields.io/badge/Java-17%2B-brightgreen.svg)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://github.com/teppan/amber-timerjob/workflows/CI/badge.svg)](https://github.com/teppan/amber-timerjob/actions)

A modern, lightweight scheduler library for Java applications that provides flexible job scheduling with support for interval-based, time-based, date-based, and cron-style scheduling.

## ✨ Features

- 🕒 **Multiple Scheduling Types**: Interval, Time, Date, and Cron scheduling
- 🌍 **TimeZone & Locale Support**: Full internationalization support
- ⚡ **Lightweight**: Minimal dependencies (only SLF4J for logging)
- 🔧 **Lifecycle Management**: Suspend, resume, and cancel jobs dynamically
- 🛡️ **Exception Handling**: Robust error handling with continued execution
- 📋 **Property-based Configuration**: Flexible job configuration
- 🧪 **Well Tested**: High test coverage with comprehensive integration tests

## 🚀 Quick Start

### Maven

```xml
<dependency>
    <groupId>net.teppan.amber</groupId>
    <artifactId>amber-timerjob</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'net.teppan.amber:amber-timerjob:1.0.0'
```

### Basic Usage

```java
import net.teppan.amber.timerjob.*;
import java.util.Timer;
import java.time.ZoneId;
import java.util.Locale;

// 1. Create your job
public class MyJob extends TimerJob {
    @Override
    protected void executeJob() throws Exception {
        getLogger().info("Hello from Juanita Amber TimerJob!");
        // Your business logic here
    }
}

// 2. Set up the job context
JobContext context = new JobContextImpl(); // Your implementation

// 3. Create a schedule directive
ScheduleDirective directive = new ScheduleDirective(
    "CRON 0 9 * * MON-FRI",           // Original directive string
    ScheduleDirective.ScheduleType.CRON,
    Optional.empty(),                   // intervalMillis
    Optional.empty(),                   // hour
    Optional.empty(),                   // minute
    Set.of(),                          // daysOfWeek
    Optional.empty(),                   // dayOfMonth
    Optional.empty(),                   // month
    Optional.of("0 9 * * MON-FRI"),    // cronExpression
    ZoneId.of("Asia/Tokyo"),           // timeZone
    Locale.JAPAN                       // locale
);

// 4. Schedule the job
Timer timer = new Timer("job-scheduler", true);
MyJob job = new MyJob();
job.setJobContext(context);
job.setScheduleDirective(directive);
job.setName("morning-job");
job.schedule(timer);
```

## 📋 Scheduling Formats

### Interval-based Scheduling
```java
"EVERY 15 MIN"  // Every 15 minutes
"EVERY 2 HR"    // Every 2 hours
"EVERY 30 SEC"  // Every 30 seconds
```

### Time-based Scheduling
```java
"AT 09:30"                    // Daily at 9:30 AM
"AT 14:00 EVERY MON|FRI"      // 2:00 PM on Monday and Friday
```

### Date-based Scheduling
```java
"AT 12:00 ON 15"      // 12:00 PM on the 15th of every month
"AT 08:00 ON 12/25"   // 8:00 AM on December 25th
```

### Cron-style Scheduling
```java
"CRON 0 9 * * MON-FRI"     // 9:00 AM weekdays
"CRON 30 14 1 * *"         // 2:30 PM on the 1st of every month
"CRON 0 */2 * * *"         // Every 2 hours
"CRON 15,45 * * * *"       // At 15 and 45 minutes past each hour
```

#### Cron Expression Format
```
┌───────────── minute (0 - 59)
│ ┌─────────── hour (0 - 23)
│ │ ┌───────── day of month (1 - 31)
│ │ │ ┌─────── month (1 - 12 or JAN-DEC)
│ │ │ │ ┌───── day of week (0 - 7 or SUN-SAT, 0 and 7 are Sunday)
│ │ │ │ │
* * * * *
```

**Special Characters:**
- `*` - matches all values
- `,` - separates multiple values
- `-` - defines ranges
- `/` - defines step values

## 🔧 Advanced Usage

### Job Lifecycle Management

```java
// Suspend a job temporarily
job.suspend();

// Resume a suspended job
job.resume();

// Cancel a job permanently
job.cancel();

// Check job status
boolean isSuspended = job.isSuspended();
boolean isCancelled = job.isCancelled();
```

### Custom Job Context

```java
public class MyJobContext implements JobContext {
    private final Logger logger = LoggerFactory.getLogger(MyJobContext.class);
    private final Properties properties = new Properties();
    
    @Override
    public Logger getLogger() {
        return logger;
    }
    
    @Override
    public Optional<String> getJobName() {
        return Optional.of("my-custom-job");
    }
    
    @Override
    public Properties getProperties() {
        return properties;
    }
    
    // Implement other methods...
}
```

### Property-based Configuration

```java
@Override
protected void executeJob() throws Exception {
    String apiUrl = getProperty("api.url", "https://api.example.com");
    int timeout = Integer.parseInt(getProperty("timeout", "30"));
    
    // Use properties in your job logic
}
```

### Initialization and Cleanup Hooks

```java
public class DatabaseJob extends TimerJob {
    private Connection connection;
    
    @Override
    protected void initialize() throws Exception {
        connection = DriverManager.getConnection(
            getProperty("db.url"), 
            getProperty("db.user"), 
            getProperty("db.password")
        );
        getLogger().info("Database connection established");
    }
    
    @Override
    protected void executeJob() throws Exception {
        // Use connection for database operations
    }
    
    @Override
    protected void cleanup() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed");
            } catch (SQLException e) {
                getLogger().warn("Error closing database connection", e);
            }
        }
    }
}
```

## 🌍 TimeZone and Locale Support

```java
ScheduleDirective directive = new ScheduleDirective(
    "CRON 0 9 * * MON-FRI",
    ScheduleDirective.ScheduleType.CRON,
    // ... other parameters
    ZoneId.of("America/New_York"),     // Specific timezone
    Locale.US                          // Specific locale
);
```

## 📊 Examples

See the [examples](examples/) directory for complete working examples:

- [Basic Timer Job](examples/BasicTimerJob.java)
- [Database Cleanup Job](examples/DatabaseCleanupJob.java)
- [Email Notification Job](examples/EmailNotificationJob.java)
- [File Processing Job](examples/FileProcessingJob.java)

## 🛠️ Requirements

- Java 17 or higher
- SLF4J for logging

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by modern scheduling libraries like Quartz and Spring Scheduler
- Built with modern Java features for optimal performance and maintainability

## 🔗 Links

- [API Documentation](https://javadoc.io/doc/net.teppan.amber/amber-timerjob)
- [GitHub Issues](https://github.com/juanitadevelopment/amber-timerjob/issues)
- [Changelog](CHANGELOG.md)
