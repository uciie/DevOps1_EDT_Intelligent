package com.example.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
    
    /**
     * Trouve une location par son adresse exacte.
     *
     * @param address l'adresse à rechercher
     * @return la location si trouvée
     */
    Optional<Location> findByAddress(String address);

    /**
     * Trouve une location par son nom.
     *
     * @param name le nom de la location
     * @return la location si trouvée (retourne le premier résultat si plusieurs)
     */
    Optional<Location> findFirstByName(String name);

    /**
     * Legacy-compatible method used by tests/code: delegates to `findFirstByName`
     * to ensure we return a single Optional even when multiple rows match.
     */
    default Optional<Location> findByName(String name) {
        return findFirstByName(name);
    }
}