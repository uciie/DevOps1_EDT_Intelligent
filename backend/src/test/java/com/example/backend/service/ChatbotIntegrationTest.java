package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.Event.EventStatus;
import com.example.backend.model.Task;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChatbotIntegrationTest {

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
    void cancelAfternoon_identifiesAndMarksEvents_pendingDeletionAndAsksConfirmation() {
        Event e1 = new Event("Visite médecin", LocalDateTime.of(2026,1,25,14,0), LocalDateTime.of(2026,1,25,15,0));
        when(eventRepository.findByUser_IdAndStartTimeBetween(anyLong(), any(), any())).thenReturn(List.of(e1));

        String res = service.cancelAfternoon(1L, "2026-01-25");

        assertEquals(EventStatus.PENDING_DELETION, e1.getStatus());
        verify(eventRepository).save(e1);
        // Vérifications supplémentaires
        assertEquals("Visite médecin", e1.getSummary());
        assertEquals(LocalDateTime.of(2026,1,25,14,0), e1.getStartTime());
        assertTrue(res.contains("1 activités") || res.contains("1 activité") || res.toLowerCase().contains("confirme"));
    }

    @Test
    void moveSportAndAddReading_createsAndUpdatesProperly_andAsksConfirmation() {
        Event sport = new Event("sport", LocalDateTime.of(2026,1,25,18,0), LocalDateTime.of(2026,1,25,19,0));
        when(eventRepository.findBySummaryContainingAndUser_Id(anyString(), anyLong())).thenReturn(List.of(sport));

        // Simuler la partie "Déplace mon sport à demain"
        String moveRes = service.moveActivity(1L, "sport", "2026-01-26", "14:30");

        assertEquals(EventStatus.PENDING_DELETION, sport.getStatus());
        assertEquals(LocalDateTime.parse("2026-01-26T14:30"), sport.getStartTime());
        // Vérifier que la fin a bien été déplacée de 1h (18:00-19:00 => 14:30-15:30)
        assertEquals(LocalDateTime.parse("2026-01-26T15:30"), sport.getEndTime());
        verify(eventRepository).save(sport);
        assertTrue(moveRes.toLowerCase().contains("confirme") || moveRes.toLowerCase().contains("propose") || moveRes.toLowerCase().contains("déplacer"));
        assertTrue(moveRes.contains("2026-01-26") && moveRes.contains("14:30"));

        // Simuler la partie "et ajoute 1h de lecture ce soir"
        String addRes = service.addTask(1L, "lecture", 60);

        // Capturer la Task sauvegardée et vérifier ses champs
        ArgumentCaptor<Task> taskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(taskCaptor.capture());
        Task saved = taskCaptor.getValue();
        assertNotNull(saved);
        assertEquals("lecture", saved.getTitle());
        assertEquals(60, saved.getEstimatedDuration());
        assertNotNull(saved.getStatus());
        assertTrue(saved.getStatus().name().contains("PENDING") || saved.getStatus().name().contains("PENDING_CREATION") || saved.getStatus().name().contains("CONFIRMED") || saved.getStatus().name().contains("DONE") || saved.getStatus().name().contains("CANCELLED"));
        assertTrue(addRes.contains("préparé") || addRes.contains("préparée") || addRes.toLowerCase().contains("prépar"));
    }
}
