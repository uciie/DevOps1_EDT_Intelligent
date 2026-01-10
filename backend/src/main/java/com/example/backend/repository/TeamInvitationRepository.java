package com.example.backend.repository;

import com.example.backend.model.TeamInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Repository pour gérer les invitations d'équipe
public interface TeamInvitationRepository extends JpaRepository<TeamInvitation, Long> {
    List<TeamInvitation> findByInvitedUserIdAndStatus(Long userId, TeamInvitation.Status status);
}