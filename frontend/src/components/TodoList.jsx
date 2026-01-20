import { useState, useEffect } from 'react';
import { useDrag } from 'react-dnd';
import { getUserTasks, getDelegatedTasks, getTeamTasks } from '../api/taskApi';
import Notification from './Notification';

import '../styles/components/TodoList.css';

const ITEM_TYPES = {
  TASK: 'task'
};

// Composant pour une tâche draggable
function DraggableTask({ 
  task, 
  onToggle, 
  onDelete, 
  onEdit, 
  isEditing, 
  onStartEdit, 
  onCancelEdit, 
  currentUser, 
  showAssignee,
  onForbidden // Nouvelle prop pour notifier le parent d'une interdiction
}) {
  const [editData, setEditData] = useState({
    title: task.title,
    estimatedDuration: task.estimatedDuration || task.duration || 30,
    priority: task.priority || 2
  });

  useEffect(() => {
    setEditData({
      title: task.title,
      estimatedDuration: task.estimatedDuration || task.duration || 30,
      priority: task.priority || 2
    });
  }, [task]);

  const [{ isDragging }, drag] = useDrag(() => ({
      type: ITEM_TYPES.TASK,
      item: { task: task }, 
      canDrag: !task.done, // (Vérifiez aussi que ceci ne renvoie pas false)
      collect: (monitor) => ({
          isDragging: !!monitor.isDragging(),
      }),
  }));

  // --- LOGIQUE DE PERMISSION ---
  // On détermine si l'utilisateur a le droit de modifier la tâche
  const canModify = (() => {
    if (!currentUser) return false;
    // 1. L'utilisateur est le créateur de la tâche
    if (task.user_id === currentUser.id) return true;
    // 2. L'utilisateur est assigné à la tâche
    if (task.assignee && task.assignee.id === currentUser.id) return true;
    // Sinon, pas de droits
    return false;
  })();

  // Fonction utilitaire pour vérifier les droits avant d'agir
  const executeIfAllowed = (actionCallback) => {
    if (canModify) {
      actionCallback();
    } else {
      // Déclenche la notification d'interdiction
      onForbidden("⛔ Vous n'avez pas les droits pour modifier cette tâche (ni créateur, ni assigné).");
    }
  };

  const handleEditSubmit = (e) => {
    e.preventDefault();
    // L'édition réelle est gérée par le parent qui vérifiera le succès
    onEdit(task.id, editData);
  };

  const handleCancel = () => {
    setEditData({
      title: task.title,
      estimatedDuration: task.estimatedDuration || task.duration || 30,
      priority: task.priority || 2
    });
    onCancelEdit();
  };

  if (isEditing) {
    return (
      <li className="task-item task-editing">
        <form className="task-edit-form" onSubmit={handleEditSubmit}>
          <input
            type="text"
            value={editData.title}
            onChange={(e) => setEditData({ ...editData, title: e.target.value })}
            placeholder="Titre de la tâche"
            autoFocus
            required
          />
          <div className="edit-form-row">
            <label>
              Durée (min)
              <input
                type="number"
                min="5"
                max="480"
                step="5"
                value={editData.estimatedDuration}
                onChange={(e) => setEditData({ ...editData, estimatedDuration: parseInt(e.target.value) })}
              />
            </label>
            <label>
              Priorité
              <select
                value={editData.priority}
                onChange={(e) => setEditData({ ...editData, priority: parseInt(e.target.value) })}
              >
                <option value="3">Basse</option>
                <option value="2">Moyenne</option>
                <option value="1">Haute</option>
              </select>
            </label>
          </div>
          <div className="edit-form-actions">
            <button type="submit" className="btn-save">✓</button>
            <button type="button" className="btn-cancel-edit" onClick={handleCancel}>×</button>
          </div>
        </form>
      </li>
    );
  }

  const assigneeName = task.assignee ? task.assignee.username : null;
  const isAssignedToOther = assigneeName && currentUser && assigneeName !== currentUser.username;

  return (
    <li
      ref={drag}
      className={`task-item priority-${task.priority} ${isDragging ? 'dragging' : ''} ${task.done ? 'task-done' : ''}`}
    >
      <div className="task-content">
        <input
          type="checkbox"
          className="task-checkbox"
          checked={task.done}
          // On retire le 'disabled' strict pour permettre le clic et afficher la notif d'erreur
          onChange={() => canModify && onToggle(task.id)}
          style={{ cursor: 'pointer' }} 
          disabled={!canModify}
        />
        <div className="task-info">
          <span className="task-title">{task.title}</span>
          <div className="task-meta">
              <span className="task-duration">{task.estimatedDuration || task.duration} min</span>
              {showAssignee && assigneeName && (
                  <span className={`assignee-badge ${isAssignedToOther ? 'other' : 'me'}`}>
                      {isAssignedToOther ? `👤 ${assigneeName}` : '👤 Moi'}
                  </span>
              )}
          </div>
        </div>
        
        {/* On affiche les boutons mais on protège le clic par executeIfAllowed */}
        <div className="task-actions">
          <button 
            className="btn-edit" 
            onClick={() => executeIfAllowed(() => onStartEdit(task.id))}
            style={{ opacity: canModify ? 1 : 0.5 }} // Indice visuel optionnel
          >
            ✏️
          </button>
          <button 
            className="btn-delete" 
            onClick={() => executeIfAllowed(() => onDelete(task.id))}
            style={{ opacity: canModify ? 1 : 0.5 }}
          >
            ×
          </button>
        </div>
      </div>
    </li>
  );
}

export default function TodoList({ onAddTask, onToggleTask, onDeleteTask, onEditTask, contextTeam, currentUser }) {
  const [showForm, setShowForm] = useState(false);
  const [editingTaskId, setEditingTaskId] = useState(null);
  const [localTasks, setLocalTasks] = useState([]); 
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState('ALL'); 
  const [notification, setNotification] = useState(null);

  const [newTask, setNewTask] = useState({
    title: '',
    estimatedDuration: 30,
    priority: 2,
    team: contextTeam ? contextTeam.id : null,
    assigneeId: '' // Initialisé à vide
  });

  // --- Chargement des données selon le contexte (Personnel vs Equipe) ---
  useEffect(() => {
    const fetchTasks = async () => {
      if (!currentUser?.id) return;
      try {
        let tasksData = [];
        
        if (contextTeam) {
          // MODE EQUIPE : On récupère uniquement les tâches de l'équipe
          tasksData = await getTeamTasks(contextTeam.id);
        } else {
          // MODE PERSONNEL : On récupère mes tâches ET les tâches que j'ai déléguées
          const [myTasks, myDelegated] = await Promise.all([
            getUserTasks(currentUser.id),
            getDelegatedTasks(currentUser.id)
          ]);
          
          // On marque les déléguées pour le filtre UI
          const delegatedWithMark = myDelegated.map(t => ({ ...t, _isDelegatedSource: true }));
          
          // ATTENTION : On filtre pour ne garder que ce qui n'appartient à aucune équipe (Espace Personnel)
          tasksData = [...myTasks, ...delegatedWithMark].filter(t => !t.teamId && !t.team);
        }

        const uniqueTasks = Array.from(new Map(tasksData.map(t => [t.id, t])).values());
        setLocalTasks(uniqueTasks);
      } catch (err) {
        console.error("Erreur fetch tasks:", err);
      }
    };
    fetchTasks();
  }, [currentUser, contextTeam, onAddTask, onEditTask, onToggleTask, onDeleteTask]);

  // --- Helpers pour Notification ---
  const triggerNotification = (message, type = 'info') => {
    setNotification({ message, type });
  };

  const closeNotification = () => {
    setNotification(null);
  };

  // --- Wrappers pour injecter les notifications de succès ---
  const handleEditTaskWrapper = async (taskId, data) => {
    try {
      await onEditTask(taskId, data);
      setEditingTaskId(null); // Fermer le mode édition
      triggerNotification("Tâche modifiée avec succès !", "success");
    } catch (error) {
      triggerNotification("Erreur lors de la modification.", "error");
    }
  };

  const handleDeleteTaskWrapper = async (taskId) => {
    if (window.confirm("Voulez-vous vraiment supprimer cette tâche ?")) {
      try {
        await onDeleteTask(taskId);
        triggerNotification("Tâche supprimée avec succès !", "success");
      } catch (error) {
        triggerNotification("Erreur lors de la suppression.", "error");
      }
    }
  };

  const handleForbiddenAction = (message) => {
    triggerNotification(message, "error");
  };


  // --- Logique de filtrage ---
  const getFilteredTasks = () => {
      let filtered = [...localTasks];

      if (searchTerm) {
          filtered = filtered.filter(t => t.title.toLowerCase().includes(searchTerm.toLowerCase()));
      }

      switch (filter) {
          case 'MINE': 
              return filtered.filter(t => t.assignee?.id === currentUser?.id || t.assignee?.username === currentUser?.username);
          case 'DELEGATED':
              // Tâches marquées comme déléguées ou dont l'assigné n'est pas moi
              return filtered.filter(t => t._isDelegatedSource || (t.assignee && t.assignee.id !== currentUser?.id));
          case 'ALL':
          default:
              return filtered;
      }
  };

  const currentFiltered = getFilteredTasks();
  const activeFilteredTasks = currentFiltered.filter(t => !t.done);
  const completedFilteredTasks = currentFiltered.filter(t => t.done);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newTask.title.trim()) {
      // Construction de l'objet à sauvegarder
      const taskToSave = {
        title: newTask.title,
        priority: newTask.priority,
        // Correction 1 : On mappe estimatedDuration vers duration
        estimatedDuration: newTask.estimatedDuration,
        done: false
      };

      // 2. Gestion des relations (Team et Assignee)
      // Spring Boot attend souvent des objets imbriqués { id: X } pour les relations
      if (contextTeam) {
        taskToSave.team = { id: contextTeam.id };

        if (newTask.assigneeId) {
          taskToSave.assignee = { id: parseInt(newTask.assigneeId) };
        } else {
          taskToSave.assignee = null;
        }
      }

      await onAddTask(taskToSave);
      handleCancel();
      // Notification de création (optionnelle, mais cohérente)
      triggerNotification("Tâche créée avec succès !", "success");
    }
  };

  const handleCancel = () => {
    setNewTask({ title: '', estimatedDuration: 30, priority: 2, assigneeId: '' });
    setShowForm(false);
  };

  const handleStartEdit = (taskId) => {
    setEditingTaskId(taskId);
    setShowForm(false);
  };

  return (
    <div className="todo-list" style={{ position: 'relative' }}>
      
      {/* Affichage de la Notification en haut de la liste */}
      <Notification 
        message={notification?.message} 
        type={notification?.type} 
        onClose={closeNotification} 
      />

      <div className="todo-header">
        <h2>{contextTeam ? `Projet ${contextTeam.name}` : '🏠 Mon Espace Personnel'}</h2>
        <div className="header-actions">
            <input 
              type="text" 
              placeholder="Rechercher..." 
              className="search-input"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
            <button className="btn-add-task" onClick={() => { setShowForm(!showForm); setEditingTaskId(null); }}>+</button>
        </div>
      </div>

      {contextTeam ? (
        <div className="task-filters">
          <button className={filter === 'ALL' ? 'active' : ''} onClick={() => setFilter('ALL')}>Tout</button>
          <button className={filter === 'MINE' ? 'active' : ''} onClick={() => setFilter('MINE')}>Moi</button>
          <button className={filter === 'DELEGATED' ? 'active' : ''} onClick={() => setFilter('DELEGATED')}>Délégué</button>
        </div>
      ) : (
        <div className="task-filters-single">
          <span className="active-tab">Toutes mes tâches</span>
        </div>
      )}

      {showForm && (
        <form className="task-form" onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Nouvelle tâche..."
            value={newTask.title}
            onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
            required
          />
          <div className="form-row">
            <label>Durée (min)
              <input type="number" value={newTask.estimatedDuration} onChange={(e) => setNewTask({ ...newTask, estimatedDuration: parseInt(e.target.value) })} />
            </label>
            <label>Priorité
              <select value={newTask.priority} onChange={(e) => setNewTask({ ...newTask, priority: parseInt(e.target.value) })}>
                <option value="3">🟢 Basse</option>
                <option value="2">🟡 Moyenne</option>
                <option value="1">🔴 Haute</option>
              </select>
            </label>
          </div>
          
          {/* Ajout du champ d'assignation si on est dans une équipe */}
          {contextTeam && contextTeam.members && (
            <div className="form-row">
              <label className="assignee-label-full">Assigner à 
                <select 
                  value={newTask.assigneeId} 
                  onChange={(e) => setNewTask({ ...newTask, team: contextTeam.id, assigneeId: e.target.value })}
                  className="assignee-select"
                >
                  <option value="">-- Non assigné --</option>
                  {contextTeam.members.map((member) => (
                    <option key={member.id} value={member.id}>
                      {member.username || member.email}
                    </option>
                  ))}
                </select>
              </label>
            </div>
          )}
          
          <div className="form-actions">
            <button type="submit" className="btn-submit">Ajouter</button>
            <button type="button" className="btn-cancel" onClick={handleCancel}>Annuler</button>
          </div>
        </form>
      )}

      <div className="tasks-section">
        <ul className="task-list">
            {activeFilteredTasks.map((task) => (
              <DraggableTask
                key={task.id}
                task={task}
                onToggle={onToggleTask}
                // On passe les wrappers qui incluent la notification de succès
                onDelete={handleDeleteTaskWrapper}
                onEdit={handleEditTaskWrapper}
                
                isEditing={editingTaskId === task.id}
                onStartEdit={handleStartEdit}
                onCancelEdit={() => setEditingTaskId(null)}
                currentUser={currentUser}
                showAssignee={true}
                // Callback pour l'interdiction
                onForbidden={handleForbiddenAction}
              />
            ))}
        </ul>

        {completedFilteredTasks.length > 0 && (
          <details className="completed-section">
            <summary>Terminées ({completedFilteredTasks.length})</summary>
            <ul className="task-list">
              {completedFilteredTasks.map((task) => (
                <DraggableTask
                  key={task.id}
                  task={task}
                  onToggle={onToggleTask}
                  // On passe les wrappers qui incluent la notification de succès
                  onDelete={handleDeleteTaskWrapper}
                  onEdit={handleEditTaskWrapper}

                  isEditing={editingTaskId === task.id}
                  onStartEdit={handleStartEdit}
                  onCancelEdit={() => setEditingTaskId(null)}
                  currentUser={currentUser}
                  showAssignee={true}
                  onForbidden={handleForbiddenAction}
                />
              ))}
            </ul>
          </details>
        )}
      </div>
    </div>
  );
}

export { ITEM_TYPES };