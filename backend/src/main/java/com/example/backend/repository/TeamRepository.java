package com.example.backend.repository;

import com.example.backend.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Utile pour inviter des membres par username (RM-01)
    
    
}