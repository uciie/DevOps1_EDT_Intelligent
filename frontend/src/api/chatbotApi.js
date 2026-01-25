import api from "./api";

/**
 * Envoie un message au chatbot et récupère la réponse
 * @param {string} message - Le message de l'utilisateur
 * @param {number} userId - L'ID de l'utilisateur
 * @returns {Promise<string>} La réponse du chatbot
 */
export async function sendChatMessage(message, userId) {
  try {
    const response = await api.post("/chat/message", {
      message: message,
      userId: userId.toString()
    });
    
    // Le backend renvoie directement une string
    return response.data;
  } catch (error) {
    console.error("Erreur lors de l'envoi du message:", error);
    throw new Error("Impossible de contacter l'assistant. Réessayez plus tard.");
  }
}