package com.example.backend.service.strategy;

import com.example.backend.model.Task;

public interface TaskSelectionStrategy {
    Task selectTask(Long userId, long availableMinutes);
}
