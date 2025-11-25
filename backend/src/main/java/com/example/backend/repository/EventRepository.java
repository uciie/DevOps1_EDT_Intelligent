package com.example.backend.repository;

import com.example.backend.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; 

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByUser_IdOrderByStartTime(Long userId);

}
