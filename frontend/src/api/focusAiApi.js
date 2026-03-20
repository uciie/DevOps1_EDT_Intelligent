import api from './api';

/**
 * Service dédié à la fonctionnalité d'upload de PDF et validation par l'IA.
 */

// Étape 1 : Envoyer le PDF et recevoir les suggestions
export const uploadSchedulePdf = async (userId, file) => {
    const formData = new FormData();
    formData.append('file', file);
    
    // On ajoute sessionId ici (on utilise userId comme valeur par défaut si pas de session spécifique)
    const response = await api.post(`/focus/ai/upload?userId=${userId}&sessionId=${userId}`, formData, {
        headers: { 
            'Content-Type': 'multipart/form-data' 
        }
    });
    return response.data;
};
// Étape 2 : Valider et enregistrer les tâches (votre curl opérationnel)
export const confirmTasks = async (userId, globalExplanation, tasks) => {
    try {
        const response = await api.post(`/focus/ai/validate?userId=${userId}`, {
            globalExplanation: globalExplanation,
            tasks: tasks
        });
        return response.data; // Retourne "Planning mis à jour et optimisé !"
    } catch (error) {
        console.error("Erreur lors de la validation des tâches:", error);
        throw error;
    }
};


export const refineSchedule = async (userFeedback, currentTasks) => {
    try {
        const response = await api.post(`/refine-ai`, {
            userFeedback: userFeedback,
            currentTasks: currentTasks
        });
        return response.data; // Renvoie le nouveau AIPlanningResponse
    } catch (error) {
        console.error("Erreur lors du raffinement du planning:", error);
        throw error;
    }
};