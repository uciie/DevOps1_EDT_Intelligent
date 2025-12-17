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
