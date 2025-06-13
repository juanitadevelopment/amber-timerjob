package examples;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.teppan.amber.timerjob.JobContext;
import net.teppan.amber.timerjob.ScheduleDirective;
import net.teppan.amber.timerjob.TimerJob;

/**
 * Basic example demonstrating how to create and schedule a simple TimerJob.
 * 
 * This example shows:
 * - Creating a custom TimerJob
 * - Implementing a basic JobContext
 * - Setting up different schedule types
 * - Running the job
 */
public class BasicTimerJob extends TimerJob {
    
    private int executionCount = 0;
    
    @Override
    protected void executeJob() throws Exception {
        executionCount++;
        getLogger().info("Hello from BasicTimerJob! Execution #{}", executionCount);
        
        // Simulate some work
        Thread.sleep(100);
        
        getLogger().info("BasicTimerJob completed execution #{}", executionCount);
    }
    
    @Override
    protected void initialize() throws Exception {
        getLogger().info("BasicTimerJob initialized");
    }
    
    @Override
    protected void cleanup() {
        getLogger().info("BasicTimerJob cleaned up after {} executions", executionCount);
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Create a basic job context
        JobContext context = new BasicJobContext();
        
        // Create the job
        BasicTimerJob job = new BasicTimerJob();
        job.setName("basic-example-job");
        job.setJobContext(context);
        
        // Example 1: Every 5 seconds
        System.out.println("=== Example 1: Interval-based scheduling (every 5 seconds) ===");
        
        ScheduleDirective intervalDirective = new ScheduleDirective(
            "EVERY 5 SEC",
            ScheduleDirective.ScheduleType.INTERVAL,
            Optional.of(5000L), // 5 seconds
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        job.setScheduleDirective(intervalDirective);
        
        Timer timer = new Timer("example-timer", true);
        job.schedule(timer);
        
        // Let it run for 20 seconds
        Thread.sleep(20000);
        
        // Cancel and show different scheduling
        job.cancel();
        
        // Example 2: Cron-based scheduling
        System.out.println("\n=== Example 2: Cron-based scheduling (every minute) ===");
        
        BasicTimerJob cronJob = new BasicTimerJob();
        cronJob.setName("cron-example-job");
        cronJob.setJobContext(context);
        
        ScheduleDirective cronDirective = new ScheduleDirective(
            "CRON * * * * *",
            ScheduleDirective.ScheduleType.CRON,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("* * * * *"), // Every minute
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        cronJob.setScheduleDirective(cronDirective);
        cronJob.schedule(timer);
        
        // Let it run for 2 minutes
        Thread.sleep(120000);
        
        // Demonstrate lifecycle management
        System.out.println("\n=== Demonstrating lifecycle management ===");
        cronJob.suspend();
        System.out.println("Job suspended");
        Thread.sleep(30000);
        
        cronJob.resume();
        System.out.println("Job resumed");
        Thread.sleep(30000);
        
        cronJob.cancel();
        timer.cancel();
        
        System.out.println("Example completed!");
    }
    
    /**
     * Basic implementation of JobContext for this example.
     */
    static class BasicJobContext implements JobContext {
        private final Logger logger = LoggerFactory.getLogger(BasicTimerJob.class);
        private final Properties properties = new Properties();
        
        public BasicJobContext() {
            // Set some example properties
            properties.setProperty("example.timeout", "30");
            properties.setProperty("example.retries", "3");
        }
        
        @Override
        public Logger getLogger() {
            return logger;
        }
        
        @Override
        public Optional<String> getJobName() {
            return Optional.of("basic-example");
        }
        
        @Override
        public Properties getProperties() {
            return properties;
        }
        
        @Override
        public Optional<String> getProperty(String name) {
            if (name == null) {
                throw new IllegalArgumentException("Property name cannot be null");
            }
            return Optional.ofNullable(properties.getProperty(name));
        }
        
        @Override
        public String getProperty(String name, String defaultValue) {
            if (name == null) {
                throw new IllegalArgumentException("Property name cannot be null");
            }
            return properties.getProperty(name, defaultValue);
        }
        
        @Override
        public void setProperty(String name, String value) {
            if (name == null) {
                throw new IllegalArgumentException("Property name cannot be null");
            }
            if (value == null) {
                properties.remove(name);
            } else {
                properties.setProperty(name, value);
            }
        }
    }
} 