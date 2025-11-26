import { useState } from 'react';
import { useDrag } from 'react-dnd';
import '../styles/components/TodoList.css';

const ITEM_TYPES = {
  TASK: 'task'
};

// Composant pour une tâche draggable
function DraggableTask({ task, onToggle, onDelete, onEdit, isEditing, onStartEdit, onCancelEdit }) {
  const [editData, setEditData] = useState({
    title: task.title,
    durationMinutes: task.durationMinutes || task.duration || 30,
    priority: task.priority || 2
  });

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

  // Si la tâche est en mode édition, afficher le formulaire
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
                <option value="1">🟢 Basse</option>
                <option value="2">🟡 Moyenne</option>
                <option value="3">🔴 Haute</option>
              </select>
            </label>
          </div>
          <div className="edit-form-actions">
            <button type="submit" className="btn-save">
              ✓ Sauvegarder
            </button>
            <button type="button" className="btn-cancel-edit" onClick={handleCancel}>
              Annuler
            </button>
          </div>
        </form>
      </li>
    );
  }

  // Affichage normal de la tâche
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
          <span className="task-duration">{task.durationMinutes || task.duration} min</span>
        </div>
        <div className="task-actions">
          <button
            className="btn-edit"
            onClick={() => onStartEdit(task.id)}
            aria-label="Modifier la tâche"
            title="Modifier"
          >
            ✏️
          </button>
          <button
            className="btn-delete"
            onClick={() => onDelete(task.id)}
            aria-label="Supprimer la tâche"
            title="Supprimer"
          >
            ×
          </button>
        </div>
      </div>
    </li>
  );
}

export default function TodoList({ tasks = [], completedTasks = [], onAddTask, onToggleTask, onDeleteTask, onEditTask }) {
  const [showForm, setShowForm] = useState(false);
  const [editingTaskId, setEditingTaskId] = useState(null);
  const [newTask, setNewTask] = useState({
    title: '',
    durationMinutes: 30,
    priority: 2
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newTask.title.trim()) {
      try {
        await onAddTask({
          title: newTask.title,
          durationMinutes: newTask.durationMinutes,
          priority: newTask.priority
        });
        setNewTask({ title: '', durationMinutes: 30, priority: 2 });
        setShowForm(false);
      } catch (error) {
        console.error('Erreur lors de l\'ajout de la tâche:', error);
      }
    }
  };

  const handleCancel = () => {
    setNewTask({ title: '', durationMinutes: 30, priority: 2 });
    setShowForm(false);
  };

  const handleStartEdit = (taskId) => {
    setEditingTaskId(taskId);
    setShowForm(false); // Fermer le formulaire d'ajout si ouvert
  };

  const handleCancelEdit = () => {
    setEditingTaskId(null);
  };

  const handleEdit = async (taskId, editData) => {
    try {
      await onEditTask(taskId, editData);
      setEditingTaskId(null);
    } catch (error) {
      console.error('Erreur lors de la modification de la tâche:', error);
    }
  };

  const activeTasks = tasks;

  return (
    <div className="todo-list">
      <div className="todo-header">
        <h2>📋 Mes Tâches</h2>
        <button
          className="btn-add-task"
          onClick={() => {
            setShowForm(!showForm);
            setEditingTaskId(null); // Fermer l'édition si ouverte
          }}
          aria-label="Ajouter une tâche"
        >
          +
        </button>
      </div>

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
                type="number"
                min="5"
                max="480"
                step="5"
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
                <option value="1">🟢 Basse</option>
                <option value="2">🟡 Moyenne</option>
                <option value="3">🔴 Haute</option>
              </select>
            </label>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn-submit">
              ✓ Ajouter
            </button>
            <button type="button" className="btn-cancel" onClick={handleCancel}>
              Annuler
            </button>
          </div>
        </form>
      )}

      <div className="tasks-section">
        {!showForm && activeTasks.length > 0 && !editingTaskId && (
          <div className="section-hint">
            💡 Glissez les tâches vers le calendrier pour les planifier
          </div>
        )}

        {activeTasks.length === 0 && !showForm && (
          <div className="empty-state">
            Aucune tâche en cours.<br />
            Cliquez sur <strong>+</strong> pour en ajouter une.
          </div>
        )}

        {activeTasks.length > 0 && (
          <ul className="task-list">
            {activeTasks.map((task) => (
              <DraggableTask
                key={task.id}
                task={task}
                onToggle={onToggleTask}
                onDelete={onDeleteTask}
                onEdit={handleEdit}
                isEditing={editingTaskId === task.id}
                onStartEdit={handleStartEdit}
                onCancelEdit={handleCancelEdit}
              />
            ))}
          </ul>
        )}

        {completedTasks.length > 0 && (
          <details className="completed-section">
            <summary>
              Terminées ({completedTasks.length})
            </summary>
            <ul className="task-list">
              {completedTasks.map((task) => (
                <DraggableTask
                  key={task.id}
                  task={task}
                  onToggle={onToggleTask}
                  onDelete={onDeleteTask}
                  onEdit={handleEdit}
                  isEditing={editingTaskId === task.id}
                  onStartEdit={handleStartEdit}
                  onCancelEdit={handleCancelEdit}
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