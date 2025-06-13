package net.teppan.amber.timerjob;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Immutable schedule directive record that represents when and how a TimerJob should be executed.
 * 
 * <p>Supports various scheduling formats including:</p>
 * <ul>
 *   <li>Interval-based: {@code EVERY 15 MIN}, {@code EVERY 2 HR}</li>
 *   <li>Time-based: {@code AT 09:30}, {@code AT 14:00 EVERY MON|FRI}</li>
 *   <li>Date-based: {@code AT 12:00 ON 15}, {@code AT 08:00 ON 12/25}</li>
 *   <li>Cron-based: {@code CRON 0 9 * * MON-FRI}</li>
 * </ul>
 * 
 * @param originalDirective the original schedule directive string
 * @param type the type of schedule (INTERVAL, TIME, DATE, CRON)
 * @param intervalMillis interval in milliseconds for INTERVAL type
 * @param hour execution hour (0-23) for TIME/DATE types
 * @param minute execution minute (0-59) for TIME/DATE types
 * @param daysOfWeek set of days of week (1=Monday, 7=Sunday) for TIME type
 * @param dayOfMonth day of month (1-31) for DATE type, null for any day
 * @param month month (1-12) for DATE type, null for any month
 * @param cronExpression parsed cron expression for CRON type
 * @param timeZone time zone for execution
 * @param locale locale for execution
 * 
 * @since 1.0
 * @author Amber TimerJob
 */
public record ScheduleDirective(
    String originalDirective,
    ScheduleType type,
    Optional<Long> intervalMillis,
    Optional<Integer> hour,
    Optional<Integer> minute,
    Set<Integer> daysOfWeek,
    Optional<Integer> dayOfMonth,
    Optional<Integer> month,
    Optional<String> cronExpression,
    ZoneId timeZone,
    Locale locale
) {
    
    /**
     * Schedule type enumeration.
     */
    public enum ScheduleType {
        /** Interval-based scheduling (EVERY n UNIT) */
        INTERVAL,
        /** Time-based scheduling (AT hh:mm [EVERY days]) */
        TIME,
        /** Date-based scheduling (AT hh:mm ON date) */
        DATE,
        /** Cron-based scheduling (CRON expression) */
        CRON,
        /** No scheduling / disabled */
        NONE
    }
    
    /**
     * Creates a new ScheduleDirective with validation.
     */
    public ScheduleDirective {
        // Validate required fields
        if (originalDirective == null) {
            throw new IllegalArgumentException("Original directive cannot be null");
        }
        
        // Apply default values for null parameters
        if (type == null) {
            type = ScheduleType.NONE;
        }
        if (timeZone == null) {
            timeZone = ZoneId.systemDefault();
        }
        if (locale == null) {
            locale = Locale.getDefault();
        }
        if (daysOfWeek == null) {
            daysOfWeek = Set.of();
        }
        if (intervalMillis == null) {
            intervalMillis = Optional.empty();
        }
        if (hour == null) {
            hour = Optional.empty();
        }
        if (minute == null) {
            minute = Optional.empty();
        }
        if (dayOfMonth == null) {
            dayOfMonth = Optional.empty();
        }
        if (month == null) {
            month = Optional.empty();
        }
        if (cronExpression == null) {
            cronExpression = Optional.empty();
        }
        
        // Validation using local parameters (not this.field)
        validateScheduleDirective(type, intervalMillis, hour, minute, cronExpression);
    }
    
    /**
     * Validates the schedule directive configuration.
     */
    private static void validateScheduleDirective(ScheduleType type, Optional<Long> intervalMillis, 
                                                 Optional<Integer> hour, Optional<Integer> minute,
                                                 Optional<String> cronExpression) {
        switch (type) {
            case INTERVAL -> {
                if (intervalMillis.isEmpty() || intervalMillis.get() <= 0) {
                    throw new IllegalArgumentException("Interval must be positive for INTERVAL type");
                }
            }
            case TIME, DATE -> {
                if (hour.isEmpty() || minute.isEmpty()) {
                    throw new IllegalArgumentException("Hour and minute must be specified for TIME/DATE type");
                }
                validateTimeValues(hour, minute);
            }
            case CRON -> {
                if (cronExpression.isEmpty()) {
                    throw new IllegalArgumentException("Cron expression must be specified for CRON type");
                }
            }
            case NONE -> {
                // No validation needed for disabled schedule
            }
        }
    }
    
    /**
     * Validates hour and minute values.
     */
    private static void validateTimeValues(Optional<Integer> hour, Optional<Integer> minute) {
        if (hour.isPresent()) {
            int h = hour.get();
            if (h < 0 || h > 23) {
                throw new IllegalArgumentException("Hour must be between 0 and 23");
            }
        }
        if (minute.isPresent()) {
            int m = minute.get();
            if (m < 0 || m > 59) {
                throw new IllegalArgumentException("Minute must be between 0 and 59");
            }
        }
    }
    
    /**
     * Returns true if this directive represents an active schedule.
     */
    public boolean isActive() {
        return type != ScheduleType.NONE;
    }
    
    /**
     * Returns a human-readable description of this schedule.
     */
    public String getDescription() {
        return switch (type) {
            case INTERVAL -> String.format("Every %d milliseconds", intervalMillis.orElse(0L));
            case TIME -> String.format("At %02d:%02d%s", 
                hour.orElse(0), minute.orElse(0), 
                daysOfWeek.isEmpty() ? " daily" : " on " + formatDaysOfWeek());
            case DATE -> String.format("At %02d:%02d on %s", 
                hour.orElse(0), minute.orElse(0), formatDate());
            case CRON -> String.format("Cron: %s", cronExpression.orElse(""));
            case NONE -> "Disabled";
        };
    }
    
    /**
     * Formats days of week for display.
     */
    private String formatDaysOfWeek() {
        if (daysOfWeek.isEmpty()) {
            return "every day";
        }
        return daysOfWeek.stream()
            .map(day -> switch (day) {
                case 1 -> "MON";
                case 2 -> "TUE";
                case 3 -> "WED";
                case 4 -> "THU";
                case 5 -> "FRI";
                case 6 -> "SAT";
                case 7 -> "SUN";
                default -> "?";
            })
            .sorted()
            .reduce((a, b) -> a + "|" + b)
            .orElse("every day");
    }
    
    /**
     * Formats date for display.
     */
    private String formatDate() {
        if (month.isPresent() && dayOfMonth.isPresent()) {
            return String.format("%02d/%02d", month.get(), dayOfMonth.get());
        } else if (dayOfMonth.isPresent()) {
            return String.format("day %d", dayOfMonth.get());
        }
        return "every day";
    }
} 