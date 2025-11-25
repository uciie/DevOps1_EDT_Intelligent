import api from "./api";

/**
 * Connecte un utilisateur
 * @param {Object} credentials - { username, password }
 * @returns {Promise<Object>} Utilisateur connecté avec son ID
 */
export async function loginUser(credentials) {
  try {
    // Tentative de récupération de tous les utilisateurs
    const response = await api.get("/users");
    const users = response.data;

    // Recherche de l'utilisateur avec les identifiants fournis
    const user = users.find(
      (u) =>
        u.username === credentials.username &&
        u.password === credentials.password
    );

    if (!user) {
      throw new Error("Invalid credentials");
    }

    // Retourner l'utilisateur sans le mot de passe
    return {
      id: user.id,
      username: user.username,
    };
  } catch (error) {
    console.error("Erreur API loginUser:", error);
    throw error;
  }
}

/**
 * Déconnecte l'utilisateur actuel
 */
export function logoutUser() {
  localStorage.removeItem("currentUser");
}

/**
 * Vérifie si un utilisateur est connecté
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