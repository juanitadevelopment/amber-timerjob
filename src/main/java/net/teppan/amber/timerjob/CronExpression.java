package net.teppan.amber.timerjob;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cron expression parser and scheduler.
 * 
 * Supports standard 5-field cron expressions: minute hour day month day-of-week.
 * 
 * Minute: 0-59
 * Hour: 0-23
 * Day of month: 1-31
 * Month: 1-12 (or JAN-DEC)
 * Day of week: 0-7 (0 and 7 are Sunday, or SUN-SAT)
 * 
 * Special characters:
 * * - matches all values
 * , - separates multiple values
 * - - defines ranges
 * / - defines step values
 * 
 * Examples:
 * 0 9 * * MON-FRI - 9:00 AM Monday through Friday
 * 30 14 1 * * - 2:30 PM on the 1st of every month
 * 0 star/2 * * * - Every 2 hours (star represents asterisk)
 * 15,45 * * * * - At 15 and 45 minutes past each hour
 * 
 * @since 1.0
 * @author Juanita Development
 */
public final class CronExpression {
    
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");
    
    // Month and day-of-week name mappings
    private static final Map<String, Integer> MONTH_NAMES = Map.ofEntries(
        Map.entry("JAN", 1), Map.entry("FEB", 2), Map.entry("MAR", 3), Map.entry("APR", 4),
        Map.entry("MAY", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AUG", 8),
        Map.entry("SEP", 9), Map.entry("OCT", 10), Map.entry("NOV", 11), Map.entry("DEC", 12)
    );
    
    private static final Map<String, Integer> DAY_NAMES = Map.of(
        "SUN", 0, "MON", 1, "TUE", 2, "WED", 3,
        "THU", 4, "FRI", 5, "SAT", 6
    );
    
    private final String originalExpression;
    private final Set<Integer> minutes;
    private final Set<Integer> hours;
    private final Set<Integer> daysOfMonth;
    private final Set<Integer> months;
    private final Set<Integer> daysOfWeek;
    
    /**
     * Parses a cron expression.
     * 
     * @param cronExpression the cron expression string
     * @throws IllegalArgumentException if the expression is invalid
     */
    public CronExpression(String cronExpression) {
        if (cronExpression == null || cronExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("Cron expression cannot be null or empty");
        }
        
        this.originalExpression = cronExpression.trim();
        String[] fields = WHITESPACE.split(this.originalExpression);
        
        if (fields.length != 5) {
            throw new IllegalArgumentException(
                "Cron expression must have 5 fields (minute hour day month day-of-week), got " + fields.length);
        }
        
        try {
            this.minutes = parseField(fields[0], 0, 59, Collections.emptyMap());
            this.hours = parseField(fields[1], 0, 23, Collections.emptyMap());
            this.daysOfMonth = parseField(fields[2], 1, 31, Collections.emptyMap());
            this.months = parseField(fields[3], 1, 12, MONTH_NAMES);
            
            // Parse and normalize days of week (convert 7 to 0 for Sunday)
            Set<Integer> parsedDaysOfWeek = parseField(fields[4], 0, 7, DAY_NAMES);
            if (parsedDaysOfWeek.contains(7)) {
                Set<Integer> normalizedDays = new HashSet<>(parsedDaysOfWeek);
                normalizedDays.remove(7);
                normalizedDays.add(0);
                this.daysOfWeek = Set.copyOf(normalizedDays);
            } else {
                this.daysOfWeek = parsedDaysOfWeek;
            }
            
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cronExpression, e);
        }
    }
    
    /**
     * Parses a single cron field.
     */
    private Set<Integer> parseField(String field, int min, int max, Map<String, Integer> names) {
        if ("*".equals(field)) {
            return createRange(min, max);
        }
        
        Set<Integer> values = new HashSet<>();
        String[] parts = field.split(",");
        
        for (String part : parts) {
            part = part.trim();
            if (part.contains("/")) {
                // Step values (e.g., */2, 10-20/3)
                String[] stepParts = part.split("/");
                if (stepParts.length != 2) {
                    throw new IllegalArgumentException("Invalid step expression: " + part);
                }
                
                Set<Integer> baseValues;
                if ("*".equals(stepParts[0])) {
                    baseValues = createRange(min, max);
                } else {
                    baseValues = parseSimpleField(stepParts[0], min, max, names);
                }
                
                int step = Integer.parseInt(stepParts[1]);
                if (step <= 0) {
                    throw new IllegalArgumentException("Step value must be positive: " + step);
                }
                
                List<Integer> sortedValues = new ArrayList<>(baseValues);
                Collections.sort(sortedValues);
                for (int i = 0; i < sortedValues.size(); i += step) {
                    values.add(sortedValues.get(i));
                }
            } else {
                values.addAll(parseSimpleField(part, min, max, names));
            }
        }
        
        return Set.copyOf(values);
    }
    
    /**
     * Parses a simple field (no step values).
     */
    private Set<Integer> parseSimpleField(String field, int min, int max, Map<String, Integer> names) {
        if (field.contains("-")) {
            // Range (e.g., 1-5, MON-FRI)
            String[] rangeParts = field.split("-");
            if (rangeParts.length != 2) {
                throw new IllegalArgumentException("Invalid range expression: " + field);
            }
            
            int start = parseValue(rangeParts[0].trim(), names);
            int end = parseValue(rangeParts[1].trim(), names);
            
            if (start > end) {
                throw new IllegalArgumentException("Range start must be <= end: " + field);
            }
            
            validateRange(start, min, max);
            validateRange(end, min, max);
            
            return createRange(start, end);
        } else {
            // Single value
            int value = parseValue(field, names);
            validateRange(value, min, max);
            return Set.of(value);
        }
    }
    
    /**
     * Parses a single value, considering name mappings.
     */
    private int parseValue(String value, Map<String, Integer> names) {
        if (names.containsKey(value.toUpperCase())) {
            return names.get(value.toUpperCase());
        }
        
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid value: " + value);
        }
    }
    
    /**
     * Validates that a value is within the allowed range.
     */
    private void validateRange(int value, int min, int max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                String.format("Value %d is out of range [%d-%d]", value, min, max));
        }
    }
    
    /**
     * Creates a set containing all integers from start to end (inclusive).
     */
    private Set<Integer> createRange(int start, int end) {
        Set<Integer> range = new HashSet<>();
        for (int i = start; i <= end; i++) {
            range.add(i);
        }
        return range;
    }
    
    /**
     * Calculates the next execution time after the given time.
     * 
     * @param after the time after which to find the next execution
     * @return the next execution time
     */
    public ZonedDateTime getNextExecutionTime(ZonedDateTime after) {
        ZonedDateTime next = after.truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        
        // Find the next valid time, with a reasonable limit to prevent infinite loops
        int attempts = 0;
        final int maxAttempts = 366 * 24 * 60; // One year worth of minutes
        
        while (attempts < maxAttempts) {
            if (matches(next)) {
                return next;
            }
            next = next.plusMinutes(1);
            attempts++;
        }
        
        throw new IllegalStateException("Unable to find next execution time for cron expression: " + originalExpression);
    }
    
    /**
     * Checks if the given time matches this cron expression.
     */
    private boolean matches(ZonedDateTime time) {
        boolean minuteMatch = minutes.contains(time.getMinute());
        boolean hourMatch = hours.contains(time.getHour());
        boolean monthMatch = months.contains(time.getMonthValue());
        
        // Handle day-of-month and day-of-week matching
        boolean dayOfMonthMatch = daysOfMonth.contains(time.getDayOfMonth());
        boolean dayOfWeekMatch = daysOfWeek.contains(time.getDayOfWeek().getValue() % 7);
        
        // If both day-of-month and day-of-week are specified (not wildcards),
        // then either can match (OR condition)
        // If only one is specified, then only that one needs to match
        boolean dayMatch;
        boolean isMonthWildcard = daysOfMonth.size() == 31; // All days 1-31
        boolean isWeekWildcard = daysOfWeek.size() == 7; // All days 0-6
        
        if (!isMonthWildcard && !isWeekWildcard) {
            // Both are specified, either can match
            dayMatch = dayOfMonthMatch || dayOfWeekMatch;
        } else if (!isMonthWildcard) {
            // Only day-of-month is specified
            dayMatch = dayOfMonthMatch;
        } else if (!isWeekWildcard) {
            // Only day-of-week is specified
            dayMatch = dayOfWeekMatch;
        } else {
            // Both are wildcards, always matches
            dayMatch = true;
        }
        
        return minuteMatch && hourMatch && dayMatch && monthMatch;
    }
    
    /**
     * Returns the original cron expression string.
     */
    public String getExpression() {
        return originalExpression;
    }
    
    /**
     * Returns a human-readable description of this cron expression.
     */
    public String getDescription() {
        StringBuilder desc = new StringBuilder();
        
        // Minutes
        if (minutes.size() == 60) {
            desc.append("every minute");
        } else if (minutes.size() == 1) {
            desc.append("at minute ").append(minutes.iterator().next());
        } else {
            desc.append("at minutes ").append(formatSet(minutes));
        }
        
        // Hours
        if (hours.size() == 24) {
            desc.append(" of every hour");
        } else if (hours.size() == 1) {
            desc.append(" of hour ").append(hours.iterator().next());
        } else {
            desc.append(" of hours ").append(formatSet(hours));
        }
        
        // Days and months
        if (daysOfMonth.size() == 31 && months.size() == 12) {
            desc.append(" every day");
        } else {
            if (daysOfMonth.size() < 31) {
                desc.append(" on day(s) ").append(formatSet(daysOfMonth));
            }
            if (months.size() < 12) {
                desc.append(" of month(s) ").append(formatSet(months));
            }
        }
        
        // Days of week
        if (daysOfWeek.size() < 7) {
            desc.append(" on ").append(formatDaysOfWeek(daysOfWeek));
        }
        
        return desc.toString();
    }
    
    /**
     * Formats a set of integers as a string.
     */
    private String formatSet(Set<Integer> values) {
        List<Integer> sorted = new ArrayList<>(values);
        Collections.sort(sorted);
        return sorted.toString();
    }
    
    /**
     * Formats days of week as a readable string.
     */
    private String formatDaysOfWeek(Set<Integer> days) {
        List<String> dayNames = new ArrayList<>();
        for (int day : days) {
            dayNames.add(switch (day) {
                case 0 -> "Sunday";
                case 1 -> "Monday";
                case 2 -> "Tuesday";
                case 3 -> "Wednesday";
                case 4 -> "Thursday";
                case 5 -> "Friday";
                case 6 -> "Saturday";
                default -> String.valueOf(day);
            });
        }
        Collections.sort(dayNames);
        return String.join(", ", dayNames);
    }
    
    @Override
    public String toString() {
        return "CronExpression{" + originalExpression + "}";
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CronExpression that = (CronExpression) o;
        return Objects.equals(originalExpression, that.originalExpression);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(originalExpression);
    }
} 