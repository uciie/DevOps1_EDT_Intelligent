import { useState, useEffect } from 'react';
import { useDrag } from 'react-dnd';
import '../styles/components/TodoList.css';

const ITEM_TYPES = {
  TASK: 'task'
};

// Composant pour une tâche draggable
function DraggableTask({ task, onToggle, onDelete, onEdit, isEditing, onStartEdit, onCancelEdit, currentUser, showAssignee }) {
  const [editData, setEditData] = useState({
    title: task.title,
    durationMinutes: task.durationMinutes || task.duration || 30,
    priority: task.priority || 2
  });

  useEffect(() => {
    setEditData({
      title: task.title,
      durationMinutes: task.durationMinutes || task.duration || 30,
      priority: task.priority || 2
    });
  }, [task]);

  const [{ isDragging }, drag] = useDrag(() => ({
    type: ITEM_TYPES.TASK,
    item: { task },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }), [task]);

  const handleEditSubmit = (e) => {
    e.preventDefault();
    onEdit(task.id, editData);
  };

  const handleCancel = () => {
    setEditData({
      title: task.title,
      durationMinutes: task.durationMinutes || task.duration || 30,
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
                value={editData.durationMinutes}
                onChange={(e) => setEditData({ ...editData, durationMinutes: parseInt(e.target.value) })}
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

  // Calcul pour savoir si la tâche est assignée à quelqu'un d'autre
  const assigneeName = task.assignee ? task.assignee.username : null;
  const isAssignedToOther = assigneeName && currentUser && assigneeName !== currentUser.username;

  return (
    <li
      ref={drag}
      className={`task-item priority-${task.priority} ${isDragging ? 'dragging' : ''} ${task.completed ? 'task-done' : ''}`}
    >
      <div className="task-content">
        <input
          type="checkbox"
          className="task-checkbox"
          checked={task.completed}
          onChange={() => onToggle(task.id)}
        />
        <div className="task-info">
          <span className="task-title">{task.title}</span>
          <div className="task-meta">
              <span className="task-duration">{task.durationMinutes || task.duration} min</span>
              {/* Badge d'assignation si pertinent */}
              {showAssignee && assigneeName && (
                  <span className={`assignee-badge ${isAssignedToOther ? 'other' : 'me'}`}>
                      {isAssignedToOther ? `👤 ${assigneeName}` : '👤 Moi'}
                  </span>
              )}
          </div>
        </div>
        <div className="task-actions">
          {/* On ne peut éditer/supprimer que ses propres tâches ou celles qu'on a créées (logique simplifiée) */}
          <button className="btn-edit" onClick={() => onStartEdit(task.id)}>✏️</button>
          <button className="btn-delete" onClick={() => onDelete(task.id)}>×</button>
        </div>
      </div>
    </li>
  );
}

// Props: contextTeam et currentUser sont nouveaux
export default function TodoList({ tasks = [], onAddTask, onToggleTask, onDeleteTask, onEditTask, contextTeam, currentUser }) {
  const [showForm, setShowForm] = useState(false);
  const [editingTaskId, setEditingTaskId] = useState(null);
  
  // États filtres
  const [filter, setFilter] = useState('ALL'); // 'ALL', 'MINE', 'DELEGATED'

  // État formulaire ajout
  const [newTask, setNewTask] = useState({
    title: '',
    durationMinutes: 30,
    priority: 2,
    assigneeId: '' // CORRECTION: Utilisation de l'ID au lieu du username pour éviter l'erreur 500
  });

  // Reset filter when context changes
  useEffect(() => {
      setFilter('ALL');
  }, [contextTeam]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newTask.title.trim()) {
      try {
        // CORRECTION: Déterminer l'ID de l'assigné (soit selectionné, soit l'utilisateur courant)
        const targetAssigneeId = newTask.assigneeId ? newTask.assigneeId : currentUser?.id;

        // Construction de l'objet tâche enrichi pour la collaboration
        const taskPayload = {
          title: newTask.title,
          durationMinutes: newTask.durationMinutes,
          priority: newTask.priority,
          // Si une équipe est active, on lie la tâche
          team: contextTeam ? { id: contextTeam.id } : null,
          // Si un assignee est sélectionné, on l'ajoute via son ID
          assignee: { id: targetAssigneeId }
        };

        await onAddTask(taskPayload);
        
        setNewTask({ title: '', durationMinutes: 30, priority: 2, assigneeId: '' });
        setShowForm(false);
      } catch (error) {
        console.error('Erreur lors de l\'ajout de la tâche:', error);
      }
    }
  };

  // --- LOGIQUE DE FILTRAGE (RM-04) ---
  const getFilteredTasks = () => {
      let filtered = tasks;

      // 1. Filtrage par équipe (Contexte)
      if (contextTeam) {
          // On ne garde que les tâches de l'équipe (supposons que la tâche a un champ teamId ou team.id)
          // Note: Si le backend ne renvoie pas l'objet team complet, vérifiez les IDs.
          filtered = tasks.filter(t => t.team && t.team.id === contextTeam.id);
      } else {
          // Mode personnel : on ne montre que les tâches sans équipe (ou explicitement perso)
          filtered = tasks.filter(t => !t.team);
      }

      // 2. Filtrage par onglet (RM-04)
      switch (filter) {
          case 'MINE': // Assignées à moi
              return filtered.filter(t => t.assignee && t.assignee.username === currentUser?.username);
          case 'DELEGATED': // Créées par moi pour les autres
             // Note: t.user est le créateur
             return filtered.filter(t => 
                 (t.user && t.user.username === currentUser?.username) && 
                 (t.assignee && t.assignee.username !== currentUser?.username)
             );
          case 'ALL': // Toutes les tâches du projet
          default:
              return filtered;
      }
  };

  const activeFilteredTasks = getFilteredTasks().filter(t => !t.completed);
  const completedFilteredTasks = getFilteredTasks().filter(t => t.completed);

  // Helper form interactions
  const handleCancel = () => {
    setNewTask({ title: '', durationMinutes: 30, priority: 2, assigneeId: '' });
    setShowForm(false);
  };

  const handleStartEdit = (taskId) => {
    setEditingTaskId(taskId);
    setShowForm(false);
  };

  return (
    <div className="todo-list">
      <div className="todo-header">
        <h2>{contextTeam ? `Projet ${contextTeam.name}` : '📋 Mes Tâches'}</h2>
        <button
          className="btn-add-task"
          onClick={() => { setShowForm(!showForm); setEditingTaskId(null); }}
        >
          +
        </button>
      </div>

      {/* --- ONGLETS DE FILTRES --- */}
      {contextTeam && (
          <div className="task-filters">
              <button 
                  className={filter === 'ALL' ? 'active' : ''} 
                  onClick={() => setFilter('ALL')}
                  title="Tout le projet"
              >Tout</button>
              <button 
                  className={filter === 'MINE' ? 'active' : ''} 
                  onClick={() => setFilter('MINE')}
                  title="Mes tâches"
              >Moi</button>
              <button 
                  className={filter === 'DELEGATED' ? 'active' : ''} 
                  onClick={() => setFilter('DELEGATED')}
                  title="Tâches déléguées"
              >Délégué</button>
          </div>
      )}

      {showForm && (
        <form className="task-form" onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Nouvelle tâche..."
            value={newTask.title}
            onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
            autoFocus
            required
          />
          <div className="form-row">
            <label>
              Durée (min)
              <input
                type="number" min="5" max="480" step="5"
                value={newTask.durationMinutes}
                onChange={(e) => setNewTask({ ...newTask, durationMinutes: parseInt(e.target.value) })}
              />
            </label>
            <label>
              Priorité
              <select
                value={newTask.priority}
                onChange={(e) => setNewTask({ ...newTask, priority: parseInt(e.target.value) })}
              >
                <option value="3">🟢 Basse</option>
                <option value="2">🟡 Moyenne</option>
                <option value="1">🔴 Haute</option>
              </select>
            </label>
          </div>

          {/* SÉLECTEUR D'ASSIGNATION (Seulement si mode équipe) */}
          {contextTeam && contextTeam.members && (
             <div className="form-row">
                 <label style={{width: '100%'}}>
                     Assigner à :
                     <select
                        className="assign-select"
                        value={newTask.assigneeId} // CORRECTION : value est l'ID
                        onChange={(e) => setNewTask({ ...newTask, assigneeId: e.target.value })}
                     >
                         <option value="">Moi-même</option>
                         {contextTeam.members
                            .filter(m => m.username !== currentUser?.username)
                            .map(m => (
                                // CORRECTION : value={m.id} pour envoyer l'ID
                                <option key={m.id} value={m.id}>{m.username}</option>
                            ))
                         }
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
        {activeFilteredTasks.length === 0 && !showForm && (
          <div className="empty-state">
            Aucune tâche ici.<br />
            {contextTeam ? "Utilisez + pour déléguer." : "Ajoutez-en une !"}
          </div>
        )}

        <ul className="task-list">
            {activeFilteredTasks.map((task) => (
              <DraggableTask
                key={task.id}
                task={task}
                onToggle={onToggleTask}
                onDelete={onDeleteTask}
                onEdit={onEditTask} // Note: onEditTask doit être passé ici
                isEditing={editingTaskId === task.id}
                onStartEdit={handleStartEdit}
                onCancelEdit={() => setEditingTaskId(null)}
                currentUser={currentUser}
                showAssignee={!!contextTeam} // Afficher l'assigné si on est en mode équipe
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
                  onDelete={onDeleteTask}
                  onEdit={onEditTask}
                  isEditing={editingTaskId === task.id}
                  onStartEdit={handleStartEdit}
                  onCancelEdit={() => setEditingTaskId(null)}
                  currentUser={currentUser}
                  showAssignee={!!contextTeam}
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