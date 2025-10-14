package com.example.scheduler.service.impl;

import com.example.scheduler.model.Task;
import com.example.scheduler.repository.TaskRepository;
import com.example.scheduler.service.TaskSelectionStrategy;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DefaultTaskSelectionStrategy implements TaskSelectionStrategy {

    private final TaskRepository taskRepository;

    public DefaultTaskSelectionStrategy(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Task selectTask(Long userId, long availableMinutes) {
        List<Task> tasks = taskRepository.findByUserId(userId);

        return tasks.stream()
                .filter(t -> !t.isDone() && t.getDurationMinutes() <= availableMinutes)
                .max(Comparator.comparing(Task::getPriority))
                .orElse(null);
    }
}
