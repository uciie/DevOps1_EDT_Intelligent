import api from "./api";

/**
 * Crée un nouvel événement
 * @param {Object} eventData - Les données de l'événement
 */
export async function createEvent(eventData) {
  try {
    const response = await api.post("/events", eventData);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la création de l'événement:", error);
    throw error;
  }
}

/**
 * Récupère tous les événements d'un utilisateur
 * @param {number} userId 
 */
export async function getUserEvents(userId) {
  try {
    const response = await api.get(`/events/user/${userId}`);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la récupération des événements:", error);
    throw error;
  }
}

/**
 * Supprime un événement
 * @param {number} eventId 
 */
export async function deleteEvent(eventId) {
  try {
    await api.delete(`/events/${eventId}`);
  } catch (error) {
    console.error("Erreur lors de la suppression de l'événement:", error);
    throw error;
  }
}

/**
 * Met à jour un événement
 * @param {number} eventId
 * @param {Object} eventData
 */
export async function updateEvent(eventId, eventData) {
  try {
    const response = await api.put(`/events/${eventId}`, eventData);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la mise à jour de l'événement:", error);
    throw error;
  }
}