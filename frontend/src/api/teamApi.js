import api from './api';

// Récupérer les équipes de l'utilisateur connecté
export const getMyTeams = async (userId) => {
  try {
    const response = await api.get(`/teams/user/${userId}`); 
    return response.data;
  } catch (error) {
    console.error("Erreur récupération équipes:", error);
    return [];
  }
};

// Créer une nouvelle équipe
export const createTeam = async (userId, teamData) => {
  const response = await api.post(`/teams/user/${userId}`, teamData);
  return response.data;
};

// Inviter un utilisateur à rejoindre une équipe
// Suppose une route: POST /teams/{teamId}/invite/${invitedUserId}?inviterId={inviterId}
export const inviteUserToTeam = async (teamId, invitedUserId, inviterId) => {
  const response = await api.post(`/teams/${teamId}/invite/${invitedUserId}?inviterId=${inviterId}`);
  return response.data;
};

// Supprimer membre d'une équipe
export const removeMemberFromTeam = async (teamId, memberId, requesterId) => {
  // L'URL correspond au Controller : DELETE /api/teams/{id}/members/{userId}
  const response = await api.delete(`/teams/${teamId}/members/${memberId}`, {
    params: { requesterId } // On passe l'ID de l'admin en paramètre
  });
  return response.data;
};

// AJOUT : Supprimer une équipe
export const deleteTeam = async (teamId, requesterId) => {
  const response = await api.delete(`/teams/${teamId}`, {
    params: { requesterId }
  });
  return response.data;
};

// Récupérer les invitations en attente pour un utilisateur
export const getPendingInvitations = (userId) => {
    return api.get(`/teams/invitations/pending/${userId}`);
};

// Répondre à une invitation (accepter ou refuser)
export const respondToInvitation = (invitationId, accept) => {
    return api.post(`/teams/invitations/${invitationId}/respond?accept=${accept}`);
};