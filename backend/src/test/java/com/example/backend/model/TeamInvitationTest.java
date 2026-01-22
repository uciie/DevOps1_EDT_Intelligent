package com.example.backend.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TeamInvitationTest {

    @Test
    void testDefaultConstructor() {
        // Act
        TeamInvitation invitation = new TeamInvitation();

        // Assert
        assertNull(invitation.getId());
        assertNull(invitation.getTeam());
        assertNull(invitation.getInvitedUser());
        assertNull(invitation.getInviter());
        assertNull(invitation.getStatus());
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        TeamInvitation invitation = new TeamInvitation();
        Long id = 1L;
        Team team = new Team();
        team.setId(10L);
        User invitedUser = new User("invited", "password");
        invitedUser.setId(20L);
        User inviter = new User("inviter", "password");
        inviter.setId(30L);
        TeamInvitation.Status status = TeamInvitation.Status.PENDING;

        // Act
        invitation.setId(id);
        invitation.setTeam(team);
        invitation.setInvitedUser(invitedUser);
        invitation.setInviter(inviter);
        invitation.setStatus(status);

        // Assert
        assertEquals(id, invitation.getId());
        assertEquals(team, invitation.getTeam());
        assertEquals(invitedUser, invitation.getInvitedUser());
        assertEquals(inviter, invitation.getInviter());
        assertEquals(status, invitation.getStatus());
    }

    @Test
    void testAllStatusValues() {
        // Arrange
        TeamInvitation invitation = new TeamInvitation();

        // Act & Assert
        invitation.setStatus(TeamInvitation.Status.PENDING);
        assertEquals(TeamInvitation.Status.PENDING, invitation.getStatus());

        invitation.setStatus(TeamInvitation.Status.ACCEPTED);
        assertEquals(TeamInvitation.Status.ACCEPTED, invitation.getStatus());

        invitation.setStatus(TeamInvitation.Status.REJECTED);
        assertEquals(TeamInvitation.Status.REJECTED, invitation.getStatus());
    }

    @Test
    void testStatusEnumValues() {
        // Vérifier que tous les statuts sont bien définis
        TeamInvitation.Status[] statuses = TeamInvitation.Status.values();
        
        assertEquals(3, statuses.length);
        assertTrue(java.util.Arrays.asList(statuses).contains(TeamInvitation.Status.PENDING));
        assertTrue(java.util.Arrays.asList(statuses).contains(TeamInvitation.Status.ACCEPTED));
        assertTrue(java.util.Arrays.asList(statuses).contains(TeamInvitation.Status.REJECTED));
    }

    @Test
    void testTeamInvitationRelations() {
        // Arrange
        TeamInvitation invitation = new TeamInvitation();
        Team team = new Team();
        team.setName("Test Team");
        User invited = new User("invited", "pass");
        User inviter = new User("inviter", "pass");

        // Act
        invitation.setTeam(team);
        invitation.setInvitedUser(invited);
        invitation.setInviter(inviter);
        invitation.setStatus(TeamInvitation.Status.PENDING);

        // Assert
        assertNotNull(invitation.getTeam());
        assertNotNull(invitation.getInvitedUser());
        assertNotNull(invitation.getInviter());
        assertEquals("Test Team", invitation.getTeam().getName());
        assertEquals("invited", invitation.getInvitedUser().getUsername());
        assertEquals("inviter", invitation.getInviter().getUsername());
    }

    @Test
    void testSetId() {
        // Arrange
        TeamInvitation invitation = new TeamInvitation();

        // Act
        invitation.setId(42L);

        // Assert
        assertEquals(42L, invitation.getId());
    }

    @Test
    void testStatusTransition() {
        // Arrange
        TeamInvitation invitation = new TeamInvitation();
        invitation.setStatus(TeamInvitation.Status.PENDING);

        // Act - Transition vers ACCEPTED
        invitation.setStatus(TeamInvitation.Status.ACCEPTED);

        // Assert
        assertEquals(TeamInvitation.Status.ACCEPTED, invitation.getStatus());
        assertNotEquals(TeamInvitation.Status.PENDING, invitation.getStatus());
    }
}
