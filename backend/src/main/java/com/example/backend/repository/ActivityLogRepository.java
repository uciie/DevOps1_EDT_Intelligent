package com.example.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.ActivityCategory;
import com.example.backend.model.ActivityLog;
@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findByUserId(Long userId);
    //List<ActivityLog> findByUserIdAndActivityType(Long userId, ActivityCategory activityType);
    List<ActivityLog> findByUserIdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);
}
