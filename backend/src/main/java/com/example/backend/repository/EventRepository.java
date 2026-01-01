package com.example.backend.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByUser_IdOrderByStartTime(Long userId);


    /** Trouve tous les événements pour un utilisateur.
     * @param userId l'ID de l'utilisateur
     * @return liste des événements
     */
    List<Event> findByUser_Id(Long userId);

    /** Trouve les événements entre deux dates pour un utilisateur.
     * @param userId l'ID de l'utilisateur
     * @param start date de début
     * @param end date de fin
     * @return liste des événements
     */
    List<Event> findByUser_IdAndStartTimeBetween(Long userId, LocalDateTime start, LocalDateTime end);


}
