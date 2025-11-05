package com.example.backend.scheduler.service;

import com.example.backend.model.Task;

public interface TaskSelectionStrategy {
    Task selectTask(Long userId, long availableMinutes);
}
