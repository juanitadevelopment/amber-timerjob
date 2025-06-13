package examples;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
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
 * Example demonstrating a database cleanup job using TimerJob.
 * 
 * This example shows:
 * - Database connection management with initialization/cleanup hooks
 * - Property-based configuration
 * - Exception handling in job execution
 * - Real-world job scheduling scenarios
 * 
 * Note: This example uses an in-memory H2 database for demonstration.
 * In a real application, you would configure your actual database.
 */
public class DatabaseCleanupJob extends TimerJob {
    
    private Connection connection;
    private int recordsDeleted = 0;
    
    @Override
    protected void initialize() throws Exception {
        // Get database configuration from properties
        String dbUrl = getProperty("db.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
        String dbUser = getProperty("db.user", "sa");
        String dbPassword = getProperty("db.password", "");
        
        getLogger().info("Connecting to database: {}", dbUrl);
        
        // Establish database connection
        connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        
        // Initialize test data (in a real scenario, this would be your actual tables)
        initializeTestData();
        
        getLogger().info("Database connection established and test data initialized");
    }
    
    @Override
    protected void executeJob() throws Exception {
        if (connection == null || connection.isClosed()) {
            throw new IllegalStateException("Database connection is not available");
        }
        
        // Get cleanup configuration from properties
        int retentionDays = Integer.parseInt(getProperty("cleanup.retention.days", "30"));
        int batchSize = Integer.parseInt(getProperty("cleanup.batch.size", "1000"));
        
        getLogger().info("Starting database cleanup - retention: {} days, batch size: {}", 
                        retentionDays, batchSize);
        
        // Cleanup old log entries
        int deletedLogs = cleanupOldLogEntries(retentionDays, batchSize);
        
        // Cleanup old temporary files
        int deletedFiles = cleanupOldTempFiles(retentionDays, batchSize);
        
        // Cleanup expired sessions
        int deletedSessions = cleanupExpiredSessions(batchSize);
        
        recordsDeleted += deletedLogs + deletedFiles + deletedSessions;
        
        getLogger().info("Database cleanup completed - deleted {} log entries, {} temp files, {} sessions", 
                        deletedLogs, deletedFiles, deletedSessions);
        
        // Vacuum/optimize database if configured
        if (Boolean.parseBoolean(getProperty("cleanup.vacuum.enabled", "false"))) {
            vacuumDatabase();
        }
    }
    
    @Override
    protected void cleanup() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed. Total records deleted: {}", recordsDeleted);
            } catch (SQLException e) {
                getLogger().warn("Error closing database connection", e);
            }
        }
    }
    
    private void initializeTestData() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            // Create test tables
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS log_entries (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    message VARCHAR(1000),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS temp_files (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    filename VARCHAR(255),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_sessions (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    session_id VARCHAR(255),
                    expires_at TIMESTAMP,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);
            
            // Insert some test data
            stmt.execute("""
                INSERT INTO log_entries (message, created_at) VALUES 
                ('Old log entry 1', DATEADD('DAY', -35, CURRENT_TIMESTAMP)),
                ('Old log entry 2', DATEADD('DAY', -40, CURRENT_TIMESTAMP)),
                ('Recent log entry', DATEADD('DAY', -5, CURRENT_TIMESTAMP))
            """);
            
            stmt.execute("""
                INSERT INTO temp_files (filename, created_at) VALUES 
                ('old_temp1.tmp', DATEADD('DAY', -45, CURRENT_TIMESTAMP)),
                ('old_temp2.tmp', DATEADD('DAY', -50, CURRENT_TIMESTAMP)),
                ('recent_temp.tmp', DATEADD('DAY', -1, CURRENT_TIMESTAMP))
            """);
            
            stmt.execute("""
                INSERT INTO user_sessions (session_id, expires_at) VALUES 
                ('expired_session_1', DATEADD('HOUR', -24, CURRENT_TIMESTAMP)),
                ('expired_session_2', DATEADD('HOUR', -48, CURRENT_TIMESTAMP)),
                ('active_session', DATEADD('HOUR', 2, CURRENT_TIMESTAMP))
            """);
        }
    }
    
    private int cleanupOldLogEntries(int retentionDays, int batchSize) throws SQLException {
        String sql = """
            DELETE FROM log_entries 
            WHERE created_at < DATEADD('DAY', ?, CURRENT_TIMESTAMP) 
            LIMIT ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, -retentionDays);
            stmt.setInt(2, batchSize);
            return stmt.executeUpdate();
        }
    }
    
    private int cleanupOldTempFiles(int retentionDays, int batchSize) throws SQLException {
        String sql = """
            DELETE FROM temp_files 
            WHERE created_at < DATEADD('DAY', ?, CURRENT_TIMESTAMP) 
            LIMIT ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, -retentionDays);
            stmt.setInt(2, batchSize);
            return stmt.executeUpdate();
        }
    }
    
    private int cleanupExpiredSessions(int batchSize) throws SQLException {
        String sql = """
            DELETE FROM user_sessions 
            WHERE expires_at < CURRENT_TIMESTAMP 
            LIMIT ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, batchSize);
            return stmt.executeUpdate();
        }
    }
    
    private void vacuumDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("ANALYZE");
            getLogger().info("Database vacuum/analyze completed");
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        // Create job context with database configuration
        JobContext context = new DatabaseJobContext();
        
        // Create the cleanup job
        DatabaseCleanupJob cleanupJob = new DatabaseCleanupJob();
        cleanupJob.setName("database-cleanup-job");
        cleanupJob.setJobContext(context);
        
        // Schedule to run every day at 2:00 AM
        ScheduleDirective dailyCleanup = new ScheduleDirective(
            "CRON 0 2 * * *",
            ScheduleDirective.ScheduleType.CRON,
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.of("0 2 * * *"), // 2:00 AM daily
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        cleanupJob.setScheduleDirective(dailyCleanup);
        
        Timer timer = new Timer("database-cleanup-timer", true);
        cleanupJob.schedule(timer);
        
        System.out.println("Database cleanup job scheduled for 2:00 AM daily");
        System.out.println("For demonstration, let's run it immediately every 10 seconds...");
        
        // For demonstration, also create a frequent cleanup job
        DatabaseCleanupJob demoJob = new DatabaseCleanupJob();
        demoJob.setName("demo-cleanup-job");
        demoJob.setJobContext(context);
        
        ScheduleDirective demoDirective = new ScheduleDirective(
            "EVERY 10 SEC",
            ScheduleDirective.ScheduleType.INTERVAL,
            Optional.of(10000L), // 10 seconds
            Optional.empty(),
            Optional.empty(),
            Set.of(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            ZoneId.systemDefault(),
            Locale.getDefault()
        );
        
        demoJob.setScheduleDirective(demoDirective);
        demoJob.schedule(timer);
        
        // Let it run for 1 minute
        Thread.sleep(60000);
        
        // Clean up
        cleanupJob.cancel();
        demoJob.cancel();
        timer.cancel();
        
        System.out.println("Database cleanup example completed!");
    }
    
    /**
     * JobContext implementation with database-specific properties.
     */
    static class DatabaseJobContext implements JobContext {
        private final Logger logger = LoggerFactory.getLogger(DatabaseCleanupJob.class);
        private final Properties properties = new Properties();
        
        public DatabaseJobContext() {
            // Database configuration
            properties.setProperty("db.url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1");
            properties.setProperty("db.user", "sa");
            properties.setProperty("db.password", "");
            
            // Cleanup configuration
            properties.setProperty("cleanup.retention.days", "30");
            properties.setProperty("cleanup.batch.size", "1000");
            properties.setProperty("cleanup.vacuum.enabled", "true");
        }
        
        @Override
        public Logger getLogger() {
            return logger;
        }
        
        @Override
        public Optional<String> getJobName() {
            return Optional.of("database-cleanup");
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