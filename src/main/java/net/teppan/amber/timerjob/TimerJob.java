package net.teppan.amber.timerjob;

import org.slf4j.Logger;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstract base class for all scheduled timer jobs in the Amber TimerJob framework.
 * 
 * <p>TimerJob provides a modern, flexible scheduling system that supports various
 * schedule formats including interval-based, time-based, date-based, and cron-style
 * scheduling. Jobs can be suspended, resumed, or cancelled dynamically.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Multiple scheduling formats (EVERY, AT, CRON)</li>
 *   <li>TimeZone and Locale support</li>
 *   <li>Lifecycle management (suspend/resume/cancel)</li>
 *   <li>Exception handling and logging</li>
 *   <li>Property-based configuration</li>
 * </ul>
 * 
 * <h3>Scheduling Examples:</h3>
 * <pre>{@code
 * // Interval-based
 * "EVERY 15 MIN"
 * "EVERY 2 HR"
 * "EVERY 30 SEC"
 * 
 * // Time-based
 * "AT 09:30"
 * "AT 14:00 EVERY MON|FRI"
 * 
 * // Date-based
 * "AT 12:00 ON 15"
 * "AT 08:00 ON 12/25"
 * 
 * // Cron-style
 * "CRON 0 9 * * MON-FRI"
 * }</pre>
 * 
 * <h3>Usage:</h3>
 * <pre>{@code
 * public class MyJob extends TimerJob {
 *     @Override
 *     protected void executeJob() {
 *         getLogger().info("Executing my job");
 *         // Job logic here
 *     }
 * }
 * 
 * // Schedule and start
 * MyJob job = new MyJob();
 * job.setJobContext(context);
 * job.setScheduleDirective(directive);
 * job.schedule(timer);
 * }</pre>
 * 
 * @since 1.0
 * @author Amber TimerJob
 */
public abstract class TimerJob extends TimerTask {
    
    // Job state management
    private final AtomicBoolean suspended = new AtomicBoolean(false);
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<JobContext> context = new AtomicReference<>();
    private final AtomicReference<ScheduleDirective> directive = new AtomicReference<>();
    
    // Configuration
    private volatile boolean autoStart = true;
    private volatile String name;
    
    /**
     * Default constructor.
     */
    protected TimerJob() {
        // Default configuration
    }
    
    /**
     * Constructor with job name.
     * 
     * @param name the job name, may be null
     */
    protected TimerJob(String name) {
        this.name = name;
    }
    
    /**
     * Sets the job context that provides access to logging, properties, and other resources.
     * 
     * @param context the job context, must not be null
     * @throws IllegalArgumentException if context is null
     */
    public final void setJobContext(JobContext context) {
        if (context == null) {
            throw new IllegalArgumentException("JobContext cannot be null");
        }
        this.context.set(context);
    }
    
    /**
     * Returns the current job context.
     * 
     * @return the job context or empty if not set
     */
    public final Optional<JobContext> getJobContext() {
        return Optional.ofNullable(context.get());
    }
    
    /**
     * Returns the logger from the current context.
     * 
     * @return the logger instance
     * @throws IllegalStateException if no context is set
     */
    protected final Logger getLogger() {
        return getJobContext()
            .orElseThrow(() -> new IllegalStateException("No JobContext available"))
            .getLogger();
    }
    
    /**
     * Sets the schedule directive for this job.
     * 
     * @param directive the schedule directive, must not be null
     * @throws IllegalArgumentException if directive is null
     */
    public final void setScheduleDirective(ScheduleDirective directive) {
        if (directive == null) {
            throw new IllegalArgumentException("ScheduleDirective cannot be null");
        }
        this.directive.set(directive);
    }
    
    /**
     * Returns the current schedule directive.
     * 
     * @return the schedule directive or empty if not set
     */
    public final Optional<ScheduleDirective> getScheduleDirective() {
        return Optional.ofNullable(directive.get());
    }
    
    /**
     * Sets the job name.
     * 
     * @param name the job name, may be null
     */
    public final void setName(String name) {
        this.name = name;
    }
    
    /**
     * Returns the job name.
     * 
     * @return the job name or null if not set
     */
    public final String getName() {
        return name;
    }
    
    /**
     * Sets whether this job should start automatically when scheduled.
     * 
     * @param autoStart true to start automatically, false otherwise
     */
    public final void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }
    
    /**
     * Returns whether this job is configured for auto-start.
     * 
     * @return true if auto-start is enabled
     */
    public final boolean isAutoStart() {
        return autoStart;
    }
    
    /**
     * Returns the time zone for this job.
     * 
     * @return the time zone or system default if no directive is set
     */
    public final ZoneId getTimeZone() {
        return getScheduleDirective()
            .map(ScheduleDirective::timeZone)
            .orElse(ZoneId.systemDefault());
    }
    
    /**
     * Returns the locale for this job.
     * 
     * @return the locale or system default if no directive is set
     */
    public final Locale getLocale() {
        return getScheduleDirective()
            .map(ScheduleDirective::locale)
            .orElse(Locale.getDefault());
    }
    
    /**
     * Returns a property value from the job context.
     * 
     * @param name the property name, must not be null
     * @return the property value or empty if not found
     * @throws IllegalArgumentException if name is null
     */
    protected final Optional<String> getProperty(String name) {
        return getJobContext()
            .flatMap(ctx -> ctx.getProperty(name));
    }
    
    /**
     * Returns a property value with a default.
     * 
     * @param name the property name, must not be null
     * @param defaultValue the default value
     * @return the property value or default if not found
     * @throws IllegalArgumentException if name is null
     */
    protected final String getProperty(String name, String defaultValue) {
        return getJobContext()
            .map(ctx -> ctx.getProperty(name, defaultValue))
            .orElse(defaultValue);
    }
    
    /**
     * Initialization hook called before the job is scheduled.
     * 
     * <p>Subclasses can override this method to perform initialization tasks.
     * The job context and schedule directive are guaranteed to be available
     * when this method is called.</p>
     * 
     * <p>The default implementation does nothing.</p>
     * 
     * @throws Exception if initialization fails
     */
    protected void initialize() throws Exception {
        // Default: no initialization
    }
    
    /**
     * Cleanup hook called when the job is cancelled.
     * 
     * <p>Subclasses can override this method to perform cleanup tasks.
     * The default implementation does nothing.</p>
     */
    protected void cleanup() {
        // Default: no cleanup
    }
    
    /**
     * Schedules this job using the provided Timer and the current schedule directive.
     * 
     * @param timer the Timer instance to use for scheduling, must not be null
     * @throws IllegalArgumentException if timer is null
     * @throws IllegalStateException if no schedule directive is set
     * @throws SchedulingException if scheduling fails
     */
    public final void schedule(Timer timer) {
        if (timer == null) {
            throw new IllegalArgumentException("Timer cannot be null");
        }
        
        ScheduleDirective sched = getScheduleDirective()
            .orElseThrow(() -> new IllegalStateException("No schedule directive set"));
        
        if (!sched.isActive()) {
            getLogger().info("Job '{}' is disabled by schedule directive", name);
            return;
        }
        
        try {
            // Call initialization hook
            initialize();
            
            // Schedule based on directive type
            ScheduleExecutor.schedule(timer, this, sched);
            
            cancelled.set(false);
            getLogger().info("Job '{}' scheduled: {}", name, sched.getDescription());
            
        } catch (Exception e) {
            cancelled.set(true);
            String msg = String.format("Failed to schedule job '%s': %s", name, e.getMessage());
            getLogger().error(msg, e);
            throw new SchedulingException(msg, e);
        }
    }
    
    /**
     * Suspends the job temporarily.
     * 
     * <p>The Timer will continue to call this job, but {@link #executeJob()}
     * will be skipped until {@link #resume()} is called.</p>
     */
    public final void suspend() {
        suspended.set(true);
        getLogger().info("Job '{}' suspended", name);
    }
    
    /**
     * Resumes a suspended job.
     */
    public final void resume() {
        suspended.set(false);
        getLogger().info("Job '{}' resumed", name);
    }
    
    /**
     * Returns whether this job is currently suspended.
     * 
     * @return true if suspended
     */
    public final boolean isSuspended() {
        return suspended.get();
    }
    
    /**
     * Cancels this job and marks it as cancelled.
     * 
     * @return the result of {@link TimerTask#cancel()}
     */
    @Override
    public final boolean cancel() {
        boolean result = super.cancel();
        cancelled.set(true);
        
        try {
            cleanup();
        } catch (Exception e) {
            getLogger().warn("Exception during job cleanup for '{}'", name, e);
        }
        
        getLogger().info("Job '{}' cancelled", name);
        return result;
    }
    
    /**
     * Returns whether this job has been cancelled.
     * 
     * @return true if cancelled
     */
    public final boolean isCancelled() {
        return cancelled.get();
    }
    
    /**
     * Internal TimerTask run method.
     * 
     * <p>This method handles the execution flow, exception catching, and
     * calls the abstract {@link #executeJob()} method if the job is not suspended.</p>
     */
    @Override
    public final void run() {
        try {
            if (canExecute()) {
                long startTime = System.currentTimeMillis();
                executeJob();
                long duration = System.currentTimeMillis() - startTime;
                getLogger().debug("Job '{}' completed in {}ms", name, duration);
            }
        } catch (Exception e) {
            getLogger().error("Exception in job '{}'", name, e);
            // Continue running despite exception
        }
    }
    
    /**
     * Determines whether the job can execute at this time.
     * 
     * @return true if the job should execute
     */
    private boolean canExecute() {
        if (suspended.get()) {
            getLogger().debug("Job '{}' skipped - suspended", name);
            return false;
        }
        
        if (cancelled.get()) {
            getLogger().debug("Job '{}' skipped - cancelled", name);
            return false;
        }
        
        ScheduleDirective sched = directive.get();
        if (sched == null || !sched.isActive()) {
            getLogger().debug("Job '{}' skipped - no active schedule", name);
            return false;
        }
        
        // Additional schedule-specific checks could be added here
        // (e.g., day-of-week, date matching for time-based schedules)
        
        return true;
    }
    
    /**
     * Abstract method that must be implemented by subclasses to define the job's work.
     * 
     * <p>This method is called by the framework when the job should execute.
     * Any exceptions thrown from this method will be caught and logged, but
     * will not stop the job from continuing to run on schedule.</p>
     * 
     * @throws Exception if the job execution encounters an error
     */
    protected abstract void executeJob() throws Exception;
    
    /**
     * Returns a string representation of this job.
     * 
     * @return string representation
     */
    @Override
    public String toString() {
        return String.format("TimerJob[name=%s, suspended=%s, cancelled=%s, schedule=%s]",
            name, suspended.get(), cancelled.get(),
            getScheduleDirective().map(ScheduleDirective::getDescription).orElse("none"));
    }
} 