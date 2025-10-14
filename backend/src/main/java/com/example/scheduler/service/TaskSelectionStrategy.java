package com.example.scheduler.service;

import com.example.scheduler.model.Task;

public interface TaskSelectionStrategy {
    Task selectTask(Long userId, long availableMinutes);
}
