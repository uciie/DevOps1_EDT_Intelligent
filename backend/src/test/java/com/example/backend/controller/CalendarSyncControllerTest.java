package com.example.backend.controller;

import com.example.backend.dto.SyncConflictDTO;
import com.example.backend.exception.GoogleApiException;
import com.example.backend.exception.SyncConflictException;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.CalendarSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

class CalendarSyncControllerTest {

    private UserRepository userRepository;
    private CalendarSyncService calendarSyncService;
    private CalendarSyncController controller;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        calendarSyncService = Mockito.mock(CalendarSyncService.class);
        controller = new CalendarSyncController(userRepository, calendarSyncService);
    }

    @Test
    void pullNow_noGoogleToken_returnsUnauthorized() {
        User user = new User();
        user.setId(42L);
        user.setGoogleAccessToken(null);

        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> resp = controller.pullNow(42L);

        assertThat(resp.getStatusCodeValue()).isEqualTo(401);
        assertThat(resp.getBody()).containsEntry("errorCode", "NO_GOOGLE_TOKEN");
        assertThat(resp.getBody()).containsEntry("success", false);
    }

    @Test
    void pullNow_success_callsSyncAndReturnsOk() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setGoogleAccessToken("token");

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> resp = controller.pullNow(7L);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        assertThat(resp.getBody()).containsEntry("success", true);
        assertThat(resp.getBody()).containsEntry("userId", 7L);
    }

    @Test
    void pullNow_conflict_returnsConflictStatus() throws Exception {
        User user = new User();
        user.setId(9L);
        user.setGoogleAccessToken("token");

        SyncConflictDTO dto = new SyncConflictDTO();
        dto.setConflicts(java.util.List.of());

        doThrow(new SyncConflictException("conflict", dto)).when(calendarSyncService).syncUser(9L);
        when(userRepository.findById(9L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> resp = controller.pullNow(9L);

        assertThat(resp.getStatusCodeValue()).isEqualTo(409);
        assertThat(resp.getBody()).containsEntry("errorCode", "SCHEDULE_CONFLICTS");
        assertThat(resp.getBody()).containsKey("conflicts");
    }

    @Test
    void pullNow_googleApiException_mapsToStatus() throws Exception {
        User user = new User();
        user.setId(11L);
        user.setGoogleAccessToken("token");

        GoogleApiException ex = new GoogleApiException("service down", "SERVICE_UNAVAILABLE", true);
        doThrow(ex).when(calendarSyncService).syncUser(11L);
        when(userRepository.findById(11L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> resp = controller.pullNow(11L);

        assertThat(resp.getStatusCodeValue()).isEqualTo(503);
        assertThat(resp.getBody()).containsEntry("errorCode", "SERVICE_UNAVAILABLE");
        assertThat(resp.getBody()).containsEntry("retryable", true);
        assertThat(resp.getBody()).containsKey("userMessage");
    }

    @Test
    void syncCurrentUser_withoutUserId_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> resp = controller.syncCurrentUser(null);
        assertThat(resp.getStatusCodeValue()).isEqualTo(400);
        assertThat(resp.getBody()).containsEntry("success", false);
    }

    @Test
    void getGoogleStatus_connectedAndNotConnected() {
        User user = new User();
        user.setId(5L);
        user.setGoogleAccessToken("tok");
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        ResponseEntity<Map<String, Object>> ok = controller.getGoogleStatus(5L);
        assertThat(ok.getStatusCodeValue()).isEqualTo(200);
        assertThat(ok.getBody()).containsEntry("connected", true);

        User user2 = new User();
        user2.setId(6L);
        user2.setGoogleAccessToken("");
        when(userRepository.findById(6L)).thenReturn(Optional.of(user2));

        ResponseEntity<Map<String, Object>> not = controller.getGoogleStatus(6L);
        assertThat(not.getBody()).containsEntry("connected", false);
    }
}
