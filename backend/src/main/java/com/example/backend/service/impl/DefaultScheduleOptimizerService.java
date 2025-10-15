package com.example.backend.service.impl;

import com.example.backend.service.ScheduleOptimizerService;
import com.example.backend.service.strategy.TaskSelectionStrategy;
import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class DefaultScheduleOptimizerService implements ScheduleOptimizerService {

    private final EventRepository eventRepository;
    private final TaskRepository taskRepository;
    private final TaskSelectionStrategy taskSelectionStrategy;

    public DefaultScheduleOptimizerService(EventRepository eventRepository,
                                           TaskRepository taskRepository,
                                           TaskSelectionStrategy taskSelectionStrategy) {
        this.eventRepository = eventRepository;
        this.taskRepository = taskRepository;
        this.taskSelectionStrategy = taskSelectionStrategy;
    }

    @Override
    public void reshuffle(Long eventId) {
        Event cancelledEvent = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        LocalDateTime start = cancelledEvent.getStartTime();
        LocalDateTime end = cancelledEvent.getEndTime();
        long freeMinutes = Duration.between(start, end).toMinutes();

        Task bestTask = taskSelectionStrategy.selectTask(cancelledEvent.getUserId(), freeMinutes);

        if (bestTask != null) {
            Event newEvent = new Event(
                    bestTask.getTitle(),
                    start,
                    start.plusMinutes(bestTask.getEstimatedDuration()),
                    cancelledEvent.getUser()
            );
            eventRepository.save(newEvent);
        }

        cancelledEvent.setStatus("CANCELLED");
        eventRepository.save(cancelledEvent);
    }
}