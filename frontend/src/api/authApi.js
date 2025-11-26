import api from "./api";

/**
 * Connecte un utilisateur via l'endpoint /api/users/login
 * @param {Object} credentials - { username, password }
 * @returns {Promise<Object>} Utilisateur connecté avec son ID
 */
export async function loginUser(credentials) {
  try {
    const response = await api.post("/users/login", {
      username: credentials.username,
      password: credentials.password
    });

    const user = response.data;

    // Sauvegarder l'utilisateur dans localStorage
    const userData = {
      id: user.id,
      username: user.username,
      email: user.email
    };
    localStorage.setItem("currentUser", JSON.stringify(userData));

    return userData;
  } catch (error) {
    console.error("Erreur API loginUser:", error);
    
    // Gestion des erreurs spécifiques
    if (error.response) {
      switch (error.response.status) {
        case 401:
          throw new Error("Identifiants incorrects");
        case 404:
          throw new Error("Utilisateur non trouvé");
        default:
          throw new Error("Erreur de connexion");
      }
    }
    throw new Error("Impossible de se connecter au serveur");
  }
}

/**
 * Déconnecte l'utilisateur actuel
 */
export function logoutUser() {
  localStorage.removeItem("currentUser");
}

/**
 * Récupère l'utilisateur actuellement connecté depuis localStorage
 * @returns {Object|null} L'utilisateur connecté ou null
 */
export function getCurrentUser() {
  const userStr = localStorage.getItem("currentUser");
  if (!userStr) return null;

  try {
    return JSON.parse(userStr);
  } catch (error) {
    console.error("Erreur lors de la lecture de l'utilisateur:", error);
    return null;
  }
}

/**
 * Vérifie si un utilisateur est connecté
 * @returns {boolean}
 */
export function isAuthenticated() {
  return getCurrentUser() !== null;
}