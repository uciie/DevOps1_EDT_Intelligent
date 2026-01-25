package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Event.EventStatus;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatbotServiceTest {

    EventRepository eventRepository;
    TaskRepository taskRepository;
    ChatbotService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(EventRepository.class);
        taskRepository = mock(TaskRepository.class);
        service = new ChatbotService(eventRepository, taskRepository);
    }

    @Test
    void cancelAfternoon_noEvents_returnsMessage() {
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any())).thenReturn(List.of());

        String res = service.cancelAfternoon(1L, "2026-01-25");

        assertTrue(res.contains("Aucune activité"));
    }

    @Test
    void cancelAfternoon_withEvents_marksPendingDeletionAndSaves() {
        Event e = mock(Event.class);
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any())).thenReturn(List.of(e));

        String res = service.cancelAfternoon(1L, "2026-01-25");

        verify(e).setStatus(EventStatus.PENDING_DELETION);
        verify(eventRepository).save(e);
        assertTrue(res.contains("activités"));
    }

    @Test
    void confirmPendingChanges_deletesAndConfirms() {
        Event e1 = mock(Event.class);
        Event e2 = mock(Event.class);
        when(e1.getStatus()).thenReturn(EventStatus.PENDING_DELETION);
        when(e2.getStatus()).thenReturn(EventStatus.PLANNED);
        when(eventRepository.findByUser_IdAndStatusNot(anyLong(), any())).thenReturn(List.of(e1, e2));

        String res = service.confirmPendingChanges(1L);

        verify(eventRepository).delete(e1);
        verify(e2).setStatus(EventStatus.CONFIRMED);
        verify(eventRepository).save(e2);
        assertTrue(res.contains("modifications"));
    }

    @Test
    void addTask_savesTaskAndReturnsMessage() {
        String res = service.addTask(1L, "Tâche test", 30);

        verify(taskRepository).save(any());
        assertTrue(res.contains("préparé"));
    }

    @Test
    void moveActivity_movesAndSavesEvent() {
        Event e = mock(Event.class);
        when(eventRepository.findBySummaryContainingAndUser_Id(anyString(), anyLong())).thenReturn(List.of(e));
        LocalDateTime start = LocalDateTime.of(2026,1,25,9,0);
        LocalDateTime end = LocalDateTime.of(2026,1,25,10,0);
        when(e.getStartTime()).thenReturn(start);
        when(e.getEndTime()).thenReturn(end);

        String res = service.moveActivity(1L, "nom", "2026-01-26", "14:30");

        ArgumentCaptor<LocalDateTime> startCap = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(e).setStartTime(startCap.capture());
        verify(e).setEndTime(any());
        verify(e).setStatus(EventStatus.PENDING_DELETION);
        verify(eventRepository).save(e);
        assertTrue(res.contains("Propose" ) || res.contains("Confirmez" ) || res.contains("déplacer"));
    }
}
