import api from "./api";

export async function registerUser(userData) {
  try {
    const response = await api.post("/users/register", userData);
    return response.data.id;
  } catch (error) {
    console.error("Erreur API registerUser :", error);
    throw error;
  }
}

export async function getUserId(username) {
  try {
    const response = await api.get(`/users/username/${username}`);
    return response.data.id;
  } catch (error) {
    console.error("Erreur API getUserId :", error);
    throw error;
  }
}

/**
 * Envoie le code d'autorisation au backend pour l'échanger contre des tokens.
 * @param {number|string} userId - L'ID de l'utilisateur.
 * @param {string} code - Le code d'autorisation reçu de Google.
 */
export async function linkGoogleAccount(userId, code) {
  try {
    // Correspond à l'endpoint @PostMapping("/{id}/google-auth") du UserController
    const response = await api.post(`/users/${userId}/google-auth`, { code });
    return response.data;
  } catch (error) {
    console.error("Erreur API linkGoogleAccount :", error);
    throw error;
  }
}

/**
 * Génère l'URL d'authentification Google OAuth2.
 * Note : Le Client ID doit idéalement être une variable d'environnement (VITE_GOOGLE_CLIENT_ID).
 */
export function getGoogleAuthUrl() {
  const rootUrl = "https://accounts.google.com/o/oauth2/v2/auth";
  
  const options = {
    // Utilisez votre Client ID (extrait de application.properties)
    client_id: import.meta.env.VITE_GOOGLE_CLIENT_ID,
    redirect_uri: "http://localhost:5173/google-callback",
    response_type: "code",
    scope: [
      "https://www.googleapis.com/auth/userinfo.profile",
      "https://www.googleapis.com/auth/userinfo.email",
      "https://www.googleapis.com/auth/calendar"
    ].join(" "),
    access_type: "offline", // Requis pour obtenir le googleRefreshToken
    prompt: "consent",
  };

  const qs = new URLSearchParams(options);
  return `${rootUrl}?${qs.toString()}`;
}

/**
 * Révoque l'accès Google Calendar de l'utilisateur.
 * Efface les tokens en base de données côté backend.
 *
 * @param {number|string} userId - L'ID de l'utilisateur.
 * @returns {Promise<Object>} Confirmation de la déconnexion
 */
export async function unlinkGoogleAccount(userId) {
  try {
    const response = await api.delete(`/users/${userId}/google-auth`);
    return response.data;
  } catch (error) {
    console.error("Erreur API unlinkGoogleAccount :", error);
    throw error;
  }
}