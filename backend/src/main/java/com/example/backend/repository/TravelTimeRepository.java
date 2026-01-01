package com.example.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.Event;
import com.example.backend.model.TravelTime;

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
     * Trouve les temps de trajet arrivant à un événement spécifique.
     *
     * @param toEvent l'événement d'arrivée
     * @return liste des temps de trajet
     */
    List<TravelTime> findByToEvent(Event toEvent);

    /**
     * Trouve les temps de trajet partant d'un événement spécifique.
     *
     * @param fromEvent l'événement de départ
     * @return liste des temps de trajet
     */
    Optional<TravelTime> findByFromEventAndToEvent(Event fromEvent, Event toEvent);
}