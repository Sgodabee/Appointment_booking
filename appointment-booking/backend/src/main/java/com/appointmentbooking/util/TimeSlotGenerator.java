package com.appointmentbooking.util;

import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates 30-minute appointment slots for a given day,
 * excluding already-taken times.
 *
 * Operating hours:
 *   Monday–Friday : 08:00 – 17:00  (18 slots)
 *   Saturday      : 09:00 – 13:00  (8 slots)
 *   Sunday        : closed          (0 slots)
 */
@Component
public class TimeSlotGenerator {

    public List<String> generateAvailable(LocalDate date, List<LocalTime> takenSlots) {
        DayOfWeek dow = date.getDayOfWeek();

        if (dow == DayOfWeek.SUNDAY) {
            return List.of();
        }

        int startHour = (dow == DayOfWeek.SATURDAY) ? 9  : 8;
        int endHour   = (dow == DayOfWeek.SATURDAY) ? 13 : 17;

        Set<String> taken = takenSlots.stream()
                .map(t -> String.format("%02d:%02d", t.getHour(), t.getMinute()))
                .collect(Collectors.toSet());

        List<String> available = new ArrayList<>();
        for (int h = startHour; h < endHour; h++) {
            for (int m : new int[]{0, 30}) {
                String slot = String.format("%02d:%02d", h, m);
                if (!taken.contains(slot)) {
                    available.add(slot);
                }
            }
        }
        return available;
    }
}
