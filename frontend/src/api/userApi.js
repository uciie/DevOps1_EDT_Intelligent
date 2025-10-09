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
