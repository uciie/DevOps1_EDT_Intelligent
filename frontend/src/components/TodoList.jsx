import { useState } from "react";
import "../styles/components/TodoList.css";

function TodoList({ tasks, onAddTask, onDeleteTask, onToggleTask }) {
  const [isAddingTask, setIsAddingTask] = useState(false);
  const [newTask, setNewTask] = useState({
    title: "",
    estimatedDuration: 60,
    priority: 1,
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    if (newTask.title.trim()) {
      onAddTask(newTask);
      setNewTask({ title: "", estimatedDuration: 60, priority: 1 });
      setIsAddingTask(false);
    }
  };

  const handleDragStart = (e, task) => {
    e.dataTransfer.setData("task", JSON.stringify(task));
    e.dataTransfer.effectAllowed = "move";
    e.currentTarget.classList.add("dragging");
  };

  const handleDragEnd = (e) => {
    e.currentTarget.classList.remove("dragging");
  };

  const undoneTasks = tasks.filter((task) => !task.done);
  const doneTasks = tasks.filter((task) => task.done);

  return (
    <div className="todo-list">
      <div className="todo-header">
        <h2>À faire</h2>
        <button
          className="btn-add-task"
          onClick={() => setIsAddingTask(!isAddingTask)}
          title="Ajouter une tâche"
        >
          {isAddingTask ? "✕" : "+"}
        </button>
      </div>

      {isAddingTask && (
        <form className="task-form" onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Titre de la tâche"
            value={newTask.title}
            onChange={(e) => setNewTask({ ...newTask, title: e.target.value })}
            autoFocus
            required
          />
          <div className="form-row">
            <label>
              Durée (min):
              <input
                type="number"
                min="5"
                max="480"
                value={newTask.estimatedDuration}
                onChange={(e) =>
                  setNewTask({ ...newTask, estimatedDuration: parseInt(e.target.value) })
                }
              />
            </label>
            <label>
              Priorité:
              <select
                value={newTask.priority}
                onChange={(e) =>
                  setNewTask({ ...newTask, priority: parseInt(e.target.value) })
                }
              >
                <option value="1">Basse</option>
                <option value="2">Moyenne</option>
                <option value="3">Haute</option>
              </select>
            </label>
          </div>
          <div className="form-actions">
            <button type="submit" className="btn-submit">
              Ajouter
            </button>
            <button
              type="button"
              className="btn-cancel"
              onClick={() => setIsAddingTask(false)}
            >
              Annuler
            </button>
          </div>
        </form>
      )}

      <div className="tasks-section">
        <p className="section-hint">
          💡 Glissez les tâches vers le calendrier pour les planifier
        </p>

        {undoneTasks.length === 0 && !isAddingTask ? (
          <p className="empty-state">Aucune tâche en attente</p>
        ) : (
          <ul className="task-list">
            {undoneTasks.map((task) => (
              <li
                key={task.id}
                className={`task-item priority-${task.priority}`}
                draggable
                onDragStart={(e) => handleDragStart(e, task)}
                onDragEnd={handleDragEnd}
              >
                <div className="task-content">
                  <input
                    type="checkbox"
                    checked={task.done}
                    onChange={() => onToggleTask(task.id)}
                    className="task-checkbox"
                  />
                  <div className="task-info">
                    <span className="task-title">{task.title}</span>
                    <span className="task-duration">{task.estimatedDuration} min</span>
                  </div>
                </div>
                <button
                  className="btn-delete"
                  onClick={() => onDeleteTask(task.id)}
                  title="Supprimer"
                >
                  🗑️
                </button>
              </li>
            ))}
          </ul>
        )}
      </div>

      {doneTasks.length > 0 && (
        <details className="completed-section">
          <summary>Terminées ({doneTasks.length})</summary>
          <ul className="task-list">
            {doneTasks.map((task) => (
              <li key={task.id} className="task-item task-done">
                <div className="task-content">
                  <input
                    type="checkbox"
                    checked={task.done}
                    onChange={() => onToggleTask(task.id)}
                    className="task-checkbox"
                  />
                  <div className="task-info">
                    <span className="task-title">{task.title}</span>
                  </div>
                </div>
                <button
                  className="btn-delete"
                  onClick={() => onDeleteTask(task.id)}
                  title="Supprimer"
                >
                  🗑️
                </button>
              </li>
            ))}
          </ul>
        </details>
      )}
    </div>
  );
}

export default TodoList;