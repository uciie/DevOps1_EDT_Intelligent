import api from "./api";

export async function getActivityStats(userId, startDate, endDate) {
  try {
    // Axios gère les paramètres null/undefined proprement
    const response = await api.get(`/activity/stats/${userId}`, {
      params: {
        start: startDate, // Format ISO string attendu
        end: endDate
      }
    });
    return response.data; // Retourne une liste de DTOs maintenant
  } catch (error) {
    console.error("Erreur stats:", error);
    throw error;
  }
}