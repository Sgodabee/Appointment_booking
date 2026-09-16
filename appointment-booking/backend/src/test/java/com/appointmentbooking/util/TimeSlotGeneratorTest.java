package com.appointmentbooking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TimeSlotGenerator")
class TimeSlotGeneratorTest {

    private final TimeSlotGenerator generator = new TimeSlotGenerator();

    // Known weekday (Tuesday) and Saturday / Sunday
    private static final LocalDate WEEKDAY  = LocalDate.of(2026, 10, 6);  // Tuesday
    private static final LocalDate SATURDAY = LocalDate.of(2026, 10, 10); // Saturday
    private static final LocalDate SUNDAY   = LocalDate.of(2026, 10, 11); // Sunday

    @Test
    @DisplayName("weekday generates 18 slots (08:00–16:30)")
    void weekdaySlotCount() {
        List<String> slots = generator.generateAvailable(WEEKDAY, List.of());
        assertThat(slots).hasSize(18);
    }

    @Test
    @DisplayName("weekday includes 08:00 and 16:30")
    void weekdayBoundaries() {
        List<String> slots = generator.generateAvailable(WEEKDAY, List.of());
        assertThat(slots).contains("08:00", "08:30", "16:30");
        assertThat(slots).doesNotContain("17:00", "07:30");
    }

    @Test
    @DisplayName("Saturday generates 8 slots (09:00–12:30)")
    void saturdaySlotCount() {
        List<String> slots = generator.generateAvailable(SATURDAY, List.of());
        assertThat(slots).hasSize(8);
    }

    @Test
    @DisplayName("Saturday includes 09:00 and 12:30 but not 08:30 or 13:00")
    void saturdayBoundaries() {
        List<String> slots = generator.generateAvailable(SATURDAY, List.of());
        assertThat(slots).contains("09:00", "12:30");
        assertThat(slots).doesNotContain("08:30", "13:00");
    }

    @Test
    @DisplayName("Sunday returns empty list (branch closed)")
    void sundayClosed() {
        assertThat(generator.generateAvailable(SUNDAY, List.of())).isEmpty();
    }

    @Test
    @DisplayName("taken slots are excluded from available list")
    void excludesTakenSlots() {
        List<LocalTime> taken = List.of(
                LocalTime.of(9, 0),
                LocalTime.of(10, 30)
        );
        List<String> slots = generator.generateAvailable(WEEKDAY, taken);

        assertThat(slots).doesNotContain("09:00", "10:30");
        assertThat(slots).contains("08:00", "09:30");
        assertThat(slots).hasSize(16); // 18 - 2 taken
    }

    @Test
    @DisplayName("all taken slots are full-day leaves no available")
    void allTakenLeavesNone() {
        // Fill all 18 weekday slots
        List<LocalTime> allTaken = generator.generateAvailable(WEEKDAY, List.of())
                .stream()
                .map(s -> LocalTime.parse(s))
                .toList();

        List<String> remaining = generator.generateAvailable(WEEKDAY, allTaken);
        assertThat(remaining).isEmpty();
    }
}
