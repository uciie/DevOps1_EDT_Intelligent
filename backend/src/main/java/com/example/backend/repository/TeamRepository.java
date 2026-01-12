package com.example.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.Team;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    // Utile pour inviter des membres par username (RM-01)
    
    
}