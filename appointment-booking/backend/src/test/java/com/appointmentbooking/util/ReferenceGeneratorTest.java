package com.appointmentbooking.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ReferenceGenerator")
class ReferenceGeneratorTest {

    private final ReferenceGenerator generator = new ReferenceGenerator();

    @Test
    @DisplayName("generates reference in APB-YYYYMMDD-XXXX format")
    void formatIsCorrect() {
        String ref = generator.generate();
        assertThat(ref).matches("^APB-\\d{8}-[A-F0-9]{4}$");
    }

    @Test
    @DisplayName("contains today's date")
    void containsToday() {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        assertThat(generator.generate()).contains(today);
    }

    @Test
    @DisplayName("generates unique values across 200 calls")
    void generatesUniqueValues() {
        Set<String> refs = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            refs.add(generator.generate());
        }
        // With 2-byte random (65536 possibilities), collisions in 200 calls are astronomically rare
        assertThat(refs.size()).isGreaterThan(190);
    }
}
