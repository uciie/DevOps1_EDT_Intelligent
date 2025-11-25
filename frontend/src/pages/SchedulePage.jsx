import React, { useState, useEffect } from 'react';
import Calendar from '../components/Calendar';
import TodoList from '../components/TodoList';
import { getCurrentUser } from '../api/authApi';
import { getUserTasks, createTask, updateTask, deleteTask } from '../api/taskApi';
import '../styles/pages/SchedulePage.css';

function SchedulePage() {
  const [tasks, setTasks] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadUserData = async () => {
      try {
        setLoading(true);
        setError(null);
        
        const user = getCurrentUser();
        if (!user) {
          setError("Utilisateur non connecté");
          return;
        }
        
        setCurrentUser(user);
        const userTasks = await getUserTasks(user.id);
        setTasks(userTasks || []);
        setEvents([]);
        
      } catch (err) {
        console.error("Erreur lors du chargement des données:", err);
        setError("Impossible de charger vos données");
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);

  const handleAddTask = async (taskData) => {
    try {
      const newTask = {
        ...taskData,
        userId: currentUser.id,
        completed: false,
        scheduledTime: null
      };
      
      const createdTask = await createTask(newTask);
      setTasks([...tasks, createdTask]);
      return createdTask;
    } catch (err) {
      console.error("Erreur lors de l'ajout de la tâche:", err);
      setError("Impossible d'ajouter la tâche");
      throw err;
    }
  };

  const handleToggleTask = async (taskId) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return;

      const updatedTask = {
        ...task,
        completed: !task.completed
      };

      await updateTask(taskId, updatedTask);
      setTasks(tasks.map(t => t.id === taskId ? updatedTask : t));
    } catch (err) {
      console.error("Erreur lors de la mise à jour de la tâche:", err);
      setError("Impossible de mettre à jour la tâche");
    }
  };

  const handleDeleteTask = async (taskId) => {
    try {
      await deleteTask(taskId);
      setTasks(tasks.filter(t => t.id !== taskId));
      setEvents(events.filter(e => e.taskId !== taskId));
    } catch (err) {
      console.error("Erreur lors de la suppression de la tâche:", err);
      setError("Impossible de supprimer la tâche");
    }
  };

  const handleDropTaskOnCalendar = async (taskId, day, hour) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return;

      const startTime = new Date(day);
      startTime.setHours(hour, 0, 0, 0);
      
      const endTime = new Date(startTime);
      endTime.setMinutes(endTime.getMinutes() + (task.durationMinutes || 60));

      const newEvent = {
        id: `event-${Date.now()}`,
        taskId: task.id,
        title: task.title,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        priority: task.priority,
        day: day.toISOString().split('T')[0],
        hour: hour
      };

      const updatedTask = {
        ...task,
        scheduledTime: startTime.toISOString()
      };

      await updateTask(taskId, updatedTask);
      
      setEvents([...events, newEvent]);
      setTasks(tasks.map(t => t.id === taskId ? updatedTask : t));
      
    } catch (err) {
      console.error("Erreur lors de la planification de la tâche:", err);
      setError("Impossible de planifier la tâche");
    }
  };

  const handleDeleteEvent = (eventId) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event) return;

      if (event.taskId) {
        const task = tasks.find(t => t.id === event.taskId);
        if (task) {
          const updatedTask = {
            ...task,
            scheduledTime: null
          };
          updateTask(event.taskId, updatedTask);
          setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
        }
      }

      setEvents(events.filter(e => e.id !== eventId));
    } catch (err) {
      console.error("Erreur lors de la suppression de l'événement:", err);
      setError("Impossible de supprimer l'événement");
    }
  };

  const handleMoveEvent = async (eventId, newDay, newHour) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event) return;

      const task = tasks.find(t => t.id === event.taskId);
      if (!task) return;

      const startTime = new Date(newDay);
      startTime.setHours(newHour, 0, 0, 0);
      
      const endTime = new Date(startTime);
      endTime.setMinutes(endTime.getMinutes() + (task.durationMinutes || 60));

      const updatedEvent = {
        ...event,
        startTime: startTime.toISOString(),
        endTime: endTime.toISOString(),
        day: newDay.toISOString().split('T')[0],
        hour: newHour
      };

      const updatedTask = {
        ...task,
        scheduledTime: startTime.toISOString()
      };

      await updateTask(event.taskId, updatedTask);
      
      setEvents(events.map(e => e.id === eventId ? updatedEvent : e));
      setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
      
    } catch (err) {
      console.error("Erreur lors du déplacement de l'événement:", err);
      setError("Impossible de déplacer l'événement");
    }
  };

  if (loading) {
    return (
      <div className="schedule-page">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Chargement de votre emploi du temps...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="schedule-page">
        <div className="error-container">
          <div className="error-icon">⚠️</div>
          <h2>Oups !</h2>
          <p>{error}</p>
          <button 
            className="btn-retry"
            onClick={() => window.location.reload()}
          >
            Réessayer
          </button>
        </div>
      </div>
    );
  }

  const unscheduledTasks = tasks.filter(t => !t.scheduledTime && !t.completed);
  const completedTasks = tasks.filter(t => t.completed);

  return (
    <div className="schedule-page">
      {currentUser && (
        <div className="schedule-welcome">
          <h1>Bonjour, {currentUser.username} 👋</h1>
          <p className="welcome-subtitle">
            Organisez votre emploi du temps de manière intelligente
          </p>
        </div>
      )}

      <div className="schedule-content">
        <aside className="schedule-sidebar">
          <TodoList
            tasks={unscheduledTasks}
            completedTasks={completedTasks}
            onAddTask={handleAddTask}
            onToggleTask={handleToggleTask}
            onDeleteTask={handleDeleteTask}
          />
        </aside>

        <main className="schedule-main">
          <Calendar
            events={events}
            onDropTask={handleDropTaskOnCalendar}
            onDeleteEvent={handleDeleteEvent}
            onMoveEvent={handleMoveEvent}
          />
        </main>
      </div>

      {error && (
        <div className="notification notification-error">
          <span className="notification-icon">⚠️</span>
          <span className="notification-message">{error}</span>
          <button 
            className="notification-close"
            onClick={() => setError(null)}
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}

export default SchedulePage;