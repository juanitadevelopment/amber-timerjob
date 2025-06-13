package net.teppan.amber.timerjob;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for TimerJob with Cron scheduling.
 */
@DisplayName("TimerJob Cron Integration Tests")
class TimerJobCronIntegrationTest {
    
    private Timer timer;
    private TestJobContext context;
    
    @BeforeEach
    void setUp() {
        timer = new Timer("test-timer", true);
        context = new TestJobContext();
    }
    
    @AfterEach
    void tearDown() {
        timer.cancel();
    }
    
    @Test
    @DisplayName("Should schedule and execute cron job successfully")
    void shouldScheduleAndExecuteCronJobSuccessfully() throws InterruptedException {
        // Given
        CountDownLatch executionLatch = new CountDownLatch(1);
        TestTimerJob job = new TestTimerJob(executionLatch);
        
        // Use a cron expression that runs every minute for testing
        String cronExpression = "* * * * *";
        ScheduleDirective directive = new ScheduleDirective(
            "CRON " + cronExpression,
            ScheduleDirective.ScheduleType.CRON,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(cronExpression),
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        job.setJobContext(context);
        job.setScheduleDirective(directive);
        job.setName("test-cron-job");
        
        // When
        job.schedule(timer);
        
        // Then
        boolean executed = executionLatch.await(65, TimeUnit.SECONDS); // Wait a bit more than a minute
        assertThat(executed).isTrue();
        assertThat(job.getExecutionCount()).isGreaterThan(0);
    }
    
    @Test
    @DisplayName("Should parse and validate cron expressions correctly")
    void shouldParseAndValidateCronExpressionsCorrectly() {
        // Given
        String[] validCronExpressions = {
            "0 9 * * MON-FRI",    // 9 AM weekdays
            "30 14 1 * *",        // 2:30 PM on 1st of month
            "0 */2 * * *",        // Every 2 hours
            "15,45 * * * *"       // At 15 and 45 minutes past each hour
        };
        
        for (String cronExpression : validCronExpressions) {
            // When & Then
            assertThatCode(() -> {
                TestTimerJob job = new TestTimerJob();
                ScheduleDirective directive = new ScheduleDirective(
                    "CRON " + cronExpression,
                    ScheduleDirective.ScheduleType.CRON,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(cronExpression),
                    ZoneId.systemDefault(),
                    Locale.getDefault()
                );
                
                job.setJobContext(context);
                job.setScheduleDirective(directive);
                job.setName("test-job-" + cronExpression.hashCode());
                
                // This should not throw an exception
                job.schedule(timer);
                job.cancel(); // Clean up immediately
            }).doesNotThrowAnyException();
        }
    }
    
    @Test
    @DisplayName("Should throw exception for invalid cron expressions")
    void shouldThrowExceptionForInvalidCronExpressions() {
        // Given
        String[] invalidCronExpressions = {
            "60 9 * * *",         // Invalid minute (60)
            "0 25 * * *",         // Invalid hour (25)
            "0 9 32 * *",         // Invalid day (32)
            "0 9 * 13 *",         // Invalid month (13)
            "0 9 * * 8",          // Invalid day of week (8)
            "0 9 * *",            // Too few fields
            "0 9 * * * *"         // Too many fields
        };
        
        for (String cronExpression : invalidCronExpressions) {
            // When & Then
            assertThatThrownBy(() -> {
                TestTimerJob job = new TestTimerJob();
                ScheduleDirective directive = new ScheduleDirective(
                    "CRON " + cronExpression,
                    ScheduleDirective.ScheduleType.CRON,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(cronExpression),
                    ZoneId.systemDefault(),
                    Locale.getDefault()
                );
                
                job.setJobContext(context);
                job.setScheduleDirective(directive);
                job.setName("test-job-invalid");
                
                job.schedule(timer);
            }).isInstanceOf(SchedulingException.class);
        }
    }
    
    @Test
    @DisplayName("Should handle job suspension and resumption with cron scheduling")
    void shouldHandleJobSuspensionAndResumptionWithCronScheduling() throws InterruptedException {
        // Given
        CountDownLatch initialExecutionLatch = new CountDownLatch(1);
        CountDownLatch resumeExecutionLatch = new CountDownLatch(2); // Wait for 2 executions after resume
        TestTimerJob job = new TestTimerJob(initialExecutionLatch, resumeExecutionLatch);
        
        String cronExpression = "* * * * *"; // Every minute
        ScheduleDirective directive = new ScheduleDirective(
            "CRON " + cronExpression,
            ScheduleDirective.ScheduleType.CRON,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.of(cronExpression),
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        job.setJobContext(context);
        job.setScheduleDirective(directive);
        job.setName("test-suspend-resume-job");
        
        // When
        job.schedule(timer);
        
        // Wait for initial execution
        boolean initialExecuted = initialExecutionLatch.await(65, TimeUnit.SECONDS);
        assertThat(initialExecuted).isTrue();
        
        // Suspend the job
        job.suspend();
        int executionCountAfterSuspend = job.getExecutionCount();
        
        // Wait a bit to ensure no more executions happen
        Thread.sleep(2000);
        assertThat(job.getExecutionCount()).isEqualTo(executionCountAfterSuspend);
        
        // Resume the job
        job.resume();
        
        // Then
        boolean resumeExecuted = resumeExecutionLatch.await(125, TimeUnit.SECONDS); // Wait for more executions
        assertThat(resumeExecuted).isTrue();
        assertThat(job.getExecutionCount()).isGreaterThan(executionCountAfterSuspend);
        
        job.cancel();
    }
    
    /**
     * Test implementation of TimerJob for testing purposes.
     */
    private static class TestTimerJob extends TimerJob {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final CountDownLatch initialLatch;
        private final CountDownLatch resumeLatch;
        
        public TestTimerJob() {
            this(null, null);
        }
        
        public TestTimerJob(CountDownLatch initialLatch) {
            this(initialLatch, null);
        }
        
        public TestTimerJob(CountDownLatch initialLatch, CountDownLatch resumeLatch) {
            this.initialLatch = initialLatch;
            this.resumeLatch = resumeLatch;
        }
        
        @Override
        protected void executeJob() throws Exception {
            int count = executionCount.incrementAndGet();
            getLogger().info("Test job executed {} times", count);
            
            if (initialLatch != null && count == 1) {
                initialLatch.countDown();
            }
            
            if (resumeLatch != null && count > 1) {
                resumeLatch.countDown();
            }
        }
        
        public int getExecutionCount() {
            return executionCount.get();
        }
    }
    
    /**
     * Test implementation of JobContext for testing purposes.
     */
    private static class TestJobContext implements JobContext {
        private final Logger logger = LoggerFactory.getLogger(TestJobContext.class);
        private final Properties properties = new Properties();
        
        @Override
        public Logger getLogger() {
            return logger;
        }
        
        @Override
        public Optional<String> getJobName() {
            return Optional.of("test-job");
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