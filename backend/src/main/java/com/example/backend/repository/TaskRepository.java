package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.model.Task;
import com.example.backend.model.User;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {


    List<Task> findByUser_Id(Long userId);

    // 1. "Mes Tâches" (Vue principale) : Tâches où je suis l'assigné
    // Correspond à CA-04 (Filtre "Assignées à moi")
    List<Task> findByAssignee(User assignee);

    // 2. "Tâches Déléguées" : Tâches que j'ai créées MAIS assignées à quelqu'un d'autre
    // Correspond à CA-04 (Filtre "Déléguées")
    List<Task> findByUserAndAssigneeNot(User creator, User assignee);

    // 3. Tâches d'une équipe spécifique (RM-04)
    List<Task> findByTeamId(Long teamId);

}
