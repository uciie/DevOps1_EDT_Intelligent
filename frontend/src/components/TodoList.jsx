import { useState } from 'react';
import { useDrag } from 'react-dnd';
import '../styles/components/TodoList.css';

const ITEM_TYPES = {
  TASK: 'task'
};

// Composant pour une tâche draggable
function DraggableTask({ task, onToggle, onDelete }) {
  const [{ isDragging }, drag] = useDrag(() => ({
    type: ITEM_TYPES.TASK,
    item: { task },
    collect: (monitor) => ({
      isDragging: monitor.isDragging(),
    }),
  }), [task]);

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
        <button
          className="btn-delete"
          onClick={() => onDelete(task.id)}
          aria-label="Supprimer la tâche"
        >
          ×
        </button>
      </div>
    </li>
  );
}

export default function TodoList({ tasks = [], completedTasks = [], onAddTask, onToggleTask, onDeleteTask }) {
  const [showForm, setShowForm] = useState(false);
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

  const activeTasks = tasks;

  return (
    <div className="todo-list">
      <div className="todo-header">
        <h2>📋 Mes Tâches</h2>
        <button
          className="btn-add-task"
          onClick={() => setShowForm(!showForm)}
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
        {!showForm && activeTasks.length > 0 && (
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