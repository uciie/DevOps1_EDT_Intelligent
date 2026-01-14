import { useState, useEffect } from 'react';
import { useDrag } from 'react-dnd';
import { getUserTasks, getDelegatedTasks, getTeamTasks } from '../api/taskApi';

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
              {showAssignee && assigneeName && (
                  <span className={`assignee-badge ${isAssignedToOther ? 'other' : 'me'}`}>
                      {isAssignedToOther ? `👤 ${assigneeName}` : '👤 Moi'}
                  </span>
              )}
          </div>
        </div>
        <div className="task-actions">
          <button className="btn-edit" onClick={() => onStartEdit(task.id)}>✏️</button>
          <button className="btn-delete" onClick={() => onDelete(task.id)}>×</button>
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

  const [newTask, setNewTask] = useState({
    title: '',
    durationMinutes: 30,
    priority: 2,
    assigneeId: ''
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

  // --- Logique de filtrage par onglets ---
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
  const activeFilteredTasks = currentFiltered.filter(t => !t.completed);
  const completedFilteredTasks = currentFiltered.filter(t => t.completed);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newTask.title.trim()) {
      // Si on est dans une équipe, on injecte le teamId
      const taskToSave = contextTeam ? { ...newTask, teamId: contextTeam.id } : newTask;
      await onAddTask(taskToSave);
      handleCancel();
    }
  };

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

      {/* --- AFFICHAGE CONDITIONNEL DES FILTRES --- */}
      {contextTeam ? (
        <div className="task-filters">
          <button className={filter === 'ALL' ? 'active' : ''} onClick={() => setFilter('ALL')}>Tout</button>
          <button className={filter === 'MINE' ? 'active' : ''} onClick={() => setFilter('MINE')}>Moi</button>
          <button className={filter === 'DELEGATED' ? 'active' : ''} onClick={() => setFilter('DELEGATED')}>Délégué</button>
        </div>
      ) : (
        /* En mode Personnel, on force l'affichage "Tout" sans boutons */
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
              <input type="number" value={newTask.durationMinutes} onChange={(e) => setNewTask({ ...newTask, durationMinutes: parseInt(e.target.value) })} />
            </label>
            <label>Priorité
              <select value={newTask.priority} onChange={(e) => setNewTask({ ...newTask, priority: parseInt(e.target.value) })}>
                <option value="3">🟢 Basse</option>
                <option value="2">🟡 Moyenne</option>
                <option value="1">🔴 Haute</option>
              </select>
            </label>
          </div>
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
                onDelete={onDeleteTask}
                onEdit={onEditTask}
                isEditing={editingTaskId === task.id}
                onStartEdit={handleStartEdit}
                onCancelEdit={() => setEditingTaskId(null)}
                currentUser={currentUser}
                showAssignee={true}
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
                  showAssignee={true}
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