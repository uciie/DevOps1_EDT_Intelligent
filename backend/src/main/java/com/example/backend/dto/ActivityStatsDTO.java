package com.example.backend.dto;

import com.example.backend.model.ActivityCategory;

public class ActivityStatsDTO {
    private ActivityCategory category;
    private long count;             // Nombre de fois
    private long totalMinutes;      // Durée totale
    private long averageMinutes;    // Moyenne par session

    public ActivityStatsDTO(ActivityCategory category, long count, long totalMinutes) {
        this.category = category;
        this.count = count;
        this.totalMinutes = totalMinutes;
        this.averageMinutes = count > 0 ? totalMinutes / count : 0;
    }

    // Getters
    public ActivityCategory getCategory() { return category; }
    public long getCount() { return count; }
    public long getTotalMinutes() { return totalMinutes; }
    public long getAverageMinutes() { return averageMinutes; }
}