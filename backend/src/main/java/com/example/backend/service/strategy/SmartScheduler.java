package com.example.backend.service.strategy;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V; // Indispensable pour lier les variables
import com.example.backend.record.AIPlanningResponse;
import com.example.backend.record.AIProposedTask;
import java.util.List;

public interface SmartScheduler {

    // Premier appel (Création)
    @SystemMessage("""
        Tu es un expert en planification d'études. 
        Analyse la demande, estime les durées et trouve les dépendances.
        Utilise le 'Chain of Thought' pour expliquer tes choix.
        Réponds TOUJOURS au format JSON conforme à AIPlanningResponse.
        """)
    AIPlanningResponse chat(@UserMessage String content);

    // Deuxième appel (La boucle d'échange / Raffinement)
    @SystemMessage("""
        Tu es un expert en planification. 
        L'utilisateur possède déjà un planning mais veut le modifier.
        Ajuste les tâches en fonction de son feedback sans changer les IDs inutiles.
        Réponds TOUJOURS au format JSON conforme à AIPlanningResponse.
        """)
    @UserMessage("Voici le planning actuel : {{tasks}}. L'utilisateur demande : {{feedback}}")
    AIPlanningResponse refine(@V("tasks") List<AIProposedTask> tasks, @V("feedback") String feedback);
}