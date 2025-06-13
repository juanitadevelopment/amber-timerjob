package net.teppan.amber.timerjob;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Utility class for scheduling TimerJob instances based on ScheduleDirective configurations.
 * 
 * <p>This class handles the translation of schedule directives into actual Timer scheduling calls.
 * It supports various scheduling types including interval-based, time-based, date-based, and
 * cron-style scheduling.</p>
 * 
 * @since 1.0
 * @author Amber TimerJob
 */
public final class ScheduleExecutor {
    
    private static final Random random = new Random();
    
    // Time constants in milliseconds
    private static final long SECOND_MS = 1000L;
    private static final long MINUTE_MS = 60 * SECOND_MS;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long DAY_MS = 24 * HOUR_MS;
    
    // Default delay for EVERY schedules to avoid startup conflicts
    private static final long DEFAULT_DELAY_MS = MINUTE_MS + (random.nextLong(10 * SECOND_MS));
    
    /**
     * Private constructor to prevent instantiation.
     */
    private ScheduleExecutor() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Schedules a TimerJob using the provided Timer and ScheduleDirective.
     * 
     * @param timer the Timer instance to use for scheduling
     * @param job the TimerJob to schedule
     * @param directive the ScheduleDirective containing scheduling information
     * @throws SchedulingException if scheduling fails
     */
    public static void schedule(Timer timer, TimerJob job, ScheduleDirective directive) {
        try {
            switch (directive.type()) {
                case INTERVAL -> scheduleInterval(timer, job, directive);
                case TIME -> scheduleTime(timer, job, directive);
                case DATE -> scheduleDate(timer, job, directive);
                case CRON -> scheduleCron(timer, job, directive);
                case NONE -> {
                    // No scheduling for disabled jobs
                }
                default -> throw new SchedulingException("Unsupported schedule type: " + directive.type());
            }
        } catch (Exception e) {
            if (e instanceof SchedulingException) {
                throw e;
            }
            throw new SchedulingException("Failed to schedule job", e);
        }
    }
    
    /**
     * Schedules an interval-based job (EVERY n UNIT).
     */
    private static void scheduleInterval(Timer timer, TimerJob job, ScheduleDirective directive) {
        long intervalMs = directive.intervalMillis()
            .orElseThrow(() -> new SchedulingException("Interval not specified for INTERVAL schedule"));
        
        if (intervalMs <= 0) {
            throw new SchedulingException("Invalid interval: " + intervalMs);
        }
        
        // Add random delay to prevent startup conflicts
        long delay = DEFAULT_DELAY_MS;
        timer.schedule(job, delay, intervalMs);
    }
    
    /**
     * Schedules a time-based job (AT hh:mm [EVERY days]).
     */
    private static void scheduleTime(Timer timer, TimerJob job, ScheduleDirective directive) {
        int hour = directive.hour()
            .orElseThrow(() -> new SchedulingException("Hour not specified for TIME schedule"));
        int minute = directive.minute()
            .orElseThrow(() -> new SchedulingException("Minute not specified for TIME schedule"));
        
        Date nextExecution = calculateNextTimeExecution(hour, minute, directive.timeZone());
        timer.schedule(job, nextExecution, DAY_MS);
    }
    
    /**
     * Schedules a date-based job (AT hh:mm ON date).
     */
    private static void scheduleDate(Timer timer, TimerJob job, ScheduleDirective directive) {
        int hour = directive.hour()
            .orElseThrow(() -> new SchedulingException("Hour not specified for DATE schedule"));
        int minute = directive.minute()
            .orElseThrow(() -> new SchedulingException("Minute not specified for DATE schedule"));
        
        Date nextExecution = calculateNextDateExecution(hour, minute, 
            directive.dayOfMonth(), directive.month(), directive.timeZone());
        timer.schedule(job, nextExecution, DAY_MS);
    }
    
    /**
     * Schedules a cron-based job.
     */
    private static void scheduleCron(Timer timer, TimerJob job, ScheduleDirective directive) {
        String cronExpressionStr = directive.cronExpression()
            .orElseThrow(() -> new SchedulingException("Cron expression not specified for CRON schedule"));
        
        try {
            CronExpression cronExpression = new CronExpression(cronExpressionStr);
            Date nextExecution = calculateNextCronExecution(cronExpression, directive.timeZone());
            
            // Schedule using a custom TimerTask that reschedules itself
            timer.schedule(new CronTimerTask(timer, job, cronExpression, directive.timeZone()), nextExecution);
            
        } catch (Exception e) {
            throw new SchedulingException("Failed to parse cron expression: " + cronExpressionStr, e);
        }
    }
    
    /**
     * Calculates the next execution time for a time-based schedule.
     */
    private static Date calculateNextTimeExecution(int hour, int minute, ZoneId timeZone) {
        ZonedDateTime now = ZonedDateTime.now(timeZone);
        ZonedDateTime nextExecution = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        
        // If the time has already passed today, schedule for tomorrow
        if (nextExecution.isBefore(now) || nextExecution.isEqual(now)) {
            nextExecution = nextExecution.plusDays(1);
        }
        
        return Date.from(nextExecution.toInstant());
    }
    
    /**
     * Calculates the next execution time for a date-based schedule.
     */
    private static Date calculateNextDateExecution(int hour, int minute, 
            java.util.Optional<Integer> dayOfMonth, java.util.Optional<Integer> month, ZoneId timeZone) {
        
        ZonedDateTime now = ZonedDateTime.now(timeZone);
        ZonedDateTime nextExecution = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        
        if (month.isPresent() && dayOfMonth.isPresent()) {
            // Specific month and day (e.g., 12/25)
            nextExecution = nextExecution.withMonth(month.get()).withDayOfMonth(dayOfMonth.get());
            
            // If this date has passed this year, schedule for next year
            if (nextExecution.isBefore(now) || nextExecution.isEqual(now)) {
                nextExecution = nextExecution.plusYears(1);
            }
        } else if (dayOfMonth.isPresent()) {
            // Specific day of month (e.g., every 15th)
            nextExecution = nextExecution.withDayOfMonth(dayOfMonth.get());
            
            // If this day has passed this month, schedule for next month
            if (nextExecution.isBefore(now) || nextExecution.isEqual(now)) {
                nextExecution = nextExecution.plusMonths(1);
            }
        } else {
            // Default to daily execution
            if (nextExecution.isBefore(now) || nextExecution.isEqual(now)) {
                nextExecution = nextExecution.plusDays(1);
            }
        }
        
        return Date.from(nextExecution.toInstant());
    }
    
    /**
     * Calculates the next execution time for a cron-based schedule.
     */
    private static Date calculateNextCronExecution(CronExpression cronExpression, ZoneId timeZone) {
        ZonedDateTime now = ZonedDateTime.now(timeZone);
        ZonedDateTime nextExecution = cronExpression.getNextExecutionTime(now);
        return Date.from(nextExecution.toInstant());
    }
    
    /**
     * Parses a time unit string and returns the corresponding milliseconds.
     * 
     * @param value the numeric value
     * @param unit the time unit (SEC, MIN, HR, DAY)
     * @return milliseconds
     * @throws SchedulingException if the unit is not recognized
     */
    public static long parseTimeUnit(int value, String unit) {
        return switch (unit.toUpperCase()) {
            case "SEC" -> value * SECOND_MS;
            case "MIN" -> value * MINUTE_MS;
            case "HR" -> value * HOUR_MS;
            case "DAY" -> value * DAY_MS;
            default -> throw new SchedulingException("Unknown time unit: " + unit);
        };
    }
    
    /**
     * Parses a day-of-week string and returns the corresponding Calendar constant.
     * 
     * @param dayOfWeek the day of week string (MON, TUE, etc.)
     * @return Calendar day constant (Calendar.MONDAY, etc.)
     * @throws SchedulingException if the day is not recognized
     */
    public static int parseDayOfWeek(String dayOfWeek) {
        return switch (dayOfWeek.toUpperCase()) {
            case "MON" -> Calendar.MONDAY;
            case "TUE" -> Calendar.TUESDAY;
            case "WED" -> Calendar.WEDNESDAY;
            case "THU" -> Calendar.THURSDAY;
            case "FRI" -> Calendar.FRIDAY;
            case "SAT" -> Calendar.SATURDAY;
            case "SUN" -> Calendar.SUNDAY;
            default -> throw new SchedulingException("Unknown day of week: " + dayOfWeek);
        };
    }
    
    /**
     * Custom TimerTask for cron-based scheduling that reschedules itself after each execution.
     */
    private static class CronTimerTask extends TimerTask {
        private final Timer timer;
        private final TimerJob job;
        private final CronExpression cronExpression;
        private final ZoneId timeZone;
        
        public CronTimerTask(Timer timer, TimerJob job, CronExpression cronExpression, ZoneId timeZone) {
            this.timer = timer;
            this.job = job;
            this.cronExpression = cronExpression;
            this.timeZone = timeZone;
        }
        
        @Override
        public void run() {
            try {
                // Execute the job
                job.run();
                
                // Schedule next execution
                if (!job.isCancelled()) {
                    Date nextExecution = calculateNextCronExecution(cronExpression, timeZone);
                    timer.schedule(new CronTimerTask(timer, job, cronExpression, timeZone), nextExecution);
                }
            } catch (Exception e) {
                // Log error but continue scheduling
                if (job.getJobContext().isPresent()) {
                    job.getJobContext().get().getLogger().error("Error in cron job execution", e);
                }
                
                // Schedule next execution even after error
                if (!job.isCancelled()) {
                    try {
                        Date nextExecution = calculateNextCronExecution(cronExpression, timeZone);
                        timer.schedule(new CronTimerTask(timer, job, cronExpression, timeZone), nextExecution);
                    } catch (Exception scheduleError) {
                        if (job.getJobContext().isPresent()) {
                            job.getJobContext().get().getLogger().error("Failed to reschedule cron job", scheduleError);
                        }
                    }
                }
            }
        }
    }
} 