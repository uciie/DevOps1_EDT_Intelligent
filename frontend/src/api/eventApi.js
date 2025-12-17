import api from "./api";

/**
 * Crée un nouvel événement
 * @param {Object} eventData - Les données de l'événement
 * @param {boolean} useGoogleMaps - (Optionnel) Préférence pour le calcul de trajet
 */
export async function createEvent(eventData, useGoogleMaps) {
  try {
    // Si la préférence est fournie, on l'ajoute au payload
    const payload = { ...eventData };
    if (useGoogleMaps !== undefined) {
      payload.useGoogleMaps = useGoogleMaps;
    }
    
    const response = await api.post("/events", payload);
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
 * @param {boolean} useGoogleMaps - (Optionnel) Préférence pour le calcul de trajet
 */
export async function updateEvent(eventId, eventData, useGoogleMaps) {
  try {
    // Si la préférence est fournie, on l'ajoute au payload
    const payload = { ...eventData };
    if (useGoogleMaps !== undefined) {
      payload.useGoogleMaps = useGoogleMaps;
    }

    const response = await api.put(`/events/${eventId}`, payload);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la mise à jour de l'événement:", error);
    throw error;
  }
}

/**
 * Demande au backend de recalculer tous les temps de trajet
 * @param {number} userId
 * @param {boolean} useGoogleMaps
 */
export async function recalculateTravelTimes(userId, useGoogleMaps) {
  try {
    // Appel POST avec Query Params (plus simple ici pour le controller existant)
    const response = await api.post(`/events/recalculate?userId=${userId}&useGoogleMaps=${useGoogleMaps}`);
    return response.data;
  } catch (error) {
    console.error("Erreur lors du recalcul des trajets:", error);
    throw error;
  }
}