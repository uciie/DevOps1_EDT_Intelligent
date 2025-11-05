package com.example.backend.scheduler.service.impl;

import org.springframework.stereotype.Service;

import com.example.backend.model.Event;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import com.example.backend.scheduler.service.ScheduleOptimizerService;
import com.example.backend.scheduler.service.TaskSelectionStrategy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

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

        // Utilisation de la stratégie (principle O de SOLID)
        Task bestTask = taskSelectionStrategy.selectTask(cancelledEvent.getUserId(), freeMinutes);

        if (bestTask != null) {
            Event newEvent = new Event(bestTask.getName(), start,
                    start.plusMinutes(bestTask.getDurationMinutes()), cancelledEvent.getUserId());
            eventRepository.save(newEvent);
        }

        cancelledEvent.setStatus("CANCELLED");
        eventRepository.save(cancelledEvent);
    }
}
