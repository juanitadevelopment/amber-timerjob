package net.teppan.amber.timerjob;

import java.time.ZoneId;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test class for ScheduleDirective record.
 */
@DisplayName("ScheduleDirective Tests")
class ScheduleDirectiveTest {
    
    @Nested
    @DisplayName("Construction and Validation")
    class ConstructionAndValidation {
        
        @Test
        @DisplayName("Should create valid INTERVAL directive")
        void shouldCreateValidIntervalDirective() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "EVERY 15 MIN",
                ScheduleDirective.ScheduleType.INTERVAL,
                Optional.of(15 * 60 * 1000L), // 15 minutes in milliseconds
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // Then
            assertThat(directive.originalDirective()).isEqualTo("EVERY 15 MIN");
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.INTERVAL);
            assertThat(directive.intervalMillis()).contains(15 * 60 * 1000L);
            assertThat(directive.isActive()).isTrue();
        }
        
        @Test
        @DisplayName("Should create valid TIME directive")
        void shouldCreateValidTimeDirective() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "AT 09:30",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(9),
                Optional.of(30),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.of("Asia/Tokyo"),
                Locale.JAPAN
            );
            
            // Then
            assertThat(directive.originalDirective()).isEqualTo("AT 09:30");
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.TIME);
            assertThat(directive.hour()).contains(9);
            assertThat(directive.minute()).contains(30);
            assertThat(directive.timeZone()).isEqualTo(ZoneId.of("Asia/Tokyo"));
            assertThat(directive.locale()).isEqualTo(Locale.JAPAN);
            assertThat(directive.isActive()).isTrue();
        }
        
        @Test
        @DisplayName("Should create valid DATE directive")
        void shouldCreateValidDateDirective() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "AT 12:00 ON 15",
                ScheduleDirective.ScheduleType.DATE,
                Optional.empty(),
                Optional.of(12),
                Optional.of(0),
                Set.of(),
                Optional.of(15),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // Then
            assertThat(directive.originalDirective()).isEqualTo("AT 12:00 ON 15");
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.DATE);
            assertThat(directive.hour()).contains(12);
            assertThat(directive.minute()).contains(0);
            assertThat(directive.dayOfMonth()).contains(15);
            assertThat(directive.isActive()).isTrue();
        }
        
        @Test
        @DisplayName("Should create valid CRON directive")
        void shouldCreateValidCronDirective() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "CRON 0 9 * * MON-FRI",
                ScheduleDirective.ScheduleType.CRON,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("0 9 * * MON-FRI"),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // Then
            assertThat(directive.originalDirective()).isEqualTo("CRON 0 9 * * MON-FRI");
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.CRON);
            assertThat(directive.cronExpression()).contains("0 9 * * MON-FRI");
            assertThat(directive.isActive()).isTrue();
        }
        
        @Test
        @DisplayName("Should create NONE directive as inactive")
        void shouldCreateNoneDirectiveAsInactive() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "",
                ScheduleDirective.ScheduleType.NONE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // Then
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.NONE);
            assertThat(directive.isActive()).isFalse();
        }
        
        @Test
        @DisplayName("Should throw exception for null originalDirective")
        void shouldThrowExceptionForNullOriginalDirective() {
            // When & Then
            assertThatThrownBy(() -> new ScheduleDirective(
                null,
                ScheduleDirective.ScheduleType.INTERVAL,
                Optional.of(1000L),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Original directive cannot be null");
        }
        
        @Test
        @DisplayName("Should apply default values for null parameters")
        void shouldApplyDefaultValuesForNullParameters() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "test",
                null, // Will default to NONE
                null, // Will default to Optional.empty()
                null, // Will default to Optional.empty()
                null, // Will default to Optional.empty()
                null, // Will default to Set.of()
                null, // Will default to Optional.empty()
                null, // Will default to Optional.empty()
                null, // Will default to Optional.empty()
                null, // Will default to system default
                null  // Will default to system default
            );
            
            // Then
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.NONE);
            assertThat(directive.intervalMillis()).isEqualTo(Optional.empty());
            assertThat(directive.hour()).isEqualTo(Optional.empty());
            assertThat(directive.minute()).isEqualTo(Optional.empty());
            assertThat(directive.daysOfWeek()).isEqualTo(Set.of());
            assertThat(directive.dayOfMonth()).isEqualTo(Optional.empty());
            assertThat(directive.month()).isEqualTo(Optional.empty());
            assertThat(directive.cronExpression()).isEqualTo(Optional.empty());
            assertThat(directive.timeZone()).isEqualTo(ZoneId.systemDefault());
            assertThat(directive.locale()).isEqualTo(Locale.getDefault());
        }
    }
    
    @Nested
    @DisplayName("Validation Rules")
    class ValidationRules {
        
        @Test
        @DisplayName("Should throw exception for INTERVAL with zero or negative interval")
        void shouldThrowExceptionForInvalidInterval() {
            // When & Then
            assertThatThrownBy(() -> new ScheduleDirective(
                "EVERY 0 MIN",
                ScheduleDirective.ScheduleType.INTERVAL,
                Optional.of(0L),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Interval must be positive for INTERVAL type");
            
            assertThatThrownBy(() -> new ScheduleDirective(
                "EVERY -5 MIN",
                ScheduleDirective.ScheduleType.INTERVAL,
                Optional.of(-5000L),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Interval must be positive for INTERVAL type");
        }
        
        @Test
        @DisplayName("Should throw exception for TIME without hour or minute")
        void shouldThrowExceptionForTimeWithoutHourOrMinute() {
            // When & Then - Missing hour
            assertThatThrownBy(() -> new ScheduleDirective(
                "AT :30",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.empty(),
                Optional.of(30),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Hour and minute must be specified for TIME/DATE type");
            
            // When & Then - Missing minute
            assertThatThrownBy(() -> new ScheduleDirective(
                "AT 09:",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(9),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Hour and minute must be specified for TIME/DATE type");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {-1, 24, 25, 100})
        @DisplayName("Should throw exception for invalid hour values")
        void shouldThrowExceptionForInvalidHour(int invalidHour) {
            // When & Then
            assertThatThrownBy(() -> new ScheduleDirective(
                "AT " + invalidHour + ":00",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(invalidHour),
                Optional.of(0),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Hour must be between 0 and 23");
        }
        
        @ParameterizedTest
        @ValueSource(ints = {-1, 60, 61, 100})
        @DisplayName("Should throw exception for invalid minute values")
        void shouldThrowExceptionForInvalidMinute(int invalidMinute) {
            // When & Then
            assertThatThrownBy(() -> new ScheduleDirective(
                "AT 09:" + invalidMinute,
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(9),
                Optional.of(invalidMinute),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Minute must be between 0 and 59");
        }
        
        @Test
        @DisplayName("Should throw exception for CRON without expression")
        void shouldThrowExceptionForCronWithoutExpression() {
            // When & Then
            assertThatThrownBy(() -> new ScheduleDirective(
                "CRON",
                ScheduleDirective.ScheduleType.CRON,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessage("Cron expression must be specified for CRON type");
        }
    }
    
    @Nested
    @DisplayName("Description Generation")
    class DescriptionGeneration {
        
        @Test
        @DisplayName("Should generate correct description for INTERVAL type")
        void shouldGenerateCorrectDescriptionForInterval() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "EVERY 15 MIN",
                ScheduleDirective.ScheduleType.INTERVAL,
                Optional.of(15 * 60 * 1000L),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("Every 900000 milliseconds");
        }
        
        @Test
        @DisplayName("Should generate correct description for TIME type")
        void shouldGenerateCorrectDescriptionForTime() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "AT 09:30",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(9),
                Optional.of(30),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("At 09:30 daily");
        }
        
        @Test
        @DisplayName("Should generate correct description for TIME type with days")
        void shouldGenerateCorrectDescriptionForTimeWithDays() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "AT 09:30 EVERY MON|FRI",
                ScheduleDirective.ScheduleType.TIME,
                Optional.empty(),
                Optional.of(9),
                Optional.of(30),
                Set.of(1, 5), // Monday and Friday
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("At 09:30 on FRI|MON");
        }
        
        @Test
        @DisplayName("Should generate correct description for DATE type")
        void shouldGenerateCorrectDescriptionForDate() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "AT 12:00 ON 25/12",
                ScheduleDirective.ScheduleType.DATE,
                Optional.empty(),
                Optional.of(12),
                Optional.of(0),
                Set.of(),
                Optional.of(25),
                Optional.of(12),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("At 12:00 on 12/25");
        }
        
        @Test
        @DisplayName("Should generate correct description for CRON type")
        void shouldGenerateCorrectDescriptionForCron() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "CRON 0 9 * * MON-FRI",
                ScheduleDirective.ScheduleType.CRON,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("0 9 * * MON-FRI"),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("Cron: 0 9 * * MON-FRI");
        }
        
        @Test
        @DisplayName("Should generate correct description for NONE type")
        void shouldGenerateCorrectDescriptionForNone() {
            // Given
            ScheduleDirective directive = new ScheduleDirective(
                "",
                ScheduleDirective.ScheduleType.NONE,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Set.of(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ZoneId.systemDefault(),
                Locale.getDefault()
            );
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("Disabled");
        }
    }
    
    @Nested
    @DisplayName("Cron Integration Tests")
    class CronIntegrationTests {
        
        @Test
        @DisplayName("Should validate valid cron expressions")
        void shouldValidateValidCronExpressions() {
            // Given
            String[] validExpressions = {
                "0 9 * * MON-FRI",
                "30 14 1 * *",
                "0 */2 * * *",
                "15,45 * * * *",
                "0 12 1 JAN,JUL *"
            };
            
            for (String expression : validExpressions) {
                // When & Then - Should not throw exception
                assertThatCode(() -> new ScheduleDirective(
                    "CRON " + expression,
                    ScheduleDirective.ScheduleType.CRON,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    Set.of(),
                    Optional.empty(),
                    Optional.empty(),
                    Optional.of(expression),
                    ZoneId.systemDefault(),
                    Locale.getDefault()
                )).doesNotThrowAnyException();
            }
        }
        
        @Test
        @DisplayName("Should validate cron directive with proper description")
        void shouldValidateCronDirectiveWithProperDescription() {
            // Given
            String cronExpression = "0 9 * * MON-FRI";
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
            
            // When
            String description = directive.getDescription();
            
            // Then
            assertThat(description).isEqualTo("Cron: " + cronExpression);
            assertThat(directive.isActive()).isTrue();
            assertThat(directive.type()).isEqualTo(ScheduleDirective.ScheduleType.CRON);
        }
    }
} 