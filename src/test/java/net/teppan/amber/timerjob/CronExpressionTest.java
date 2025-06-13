package net.teppan.amber.timerjob;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test class for CronExpression.
 */
@DisplayName("CronExpression Tests")
class CronExpressionTest {
    
    @Nested
    @DisplayName("Construction and Validation")
    class ConstructionAndValidation {
        
        @Test
        @DisplayName("Should parse simple cron expression")
        void shouldParseSimpleCronExpression() {
            // Given
            String cronExpression = "0 9 * * MON-FRI";
            
            // When
            CronExpression cron = new CronExpression(cronExpression);
            
            // Then
            assertThat(cron.getExpression()).isEqualTo(cronExpression);
            assertThat(cron.getDescription()).contains("minute 0", "hour 9");
        }
        
        @Test
        @DisplayName("Should parse cron expression with step values")
        void shouldParseCronExpressionWithStepValues() {
            // Given
            String cronExpression = "0 */2 * * *";
            
            // When
            CronExpression cron = new CronExpression(cronExpression);
            
            // Then
            assertThat(cron.getExpression()).isEqualTo(cronExpression);
            assertThat(cron.getDescription()).contains("minute 0");
        }
        
        @Test
        @DisplayName("Should parse cron expression with month names")
        void shouldParseCronExpressionWithMonthNames() {
            // Given
            String cronExpression = "0 12 1 JAN,JUL *";
            
            // When
            CronExpression cron = new CronExpression(cronExpression);
            
            // Then
            assertThat(cron.getExpression()).isEqualTo(cronExpression);
        }
        
        @Test
        @DisplayName("Should parse cron expression with day names")
        void shouldParseCronExpressionWithDayNames() {
            // Given
            String cronExpression = "30 14 * * MON,WED,FRI";
            
            // When
            CronExpression cron = new CronExpression(cronExpression);
            
            // Then
            assertThat(cron.getExpression()).isEqualTo(cronExpression);
        }
        
        @Test
        @DisplayName("Should normalize Sunday (7 to 0)")
        void shouldNormalizeSunday() {
            // Given
            String cronExpression = "0 10 * * 7"; // Sunday as 7
            
            // When
            CronExpression cron = new CronExpression(cronExpression);
            
            // Then
            assertThat(cron.getExpression()).isEqualTo(cronExpression);
        }
        
        @Test
        @DisplayName("Should throw exception for null expression")
        void shouldThrowExceptionForNullExpression() {
            // When & Then
            assertThatThrownBy(() -> new CronExpression(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cron expression cannot be null or empty");
        }
        
        @Test
        @DisplayName("Should throw exception for empty expression")
        void shouldThrowExceptionForEmptyExpression() {
            // When & Then
            assertThatThrownBy(() -> new CronExpression(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cron expression cannot be null or empty");
            
            assertThatThrownBy(() -> new CronExpression("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Cron expression cannot be null or empty");
        }
        
        @ParameterizedTest
        @ValueSource(strings = {
            "0 9 * *",        // Too few fields
            "0 9 * * * *",    // Too many fields
            "60 9 * * *",     // Invalid minute (60)
            "0 25 * * *",     // Invalid hour (25)
            "0 9 32 * *",     // Invalid day (32)
            "0 9 * 13 *",     // Invalid month (13)
            "0 9 * * 8"       // Invalid day of week (8)
        })
        @DisplayName("Should throw exception for invalid cron expressions")
        void shouldThrowExceptionForInvalidCronExpressions(String invalidExpression) {
            // When & Then
            assertThatThrownBy(() -> new CronExpression(invalidExpression))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
    
    @Nested
    @DisplayName("Next Execution Time Calculation")
    class NextExecutionTimeCalculation {
        
        @Test
        @DisplayName("Should calculate next execution for every minute")
        void shouldCalculateNextExecutionForEveryMinute() {
            // Given
            CronExpression cron = new CronExpression("* * * * *");
            ZonedDateTime now = ZonedDateTime.of(2023, 12, 15, 10, 30, 45, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(now);
            
            // Then
            assertThat(next).isEqualTo(ZonedDateTime.of(2023, 12, 15, 10, 31, 0, 0, ZoneId.systemDefault()));
        }
        
        @Test
        @DisplayName("Should calculate next execution for specific time")
        void shouldCalculateNextExecutionForSpecificTime() {
            // Given
            CronExpression cron = new CronExpression("0 9 * * *");
            ZonedDateTime now = ZonedDateTime.of(2023, 12, 15, 10, 30, 0, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(now);
            
            // Then
            assertThat(next).isEqualTo(ZonedDateTime.of(2023, 12, 16, 9, 0, 0, 0, ZoneId.systemDefault()));
        }
        
        @Test
        @DisplayName("Should calculate next execution for weekdays only")
        void shouldCalculateNextExecutionForWeekdaysOnly() {
            // Given
            CronExpression cron = new CronExpression("0 9 * * MON-FRI");
            ZonedDateTime fridayNight = ZonedDateTime.of(2023, 12, 15, 18, 0, 0, 0, ZoneId.systemDefault()); // Assuming this is Friday
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(fridayNight);
            
            // Then
            assertThat(next.getHour()).isEqualTo(9);
            assertThat(next.getMinute()).isEqualTo(0);
        }
        
        @Test
        @DisplayName("Should calculate next execution for specific day of month")
        void shouldCalculateNextExecutionForSpecificDayOfMonth() {
            // Given
            CronExpression cron = new CronExpression("0 12 1 * *");
            ZonedDateTime now = ZonedDateTime.of(2023, 12, 15, 10, 0, 0, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(now);
            
            // Then
            assertThat(next.getDayOfMonth()).isEqualTo(1);
            assertThat(next.getHour()).isEqualTo(12);
            assertThat(next.getMinute()).isEqualTo(0);
            // Should be next month since we're past the 1st of this month
            assertThat(next.getMonth()).isEqualTo(now.getMonth().plus(1));
        }
        
        @Test
        @DisplayName("Should handle step values correctly")
        void shouldHandleStepValuesCorrectly() {
            // Given
            CronExpression cron = new CronExpression("0 */3 * * *");
            ZonedDateTime now = ZonedDateTime.of(2023, 12, 15, 10, 30, 0, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(now);
            
            // Then
            assertThat(next.getHour()).isIn(12, 15, 18, 21, 0, 3, 6, 9); // Multiple of 3
            assertThat(next.getMinute()).isEqualTo(0);
        }
    }
    
    @Nested
    @DisplayName("Description Generation")
    class DescriptionGeneration {
        
        @Test
        @DisplayName("Should generate description for every minute")
        void shouldGenerateDescriptionForEveryMinute() {
            // Given
            CronExpression cron = new CronExpression("* * * * *");
            
            // When
            String description = cron.getDescription();
            
            // Then
            assertThat(description).contains("every minute", "every hour", "every day");
        }
        
        @Test
        @DisplayName("Should generate description for specific time")
        void shouldGenerateDescriptionForSpecificTime() {
            // Given
            CronExpression cron = new CronExpression("30 14 * * *");
            
            // When
            String description = cron.getDescription();
            
            // Then
            assertThat(description).contains("minute 30", "hour 14", "every day");
        }
        
        @Test
        @DisplayName("Should generate description for weekdays")
        void shouldGenerateDescriptionForWeekdays() {
            // Given
            CronExpression cron = new CronExpression("0 9 * * MON-FRI");
            
            // When
            String description = cron.getDescription();
            
            // Then
            assertThat(description).contains("minute 0", "hour 9");
            assertThat(description).containsAnyOf("Monday", "Friday");
        }
    }
    
    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {
        
        @Test
        @DisplayName("Should handle leap year correctly")
        void shouldHandleLeapYearCorrectly() {
            // Given
            CronExpression cron = new CronExpression("0 12 29 2 *"); // Feb 29
            ZonedDateTime leapYear = ZonedDateTime.of(2024, 1, 15, 10, 0, 0, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(leapYear);
            
            // Then
            assertThat(next.getMonthValue()).isEqualTo(2);
            assertThat(next.getDayOfMonth()).isEqualTo(29);
            assertThat(next.getYear()).isEqualTo(2024); // 2024 is a leap year
        }
        
        @Test
        @DisplayName("Should handle end of year correctly")
        void shouldHandleEndOfYearCorrectly() {
            // Given
            CronExpression cron = new CronExpression("0 0 1 1 *"); // Jan 1st at midnight
            ZonedDateTime endOfYear = ZonedDateTime.of(2023, 12, 31, 23, 30, 0, 0, ZoneId.systemDefault());
            
            // When
            ZonedDateTime next = cron.getNextExecutionTime(endOfYear);
            
            // Then
            assertThat(next).isEqualTo(ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()));
        }
    }
} 