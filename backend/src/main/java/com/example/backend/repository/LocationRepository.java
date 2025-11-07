package com.example.backend.repository;

import com.example.backend.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

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
     * @return la location si trouvée
     */
    Optional<Location> findByName(String name);
}