package com.example.backend.repository;

import com.example.backend.model.Team;
import com.example.backend.model.TeamInvitation;
import com.example.backend.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class TeamInvitationRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TeamInvitationRepository teamInvitationRepository;

    @Test
    void testSaveTeamInvitation() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team = new Team();
        team.setName("Test Team");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team);

        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setInvitedUser(invited);
        invitation.setInviter(inviter);
        invitation.setStatus(TeamInvitation.Status.PENDING);

        // Act
        TeamInvitation saved = teamInvitationRepository.save(invitation);

        // Assert
        assertNotNull(saved.getId());
        assertEquals(team, saved.getTeam());
        assertEquals(invited, saved.getInvitedUser());
        assertEquals(inviter, saved.getInviter());
        assertEquals(TeamInvitation.Status.PENDING, saved.getStatus());
    }

    @Test
    void testFindByTeamId() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited1 = new User("invited1", "password");
        User invited2 = new User("invited2", "password");
        Team team1 = new Team();
        team1.setName("Team 1");
        Team team2 = new Team();
        team2.setName("Team 2");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited1);
        entityManager.persistAndFlush(invited2);
        entityManager.persistAndFlush(team1);
        entityManager.persistAndFlush(team2);

        TeamInvitation inv1 = new TeamInvitation();
        inv1.setTeam(team1);
        inv1.setInvitedUser(invited1);
        inv1.setInviter(inviter);
        inv1.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(inv1);

        TeamInvitation inv2 = new TeamInvitation();
        inv2.setTeam(team1);
        inv2.setInvitedUser(invited2);
        inv2.setInviter(inviter);
        inv2.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(inv2);

        TeamInvitation inv3 = new TeamInvitation();
        inv3.setTeam(team2);
        inv3.setInvitedUser(invited1);
        inv3.setInviter(inviter);
        inv3.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(inv3);

        // Act
        List<TeamInvitation> results = teamInvitationRepository.findByTeamId(team1.getId());

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(inv -> inv.getTeam().getId().equals(team1.getId())));
    }

    @Test
    void testFindByInvitedUserIdAndStatus() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team1 = new Team();
        team1.setName("Team 1");
        Team team2 = new Team();
        team2.setName("Team 2");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team1);
        entityManager.persistAndFlush(team2);

        TeamInvitation inv1 = new TeamInvitation();
        inv1.setTeam(team1);
        inv1.setInvitedUser(invited);
        inv1.setInviter(inviter);
        inv1.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(inv1);

        TeamInvitation inv2 = new TeamInvitation();
        inv2.setTeam(team2);
        inv2.setInvitedUser(invited);
        inv2.setInviter(inviter);
        inv2.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(inv2);

        TeamInvitation inv3 = new TeamInvitation();
        inv3.setTeam(team1);
        inv3.setInvitedUser(invited);
        inv3.setInviter(inviter);
        inv3.setStatus(TeamInvitation.Status.ACCEPTED);
        entityManager.persistAndFlush(inv3);

        // Act
        List<TeamInvitation> results = teamInvitationRepository.findByInvitedUserIdAndStatus(
            invited.getId(), TeamInvitation.Status.PENDING);

        // Assert
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(inv -> 
            inv.getInvitedUser().getId().equals(invited.getId()) &&
            inv.getStatus() == TeamInvitation.Status.PENDING));
    }

    @Test
    void testFindByTeamIdAndInvitedUserIdAndStatus() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team = new Team();
        team.setName("Test Team");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team);

        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setInvitedUser(invited);
        invitation.setInviter(inviter);
        invitation.setStatus(TeamInvitation.Status.PENDING);
        entityManager.persistAndFlush(invitation);

        // Act
        Optional<TeamInvitation> result = teamInvitationRepository.findByTeamIdAndInvitedUserIdAndStatus(
            team.getId(), invited.getId(), TeamInvitation.Status.PENDING);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(invitation.getId(), result.get().getId());
        assertEquals(team.getId(), result.get().getTeam().getId());
        assertEquals(invited.getId(), result.get().getInvitedUser().getId());
        assertEquals(TeamInvitation.Status.PENDING, result.get().getStatus());
    }

    @Test
    void testFindByTeamIdAndInvitedUserIdAndStatus_NotFound() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team = new Team();
        team.setName("Test Team");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team);

        // Act - Chercher une invitation qui n'existe pas
        Optional<TeamInvitation> result = teamInvitationRepository.findByTeamIdAndInvitedUserIdAndStatus(
            team.getId(), invited.getId(), TeamInvitation.Status.PENDING);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testFindByTeamIdAndInvitedUserIdAndStatus_WrongStatus() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team = new Team();
        team.setName("Test Team");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team);

        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setInvitedUser(invited);
        invitation.setInviter(inviter);
        invitation.setStatus(TeamInvitation.Status.ACCEPTED);
        entityManager.persistAndFlush(invitation);

        // Act - Chercher avec un statut différent
        Optional<TeamInvitation> result = teamInvitationRepository.findByTeamIdAndInvitedUserIdAndStatus(
            team.getId(), invited.getId(), TeamInvitation.Status.PENDING);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void testDeleteTeamInvitation() {
        // Arrange
        User inviter = new User("inviter", "password");
        User invited = new User("invited", "password");
        Team team = new Team();
        team.setName("Test Team");
        
        entityManager.persistAndFlush(inviter);
        entityManager.persistAndFlush(invited);
        entityManager.persistAndFlush(team);

        TeamInvitation invitation = new TeamInvitation();
        invitation.setTeam(team);
        invitation.setInvitedUser(invited);
        invitation.setInviter(inviter);
        invitation.setStatus(TeamInvitation.Status.PENDING);
        TeamInvitation saved = teamInvitationRepository.save(invitation);
        Long id = saved.getId();

        // Act
        teamInvitationRepository.deleteById(id);

        // Assert
        assertFalse(teamInvitationRepository.existsById(id));
    }
}
