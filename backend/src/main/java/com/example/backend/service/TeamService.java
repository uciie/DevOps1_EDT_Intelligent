package com.example.backend.service;

import com.example.backend.model.Team;
import com.example.backend.model.User;
import java.util.List;
import java.util.Set;

public interface TeamService {
    // Créer une équipe
    Team createTeam(String name, String description, Long ownerId);

    // Ajouter un membre via son userId (RM-01)
    Team addMemberByUserId(Long teamId, Long userId);

    // Récupérer les équipes d'un utilisateur
    List<Team> getTeamsByUserId(Long userId);

    // Récupérer les membres d'une équipe
    Set<User> getTeamMembers(Long teamId);
}