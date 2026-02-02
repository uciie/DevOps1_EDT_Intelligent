import api from "./api";

/**
 * Déclenche manuellement une synchronisation Google Calendar (pull)
 * pour l'utilisateur courant.
 * Le backend (CalendarSyncScheduler / CalendarImportService) fait déjà
 * le pull toutes les 15 min ; cette route permet de le forcer à la demande.
 *
 * @param {number|string} userId
 * @returns {Promise<void>}
 */
export async function syncGoogleCalendar(userId) {
  // POST /api/calendar/sync/pull/{userId}
  // Le contrôleur côté backend peut être un @PostMapping simple qui
  // appelle calendarImportService.pullEventsFromGoogle(user).
  // Si cet endpoint n'existe pas encore, il suffit de en créer un petit
  // dans un CalendarSyncController :
  //
  //   @PostMapping("/calendar/sync/pull/{userId}")
  //   public ResponseEntity<Void> pullNow(@PathVariable Long userId) {
  //       User user = userRepository.findById(userId).orElseThrow();
  //       calendarImportService.pullEventsFromGoogle(user);
  //       return ResponseEntity.ok().build();
  //   }
  //
  const response = await api.post(`/calendar/sync/pull/${userId}`);
  return response.data;
}