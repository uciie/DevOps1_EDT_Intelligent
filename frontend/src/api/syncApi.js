import api from "./api";

/**
 * Déclenche manuellement une synchronisation Google Calendar (pull)
 * pour l'utilisateur courant.
 * Le backend (CalendarSyncScheduler / CalendarImportService) fait déjà
 * le pull toutes les 15 min ; cette route permet de le forcer à la demande.
 *
 * @param {number|string} userId
 * @returns {Promise<{success: boolean, message: string, syncedCount?: number, timestamp?: number}>}
 * @throws {Error} En cas d'erreur réseau ou serveur
 */
export async function syncGoogleCalendar(userId) {
  try {
    // POST /api/calendar/sync/pull/{userId}
    // Le contrôleur côté backend peut être un @PostMapping simple qui
    // appelle calendarImportService.pullEventsFromGoogle(user).
    // Si cet endpoint n'existe pas encore, il suffit de en créer un petit
    // dans un CalendarSyncController :
    //
    //   @PostMapping("/api/calendar/sync/pull/{userId}")
    //   public ResponseEntity<Map<String, Object>> pullNow(@PathVariable Long userId) {
    //       Map<String, Object> response = new HashMap<>();
    //       try {
    //           User user = userRepository.findById(userId).orElseThrow();
    //           int syncedCount = calendarImportService.pullEventsFromGoogle(user);
    //           response.put("success", true);
    //           response.put("message", "Synchronisation réussie");
    //           response.put("syncedCount", syncedCount);
    //           response.put("timestamp", System.currentTimeMillis());
    //           return ResponseEntity.ok(response);
    //       } catch (Exception e) {
    //           response.put("success", false);
    //           response.put("message", e.getMessage());
    //           return ResponseEntity.status(500).body(response);
    //       }
    //   }
    //
    const response = await api.post(`/calendar/sync/pull/${userId}`);
    return response.data;
  } catch (error) {
    // Gestion des erreurs HTTP
    if (error.response) {
      // Le serveur a répondu avec un code d'erreur (4xx, 5xx)
      const errorData = error.response.data;
      
      // Si le backend retourne un objet structuré avec success: false
      if (errorData && typeof errorData === 'object') {
        throw new Error(errorData.message || "Erreur lors de la synchronisation");
      }
      
      // Sinon, message d'erreur générique basé sur le code HTTP
      if (error.response.status === 401) {
        throw new Error("Compte Google non lié. Veuillez vous connecter à Google Calendar.");
      } else if (error.response.status === 404) {
        throw new Error("Utilisateur introuvable");
      } else if (error.response.status === 500) {
        throw new Error("Erreur serveur lors de la synchronisation");
      } else {
        throw new Error("Erreur lors de la synchronisation");
      }
    } else if (error.request) {
      // La requête a été faite mais pas de réponse du serveur
      throw new Error("Impossible de contacter le serveur. Vérifiez votre connexion.");
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