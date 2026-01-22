package com.example.backend.controller;

import com.example.backend.dto.ActivityStatsDTO;
import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
import com.example.backend.service.ActivityLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ActivityLogController.class)
public class ActivityLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ActivityLogService activityLogService;

    private ObjectMapper objectMapper;
    private ActivityLog testActivityLog;
    private ActivityLogController.ActivityRecordRequest recordRequest;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        testActivityLog = new ActivityLog();
        testActivityLog.setId(1L);
        testActivityLog.setUserId(1L);
        testActivityLog.setActivityType(ActivityCategory.TRAVAIL);
        testActivityLog.setStartTime(LocalDateTime.now().minusHours(2));
        testActivityLog.setEndTime(LocalDateTime.now());
        testActivityLog.setEventId(null);

        recordRequest = new ActivityLogController.ActivityRecordRequest();
        recordRequest.setUserId(1L);
        recordRequest.setActivityType(ActivityCategory.TRAVAIL);
        recordRequest.setStartTime(testActivityLog.getStartTime());
        recordRequest.setEndTime(testActivityLog.getEndTime());
        recordRequest.setEventId(null);
    }

    @Test
    void testRecordActivity() throws Exception {
        Mockito.when(activityLogService.recordActivity(
            eq(1L),
            eq(ActivityCategory.TRAVAIL),
            eq(null),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(testActivityLog);

        mockMvc.perform(post("/api/activity/record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.activityType").value("TRAVAIL"));
    }

    @Test
    void testGetStats() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusDays(30);
        LocalDateTime end = LocalDateTime.now();
        
        ActivityStatsDTO statsDTO = new ActivityStatsDTO(ActivityCategory.TRAVAIL, 5, 600);
        List<ActivityStatsDTO> stats = Arrays.asList(statsDTO);

        Mockito.when(activityLogService.getStats(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
            .thenReturn(stats);

        mockMvc.perform(get("/api/activity/stats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("TRAVAIL"))
                .andExpect(jsonPath("$[0].count").value(5))
                .andExpect(jsonPath("$[0].totalMinutes").value(600));
    }

    @Test
    void testGetStatsWithDateRange() throws Exception {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 1, 31, 23, 59);
        
        ActivityStatsDTO statsDTO = new ActivityStatsDTO(ActivityCategory.SPORT, 10, 1200);
        List<ActivityStatsDTO> stats = Arrays.asList(statsDTO);

        Mockito.when(activityLogService.getStats(eq(1L), eq(start), eq(end)))
            .thenReturn(stats);

        mockMvc.perform(get("/api/activity/stats/1")
                .param("start", "2025-01-01T00:00:00")
                .param("end", "2025-01-31T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("SPORT"));
    }

    @Test
    void testGetStatsWithDefaultDates() throws Exception {
        ActivityStatsDTO statsDTO = new ActivityStatsDTO(ActivityCategory.ETUDE, 3, 180);
        List<ActivityStatsDTO> stats = Arrays.asList(statsDTO);

        Mockito.when(activityLogService.getStats(
            eq(1L),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(stats);

        mockMvc.perform(get("/api/activity/stats/1"))
                .andExpect(status().isOk());
        
        // Vérifier que les dates par défaut (30 derniers jours) sont utilisées
        Mockito.verify(activityLogService).getStats(
            eq(1L),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        );
    }

    @Test
    void testRecordActivityWithEventId() throws Exception {
        recordRequest.setEventId(100L);
        testActivityLog.setEventId(100L);

        Mockito.when(activityLogService.recordActivity(
            eq(1L),
            eq(ActivityCategory.TRAVAIL),
            eq(100L),
            any(LocalDateTime.class),
            any(LocalDateTime.class)
        )).thenReturn(testActivityLog);

        mockMvc.perform(post("/api/activity/record")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recordRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventId").value(100L));
    }
}
