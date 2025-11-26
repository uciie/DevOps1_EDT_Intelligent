import api from "./api";

/**
 * Récupère toutes les tâches d'un utilisateur
 */
export async function getUserTasks(userId) {
  try {
    const response = await api.get(`/tasks/user/${userId}`);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la récupération des tâches:", error);
    throw error;
  }
}

/**
 * Crée une nouvelle tâche
 */
export async function createTask(taskData) {
  try {
    const response = await api.post("/tasks", taskData);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la création de la tâche:", error);
    throw error;
  }
}

/**
 * Met à jour une tâche
 */
export async function updateTask(taskId, taskData) {
  try {
    const response = await api.put(`/tasks/${taskId}`, taskData);
    return response.data;
  } catch (error) {
    console.error("Erreur lors de la mise à jour de la tâche:", error);
    throw error;
  }
}

/**
 * Supprime une tâche
 */
export async function deleteTask(taskId) {
  try {
    await api.delete(`/tasks/${taskId}`);
  } catch (error) {
    console.error("Erreur lors de la suppression de la tâche:", error);
    throw error;
  }
}