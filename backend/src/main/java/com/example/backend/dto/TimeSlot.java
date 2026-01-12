package com.example.backend.dto;

import java.time.LocalDateTime;
import java.time.Duration;

public record TimeSlot(
    LocalDateTime start,
    LocalDateTime end
) {
    public long getDurationMinutes() {
        return Duration.between(start, end).toMinutes();
    }
}