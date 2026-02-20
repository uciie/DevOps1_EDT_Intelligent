package com.example.backend.service;

import com.example.backend.model.Event;
import com.example.backend.model.User;
import com.example.backend.repository.EventRepository;
import com.example.backend.repository.TeamRepository;
import com.example.backend.repository.TravelTimeRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.impl.EventServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventServiceImplTest {

    private EventRepository eventRepository;
    private UserRepository userRepository;
    private TravelTimeService travelTimeService;
    private TravelTimeRepository travelTimeRepository;
    private TeamRepository teamRepository;
    private TravelTimeCalculator primaryCalculator;
    private TravelTimeCalculator simpleCalculator;
    private SyncDelegateService syncDelegateService;

    private EventServiceImpl service;

    @BeforeEach
    void setUp() {
        eventRepository = Mockito.mock(EventRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        travelTimeService = Mockito.mock(TravelTimeService.class);
        travelTimeRepository = Mockito.mock(TravelTimeRepository.class);
        teamRepository = Mockito.mock(TeamRepository.class);
        primaryCalculator = Mockito.mock(TravelTimeCalculator.class);
        simpleCalculator = Mockito.mock(TravelTimeCalculator.class);
        syncDelegateService = Mockito.mock(SyncDelegateService.class);

        service = new EventServiceImpl(eventRepository, userRepository, travelTimeService,
                travelTimeRepository, primaryCalculator, simpleCalculator, teamRepository, syncDelegateService);
    }

    @Test
    void getEventsByUserId_delegatesToRepository() {
        when(eventRepository.findByUser_IdOrderByStartTime(10L)).thenReturn(List.of());

        List<Event> events = service.getEventsByUserId(10L);

        assertThat(events).isEmpty();
        verify(eventRepository).findByUser_IdOrderByStartTime(10L);
    }

    @Test
    void getEventById_notFound_throws() {
        when(eventRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.getEventById(99L));
    }

    @Test
    void deleteEvent_notExists_throws() {
        when(eventRepository.existsById(555L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> service.deleteEvent(555L));
    }
}
