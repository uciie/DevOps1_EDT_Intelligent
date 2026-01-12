package com.example.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class UserFocusPreference {
    @Id
    private Long userId; 

    private int maxEventsPerDay = 5;      // Point (a) : Limite de surcharge
    private int minFocusDuration = 60;     // Point (b) : Durée min du deep work
    private boolean focusModeEnabled = true;
    
    public enum FocusTimePreference {
        MATIN,       // 09h - 12h
        APRES_MIDI,  // 14h - 17h
        SOIR         // 18h - 21h
    }

    private FocusTimePreference preferredFocusTime = FocusTimePreference.MATIN;

    public UserFocusPreference() {}

    public UserFocusPreference(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getMaxEventsPerDay() {
        return maxEventsPerDay;
    }

    public int getMinFocusDuration() {
        return minFocusDuration;
    }

    public FocusTimePreference getPreferredFocusTime() {
        return preferredFocusTime;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setMaxEventsPerDay(int maxEventsPerDay) {
        this.maxEventsPerDay = maxEventsPerDay;
    }

    public void setMinFocusDuration(int minFocusDuration) {
        this.minFocusDuration = minFocusDuration;
    }

    public void setPreferredFocusTime(FocusTimePreference preferredFocusTime) {
        this.preferredFocusTime = preferredFocusTime;
    }
    // À ajouter dans UserFocusPreference.java
    public boolean isFocusModeEnabled() {
        return focusModeEnabled;
    }

    public void setFocusModeEnabled(boolean focusModeEnabled) {
        this.focusModeEnabled = focusModeEnabled;
    }
}

