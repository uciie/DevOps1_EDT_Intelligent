import { useState, useEffect } from "react";
import TodoList from "../components/TodoList";
import Calendar from "../components/Calendar";
import api from "../api/api";
import "../styles/pages/SchedulePage.css";

// Page principale de gestion de l'emploi du temps
function SchedulePage() {
  const [tasks, setTasks] = useState([]);
  const [events, setEvents] = useState([]);
  const [currentUser, setCurrentUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Récupérer l'utilisateur depuis localStorage
    const storedUser = localStorage.getItem("currentUser");
    if (storedUser) {
      const user = JSON.parse(storedUser);
      setCurrentUser(user);
      loadUserData(user.id);
    } else {
      setLoading(false);
    }
  }, []);

  const loadUserData = async (userId) => {
    try {
      // Charger les tâches
      const tasksResponse = await api.get(`/tasks/user/${userId}`);
      setTasks(tasksResponse.data || []);

      // Charger les événements
      const eventsResponse = await api.get(`/events/user/${userId}`);
      setEvents(eventsResponse.data || []);
    } catch (error) {
      console.error("Erreur lors du chargement des données:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleAddTask = async (taskData) => {
    try {
      const response = await api.post("/tasks", {
        ...taskData,
        userId: currentUser.id,
        done: false,
      });
      setTasks([...tasks, response.data]);
    } catch (error) {
      console.error("Erreur lors de l'ajout de la tâche:", error);
      alert("Erreur lors de l'ajout de la tâche");
    }
  };

  const handleDeleteTask = async (taskId) => {
    try {
      await api.delete(`/tasks/${taskId}`);
      setTasks(tasks.filter((task) => task.id !== taskId));
    } catch (error) {
      console.error("Erreur lors de la suppression:", error);
    }
  };

  const handleToggleTask = async (taskId) => {
    try {
      const task = tasks.find((t) => t.id === taskId);
      const response = await api.put(`/tasks/${taskId}`, {
        ...task,
        done: !task.done,
      });
      setTasks(tasks.map((t) => (t.id === taskId ? response.data : t)));
    } catch (error) {
      console.error("Erreur lors de la mise à jour:", error);
    }
  };

  const handleDropTaskOnCalendar = async (task, timeSlot) => {
    try {
      // Créer un événement à partir de la tâche
      const eventData = {
        summary: task.title,
        startTime: timeSlot.start,
        endTime: timeSlot.end,
        userId: currentUser.id,
      };

      const response = await api.post("/events", eventData);
      setEvents([...events, response.data]);

      // Optionnel : marquer la tâche comme planifiée ou la supprimer
      // await handleDeleteTask(task.id);
    } catch (error) {
      console.error("Erreur lors de la création de l'événement:", error);
      alert("Erreur lors de l'ajout à l'emploi du temps");
    }
  };

  const handleDeleteEvent = async (eventId) => {
    try {
      await api.delete(`/events/${eventId}`);
      setEvents(events.filter((event) => event.id !== eventId));
    } catch (error) {
      console.error("Erreur lors de la suppression de l'événement:", error);
    }
  };

  const handleUpdateEvent = async (eventId, updatedData) => {
    try {
      const response = await api.put(`/events/${eventId}`, updatedData);
      setEvents(events.map((e) => (e.id === eventId ? response.data : e)));
    } catch (error) {
      console.error("Erreur lors de la mise à jour de l'événement:", error);
    }
  };

  if (loading) {
    return (
      <div className="schedule-loading">
        <p>Chargement...</p>
      </div>
    );
  }

  if (!currentUser) {
    return (
      <div className="schedule-no-user">
        <h2>Veuillez vous connecter</h2>
        <p>Vous devez créer un compte ou vous connecter pour accéder à votre emploi du temps.</p>
      </div>
    );
  }

  return (
    <div className="schedule-page">
      <header className="schedule-header">
        <h1>Gestion d'Emploi du Temps</h1>
        <p className="user-info">
          Bienvenue, <strong>{currentUser.username}</strong>
        </p>
      </header>

      <div className="schedule-container">
        <aside className="todo-sidebar">
          <TodoList
            tasks={tasks}
            onAddTask={handleAddTask}
            onDeleteTask={handleDeleteTask}
            onToggleTask={handleToggleTask}
          />
        </aside>

        <main className="calendar-main">
          <Calendar
            events={events}
            onDropTask={handleDropTaskOnCalendar}
            onDeleteEvent={handleDeleteEvent}
            onUpdateEvent={handleUpdateEvent}
          />
        </main>
      </div>
    </div>
  );
}

export default SchedulePage;