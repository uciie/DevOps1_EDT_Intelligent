import api from "./api";

/**
 * Déclenche manuellement une synchronisation Google Calendar (bidirectionnelle)
 * Gère les conflits, les erreurs réseau retryables et les erreurs d'authentification
 * 
 * @param {number|string} userId
 * @returns {Promise<Object>} Résultat de la synchronisation avec gestion des cas spéciaux
 */
export async function syncGoogleCalendar(userId) {
  try {
    const response = await api.post(`/calendar/sync/pull/${userId}`);
    return response.data;
  } catch (error) {
    // Gestion des erreurs HTTP
    if (error.response) {
      const errorData = error.response.data;
      const statusCode = error.response.status;
      
      // ===== GESTION SPÉCIFIQUE DES CONFLITS (HTTP 409) =====
      if (statusCode === 409 && errorData.errorCode === 'SCHEDULE_CONFLICTS') {
        // On retourne l'objet d'erreur complet pour que le composant puisse afficher les conflits
        return {
          success: false,
          hasConflicts: true,
          conflicts: errorData.conflicts || [],
          conflictCount: errorData.conflictCount || 0,
          message: errorData.message || "Des conflits de créneaux ont été détectés"
        };
      }
      
      // ===== GESTION DES ERREURS RÉSEAU RETRYABLES (HTTP 503) =====
      if (statusCode === 503) {
        return {
          success: false,
          errorCode: errorData.errorCode || 'SERVICE_UNAVAILABLE',
          retryable: true,
          message: errorData.userMessage || errorData.message || "Service Google Calendar temporairement indisponible",
          userMessage: errorData.userMessage
        };
      }
      
      // ===== GESTION DES ERREURS D'AUTHENTIFICATION (HTTP 401) =====
      if (statusCode === 401) {
        return {
          success: false,
          errorCode: errorData.errorCode || 'UNAUTHORIZED',
          retryable: false,
          message: errorData.userMessage || "Votre connexion Google a expiré. Veuillez vous reconnecter.",
          userMessage: errorData.userMessage,
          needsReauth: true // Flag pour rediriger vers la page de configuration
        };
      }
      
      // ===== GESTION DES AUTRES ERREURS HTTP =====
      if (errorData && typeof errorData === 'object') {
        return {
          success: false,
          errorCode: errorData.errorCode,
          retryable: errorData.retryable || false,
          message: errorData.userMessage || errorData.message || "Erreur lors de la synchronisation",
          userMessage: errorData.userMessage
        };
      }
      
      // Erreur générique
      throw new Error("Erreur lors de la synchronisation");
    } else if (error.request) {
      // La requête a été faite mais pas de réponse du serveur
      return {
        success: false,
        errorCode: 'NETWORK_ERROR',
        retryable: true,
        message: "Impossible de contacter le serveur. Vérifiez votre connexion.",
        userMessage: "Impossible de contacter le serveur. Vérifiez votre connexion."
      };
    } else {
      // Erreur lors de la configuration de la requête
      throw new Error(error.message || "Erreur inattendue");
    }
  }
}

/**
 * Vérifie si l'utilisateur a connecté son compte Google Calendar.
 * 
 * @param {number|string} userId - L'identifiant de l'utilisateur
 * @returns {Promise<{connected: boolean, userId: number}>}
 */
export async function checkGoogleCalendarStatus(userId) {
  try {
    const response = await api.get(`/calendar/status/${userId}`);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la vérification du statut Google:", error);
    return { connected: false, userId, error: error.message };
  }
}

/**
 * Synchronise le calendrier de l'utilisateur courant (endpoint simplifié).
 * 
 * @param {number|string} userId - L'identifiant de l'utilisateur
 * @returns {Promise<{success: boolean, message: string}>}
 */
export async function syncCurrentUserCalendar(userId) {
  try {
    const response = await api.post(`/calendar/sync`, null, {
      params: { userId }
    });
    return response.data;
  } catch (error) {
    if (error.response) {
      throw new Error(error.response.data.message || "Erreur de synchronisation");
    }
    throw new Error("Impossible de synchroniser le calendrier");
  }
}