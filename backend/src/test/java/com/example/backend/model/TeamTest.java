package com.example.backend.model;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitaires pour l'entité Team.
 * Vérifie le comportement des constructeurs et la gestion de la relation avec les utilisateurs.
 */
class TeamTest {

    @Test
    void testTeamCreationAndGetters() {
        // Test du constructeur avec paramètres
        Team team = new Team("Alpha", "Description Alpha", 1L);

        assertThat(team.getName()).isEqualTo("Alpha");
        assertThat(team.getDescription()).isEqualTo("Description Alpha");
        assertThat(team.getOwnerId()).isEqualTo(1L);
        assertThat(team.getMembers()).isNotNull().isEmpty();
    }

    @Test
    void testAddAndRemoveMember() {
        Team team = new Team("Alpha", "Desc", 1L);
        User user = new User("user1", "password1");
        user.setId(2L);
        user.setTeams(new HashSet<>());

        // Test de l'ajout d'un membre avec mise à jour bidirectionnelle
        team.addMember(user);
        assertThat(team.getMembers()).contains(user);
        assertThat(user.getTeams()).contains(team);

        // Test de la suppression d'un membre
        team.removeMember(user);
        assertThat(team.getMembers()).doesNotContain(user);
        assertThat(user.getTeams()).doesNotContain(team);
    }
}