package com.example.backend.service;

import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.*;

class CalendarSyncSchedulerTest {

    private UserRepository userRepository;
    private CalendarSyncService calendarSyncService;
    private CalendarSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        calendarSyncService = Mockito.mock(CalendarSyncService.class);
        scheduler = new CalendarSyncScheduler(userRepository, calendarSyncService);
    }

    @Test
    void syncAllUsers_onlyCallsEligibleAndContinuesOnError() throws Exception {
        User u1 = new User(); u1.setId(1L); u1.setGoogleAccessToken("tok1");
        User u2 = new User(); u2.setId(2L); u2.setGoogleAccessToken(null);
        User u3 = new User(); u3.setId(3L); u3.setGoogleAccessToken("tok3");

        when(userRepository.findAll()).thenReturn(List.of(u1, u2, u3));

        // u1 succeeds
        doNothing().when(calendarSyncService).syncUser(1L);
        // u3 fails but should not stop u1
        doThrow(new RuntimeException("boom")).when(calendarSyncService).syncUser(3L);

        // Act - should not throw
        scheduler.syncAllUsers();

        // Verify only eligible users called
        verify(calendarSyncService, times(1)).syncUser(1L);
        verify(calendarSyncService, times(1)).syncUser(3L);
        verify(calendarSyncService, never()).syncUser(2L);
    }

    @Test
    void syncAllUsers_noEligibleUsers_noCalls() throws Exception {
        User u1 = new User(); u1.setId(1L); u1.setGoogleAccessToken(null);
        when(userRepository.findAll()).thenReturn(List.of(u1));

        scheduler.syncAllUsers();

        verify(calendarSyncService, never()).syncUser(anyLong());
    }
}
