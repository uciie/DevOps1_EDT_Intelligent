import React, { useState, useEffect } from 'react';
import Calendar from '../components/Calendar';
import TodoList from '../components/TodoList';
import EventForm from '../components/form/EventForm';
import Notification from '../components/Notification'; // Ajout de l'import
import { Event } from '../components/Event';
import { getCurrentUser } from '../api/authApi';
import { getUserTasks, createTask, updateTask, deleteTask, planifyTask } from '../api/taskApi';
import { createEvent, getUserEvents, updateEvent, deleteEvent} from '../api/eventApi'; 

import '../styles/pages/SchedulePage.css';

function SchedulePage() {
  const [tasks, setTasks] = useState([]);
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [currentUser, setCurrentUser] = useState(null);
  
  // Correction : Séparation des erreurs de page (blocantes) et des notifications (temporaires)
  const [pageError, setPageError] = useState(null);
  const [notification, setNotification] = useState(null); // { message, type: 'success'|'error' }

  // États pour la modale
  const [isEventFormOpen, setIsEventFormOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState(null);
  const [selectedHour, setSelectedHour] = useState(9); // Heure par défaut
  
  // --- NOUVEAU : État pour stocker l'événement à modifier ---
  const [eventToEdit, setEventToEdit] = useState(null);

  // Helper pour afficher une notification
  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
  };

  // Ajoute les champs 'day' et 'hour' nécessaires au composant Calendar
  const formatEventForCalendar = (evt) => {
    if (!evt || !evt.startTime) return evt;
    const date = new Date(evt.startTime);
    return {
      ...evt,
      day: date.toISOString(), // Utilisé pour comparer les jours
      hour: date.getHours(),   // Utilisé pour placer dans la grille horaire
      // Assure qu'on a toujours un texte à afficher (Backend utilise 'summary', Task utilise 'title')
      summary: evt.summary || evt.title || "Sans titre"
    };
  };

  useEffect(() => {
    const loadUserData = async () => {
      try {
        setLoading(true);
        setPageError(null);
        
        const user = getCurrentUser();
        if (!user) {
          setPageError("Utilisateur non connecté");
          return;
        }
        
        setCurrentUser(user);
        
        // --- 1. CHARGEMENT DES TÂCHES (Pour la TodoList à gauche) ---
        const rawTasksData = await getUserTasks(user.id);
        
        // Normalisation de la liste des tâches (gestion des formats Page, Data, Array)
        let tasksArray = [];
        if (Array.isArray(rawTasksData)) {
            tasksArray = rawTasksData;
        } else if (rawTasksData && Array.isArray(rawTasksData.data)) {
            tasksArray = rawTasksData.data;
        } else if (rawTasksData && Array.isArray(rawTasksData.content)) {
            tasksArray = rawTasksData.content;
        } else {
            tasksArray = [];
        }
        setTasks(tasksArray);
        
        // --- 2. CHARGEMENT DES ÉVÉNEMENTS (Pour le Calendrier) ---
        // C'est ici la correction majeure : on appelle directement la table 'event'
        const rawEventsData = await getUserEvents(user.id);
        
        const loadedEvents = (rawEventsData || []).map(evt => {
            // Sécurisation de la date
            const startDate = new Date(evt.startTime);
            
            return {
              ...evt, // On garde toutes les propriétés (id, color, location...)
              
              // Normalisation du titre (le backend envoie souvent 'summary')
              title: evt.summary || evt.title || "Sans titre",
              
              // Propriétés calculées requises par Calendar.jsx
              day: startDate.toISOString().split('T')[0], // format YYYY-MM-DD
              hour: startDate.getHours(),
              
              // On s'assure que taskId est présent (si l'event est lié à une tâche)
              taskId: evt.taskId || (evt.task ? evt.task.id : null)
            };
        });

        setEvents(loadedEvents);
        
      } catch (err) {
        console.error("Erreur lors du chargement des données:", err);
        setPageError("Impossible de charger vos données");
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
      showNotification("Tâche ajoutée avec succès !", "success"); // Notification succès
      return createdTask;
    } catch (err) {
      console.error("Erreur lors de l'ajout de la tâche:", err);
      showNotification("Impossible d'ajouter la tâche", "error"); // Notification erreur
      throw err;
    }
  };

  const handleEditTask = async (taskId, editData) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return; // Tâche non trouvée

      // Préparation de l'objet à envoyer au backend
      const taskToUpdate = {
        ...task,
        title: editData.title,
        // IMPORTANT : On envoie la durée sous les deux noms possibles pour être sûr
        // que le Backend Java le reconnaisse (souvent 'duration' ou 'durationMinutes')
        estimatedDuration: editData.durationMinutes, 
        duration: editData.durationMinutes,       
        durationMinutes: editData.durationMinutes,
        priority: editData.priority
      };
      console.log('Task initiale:', task);
      console.log('Mise à jour de la tâche avec:', taskToUpdate);
      // 1. On attend la réponse du serveur (la tâche sauvegardée en BDD)
      const savedTask = await updateTask(taskId, taskToUpdate);
      console.log('Tâche sauvegardée par le serveur:', savedTask);
      // 2. On met à jour l'état local avec la version CONFIRMÉE par le serveur
      // Si savedTask est undefined (erreur api), l'affichage ne changera pas,
      // ce qui vous alertera qu'il y a un souci.
      if (savedTask) {
        setTasks(tasks.map(t => t.id === taskId ? savedTask : t));

        // Mettre à jour l'événement associé si la tâche est planifiée
        if (task.scheduledTime) {
          const relatedEvent = events.find(e => e.taskId === taskId);
          if (relatedEvent) {
            const startTime = new Date(task.scheduledTime);
            const endTime = new Date(startTime);
            endTime.setMinutes(endTime.getMinutes() + editData.durationMinutes);

            const updatedEvent = {
              ...relatedEvent,
              title: editData.title,
              endTime: endTime.toISOString(),
              priority: editData.priority
            };

            setEvents(events.map(e => e.id === relatedEvent.id ? updatedEvent : e));
          }
        }
        showNotification("Tâche modifiée !", "success");
      }
    } catch (err) {
      console.error("Erreur lors de la modification de la tâche:", err);
      showNotification("Impossible de modifier la tâche", "error");
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
      showNotification("Impossible de mettre à jour la tâche", "error");
    }
  };

  const handleDeleteTask = async (taskId) => {
    try {
      await deleteTask(taskId);
      setTasks(tasks.filter(t => t.id !== taskId));
      setEvents(events.filter(e => e.taskId !== taskId));
      showNotification("Tâche supprimée", "success");
    } catch (err) {
      console.error("Erreur lors de la suppression de la tâche:", err);
      showNotification("Impossible de supprimer la tâche", "error");
    }
  };

const handleDropTaskOnCalendar = async (taskId, day, hour) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return;

      // 1. Ne pas calculer startTime et endTime.
      // Le paramètre 'day' est souvent nécessaire pour le frontend pour savoir quel jour l'utilisateur regarde.
      // Cependant, nous allons ignorer l'heure et la date précises du drop pour le backend.
      
      // 2. Appeler le service backend 'planifyTask' avec NULL pour déclencher la logique First-Fit.
      // NOTE: Assurez-vous que planifyTask dans taskApi.js gère le passage de null (voir étape 1 ci-dessous).
      const plannedTask = await planifyTask(taskId, null, null); // <== CLÉ DE LA CORRECTION

      // 3. Mettre à jour les états locaux avec la réponse du backend
      
      // La Task mise à jour
      setTasks(tasks.map(t => t.id === taskId ? plannedTask : t));

      // L'Event créé (doit contenir les dates calculées par le First-Fit)
      if (!plannedTask.event) {
          throw new Error("Le service de planification n'a pas retourné l'événement créé.");
      }
      
      const newEvent = {
        id: plannedTask.event.id,
        taskId: plannedTask.id, 
        title: plannedTask.title,
        // Utiliser les heures calculées par le backend
        startTime: plannedTask.event.startTime, 
        endTime: plannedTask.event.endTime,
        priority: plannedTask.priority,
        // Calculer les propriétés 'day' et 'hour' à partir du résultat du backend pour le Calendar
        day: new Date(plannedTask.event.startTime).toISOString().split('T')[0],
        hour: new Date(plannedTask.event.startTime).getHours()
      };
      
      // Ajout du nouvel événement au calendrier
      setEvents([...events, newEvent]);
      showNotification("Tâche planifiée automatiquement !", "success");
      
    } catch (err) {
      console.error("Erreur lors de la planification automatique de la tâche:", err);
      showNotification("Impossible de planifier la tâche automatiquement", "error");
    }
  };

  // --- GESTION DES ÉVÉNEMENTS ---

  // --- LOGIQUE D'OUVERTURE DU FORMULAIRE ---

  // Cas 1: Clic dans une cellule vide (Création)
  const handleCellClick = (day, hour) => {
      setEventToEdit(null); // Mode création : pas d'événement à éditer
      setSelectedDate(day);
      setSelectedHour(hour);
      setIsEventFormOpen(true);
    };

  // Cas 2: Clic sur un événement existant (Modification)
  const handleOpenEditModal = (event) => {
      setEventToEdit(event); // Mode édition : on stocke l'event
      setSelectedDate(null); // Pas besoin, l'event a déjà ses dates
      setIsEventFormOpen(true);
  };

  // Callback pour sauvegarder depuis le formulaire
  const handleSaveEvent = async (eventData) => {
    try {
      if (eventToEdit) {
        // --- LOGIQUE DE MODIFICATION ---
        const eventId = eventToEdit.id;
        
        // Fusionner l'ancien événement avec les nouvelles données du formulaire
        const updatedEventPayload = {
          ...eventToEdit,
          ...eventData, 
        };

        const savedEvent = await updateEvent(eventId, updatedEventPayload);
        
        // Formater pour l'affichage calendrier
        const formattedEvent = formatEventForCalendar(savedEvent);
        formattedEvent.color = eventData.color || eventToEdit.color;

        // Mise à jour de la liste
        setEvents(events.map(e => e.id === eventId ? formattedEvent : e));
        showNotification("Événement modifié !", "success");

      } else {
        // --- LOGIQUE DE CRÉATION (Existante) ---
        // eventData vient de EventForm, contient déjà { summary, startTime, endTime, location, etc. }
        const newEventPayload = {
          ...eventData,
          userId: currentUser.id,
        };
        
        const createdEvent = await createEvent(newEventPayload);
        const formattedEvent = formatEventForCalendar(createdEvent);
        
        // On ajoute les infos de couleur pour l'affichage immédiat
        formattedEvent.color = eventData.color; 

        setEvents(prev => [...prev, formattedEvent]);
        showNotification("Événement créé avec succès !", "success");
      }

      // Fermeture et nettoyage
      setIsEventFormOpen(false);
      setEventToEdit(null);
      
    } catch (error) {
      console.error("Erreur lors de la sauvegarde de l'événement:", error);
      
      // On vérifie si le backend nous a envoyé un message spécifique (ex: 400 Bad Request)
      if (error.response && error.response.data) {
        // Affiche le message textuel renvoyé par le backend, "Impossible d'arriver à l'heure..."
        showNotification(error.response.data, "error"); 
      } else {
        // Fallback pour les autres erreurs (ex: serveur éteint)
        showNotification("Impossible de sauvegarder l'événement (Erreur inconnue)", "error");
      }
    }
  };

  const handleDeleteEvent = async (eventId) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event) return;

      // 2. Appel API pour supprimer l'événement en base de données
      await deleteEvent(eventId);

      // 3. Gestion de la tâche associée (si elle existe)
      if (event.taskId) {
        const task = tasks.find(t => t.id === event.taskId);
        if (task) {
          const updatedTask = {
            ...task,
            scheduledTime: null
          };
          // attendre la mise à jour de la tâche 
          await updateTask(event.taskId, updatedTask);
          setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
        }
      }

      // 4. Mise à jour de l'affichage (État local)
      setEvents(events.filter(e => e.id !== eventId));
      showNotification("Événement supprimé", "success");

    } catch (err) {
      console.error("Erreur lors de la suppression de l'événement:", err);
      showNotification("Impossible de supprimer l'événement", "error");
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
      showNotification("Impossible de déplacer l'événement", "error");
    }
  };

  const handleEditEvent = async (eventId, editData) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event) return;
      const updatedEvent = {
        ...event,
        ...editData
      };
      const savedEvent = await updateEvent(eventId, updatedEvent);
      setEvents(events.map(e => e.id === eventId ? savedEvent : e));
      showNotification("Événement modifié", "success");
    } catch (err) {
      console.error("Erreur lors de la modification de l'événement:", err);
      showNotification("Impossible de modifier l'événement", "error");
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

  // Utilisation de pageError pour les erreurs bloquantes
  if (pageError) {
    return (
      <div className="schedule-page">
        <div className="error-container">
          <div className="error-icon">⚠️</div>
          <h2>Oups !</h2>
          <p>{pageError}</p>
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
          {/* Conteneur pour le texte à gauche */}
          <div className="welcome-text">
            <h1>Bonjour, {currentUser.username} 👋</h1>
            <p className="welcome-subtitle">
              Organisez votre emploi du temps de manière intelligente
            </p>
          </div>

        </div>
      )}

      <div className="schedule-content">
        <aside className="schedule-sidebar">
          <TodoList
            tasks={unscheduledTasks}
            completedTasks={completedTasks}
            onAddTask={handleAddTask}
            onEditTask={handleEditTask}
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
            onAddEventRequest={handleCellClick} 
            // On passe la nouvelle fonction d'ouverture ici
            onEditEvent={handleOpenEditModal}
          />
        </main>
      </div>
      {/* La Modale */}
      <EventForm 
        isOpen={isEventFormOpen}
        onClose={() => {
            setIsEventFormOpen(false);
            setEventToEdit(null); // Reset de l'event à la fermeture
        }}
        onSave={handleSaveEvent}
        initialDate={selectedDate}
        initialHour={selectedHour}
        // IMPORTANT : On passe l'événement à modifier pour pré-remplir le formulaire
        initialData={eventToEdit} 
      />
      
      {/* Affichage des notifications (succès ou erreur d'action) */}
      <Notification 
        message={notification?.message} 
        type={notification?.type} 
        onClose={() => setNotification(null)} 
      />
    </div>
  );
}

export default SchedulePage;