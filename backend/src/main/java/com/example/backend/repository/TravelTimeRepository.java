package com.example.backend.repository;

import com.example.backend.model.TravelTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TravelTimeRepository extends JpaRepository<TravelTime, Long> {
    
    /**
     * Trouve tous les temps de trajet pour un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @return liste des temps de trajet
     */
    List<TravelTime> findByUser_Id(Long userId);
    
    /**
     * Trouve les temps de trajet entre deux dates pour un utilisateur.
     *
     * @param userId l'ID de l'utilisateur
     * @param start date de début
     * @param end date de fin
     * @return liste des temps de trajet dans la période
     */
    List<TravelTime> findByUser_IdAndStartTimeBetween(
        Long userId, LocalDateTime start, LocalDateTime end
    );
    
    /**
     * Trouve le temps de trajet vers un événement spécifique.
     *
     * @param eventId l'ID de l'événement de destination
     * @return le temps de trajet ou null
     */
    TravelTime findByToEvent_Id(Long eventId);
}