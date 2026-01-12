
import React, { useState, useEffect } from 'react';
import Calendar from '../components/Calendar';
import TodoList from '../components/TodoList';
import EventForm from '../components/form/EventForm';
import Notification from '../components/Notification';
import { getCurrentUser } from '../api/authApi';
import { getUserId} from '../api/userApi';
import { getUserTasks, getDelegatedTasks, createTask, updateTask, deleteTask, planifyTask } from '../api/taskApi';
import { createEvent, getUserEvents, updateEvent, deleteEvent } from '../api/eventApi';
import { getMyTeams, createTeam, inviteUserToTeam, removeMemberFromTeam, deleteTeam } from '../api/teamApi';
import '../styles/pages/SchedulePage.css';

// Helper pour normaliser les données (gérer content, data ou array direct)
const normalizeData = (response) => {
    // 0. Sécurité : si null/undefined
    if (!response) return [];

    // 1. Cas : Parfois Axios ou le backend renvoie une string JSON si le contenu est complexe
    if (typeof response === 'string') {
        try {
            response = JSON.parse(response);
        } catch (e) {
            console.error("Erreur parsing JSON dans normalizeData:", e);
            return [];
        }
    }

    // 2. Cas : C'est un tableau pur
    if (Array.isArray(response)) {
        return response;
    }

    // 3. Cas : Axios (response.data)
    if (response.data && Array.isArray(response.data)) {
        return response.data;
    }

    // 4. Cas : Spring Boot Pageable ({ content: [...] })
    if (response.content && Array.isArray(response.content)) {
        return response.content;
    }

    // 5. Cas : Wrapper complexe
    if (response.data && response.data.content && Array.isArray(response.data.content)) {
        return response.data.content;
    }
    
    console.warn("Format de données non reconnu par normalizeData:", response);
    return [];
};

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

  //État pour stocker l'événement à modifier ---
  const [eventToEdit, setEventToEdit] = useState(null);

  //États pour la Collaboration (Teams) ---
  const [teams, setTeams] = useState([]); // Initialisé comme tableau vide
  const [selectedTeam, setSelectedTeam] = useState(null); // Si null => Mode Personnel
  const [showCreateTeam, setShowCreateTeam] = useState(false);
  const [newTeamName, setNewTeamName] = useState('');
  const [inviteUsername, setInviteUsername] = useState('');


  // Helper pour afficher une notification
  const showNotification = (message, type = 'success') => {
    setNotification({ message, type });
  };

  // Helper pour récupérer la préférence de transport
  const getGoogleMapsPreference = () => {
    const pref = localStorage.getItem("useGoogleMaps");
    // Par défaut true si non défini
    return pref !== null ? JSON.parse(pref) : true;
  };

  // Ajoute les champs 'day' et 'hour' nécessaires au composant Calendar
  const formatEventForCalendar = (evt) => {
    if (!evt || !evt.startTime) return evt;
    const date = new Date(evt.startTime);
    return {
      ...evt,
      day: date.toISOString(), // Utilisé pour comparer les jours
      hour: date.getHours(),   // Utilisé pour placer dans la grille horaire
      // Assure qu'on a toujours un texte à afficher (Backend utilise 'summary', Task utilise 'title')
      summary: evt.summary || evt.title || "Sans titre"
    };
  };
}

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

        // --- 1. CHARGEMENT DES DONNÉES UTILISATEUR (Tâches + Events) ---
        // Note: Dans une version avancée, si selectedTeam est défini, 
        // on pourrait appeler des endpoints spécifiques à l'équipe.
        // Ici, on charge tout ce qui concerne l'user et on filtre côté client.
        
        //Chargement parallèle des tâches assignées ET déléguées
        const [rawTasksResponse, rawDelegatedResponse] = await Promise.all([
          getUserTasks(user.id),
          getDelegatedTasks(user.id)
        ]);

        const myTasks = normalizeData(rawTasksResponse);
        const delegatedTasks = normalizeData(rawDelegatedResponse);

        // Fusionner les listes en évitant les doublons (via Map par ID)
        const allTasksMap = new Map();
        myTasks.forEach(t => allTasksMap.set(t.id, t));
        delegatedTasks.forEach(t => allTasksMap.set(t.id, t));

        // Convertir la Map en tableau pour le state
        setTasks(Array.from(allTasksMap.values()));

        // --- 2. CHARGEMENT DES ÉVÉNEMENTS ---
        const rawEventsData = await getUserEvents(user.id);
        // On sécurise aussi les events au cas où
        const eventsArray = Array.isArray(rawEventsData) ? rawEventsData : [];

        const loadedEvents = eventsArray.map(evt => {
          const startDate = new Date(evt.startTime);

          return {
            ...evt,
            title: evt.summary || evt.title || "Sans titre",
            day: startDate.toISOString().split('T')[0],
            hour: startDate.getHours(),
            taskId: evt.taskId || (evt.task ? evt.task.id : null)
          };
        });

        setEvents(loadedEvents);

        // --- CHARGEMENT DES ÉQUIPES ---
        try {
            const teamsResponse = await getMyTeams(user.id);
            //console.log("Équipes chargées :", teamsResponse);
            //Utilisation de normalizeData pour éviter l'erreur .map
            const myTeams = normalizeData(teamsResponse);
            //console.log("Équipes normalisées :", myTeams);
            setTeams(myTeams);
        } catch (teamErr) {
            console.warn("Impossible de charger les équipes", teamErr);
            setTeams([]); // En cas d'erreur, on assure un tableau vide
        }

      } catch (err) {
        console.error("Erreur lors du chargement des données:", err);
        setPageError("Impossible de charger vos données");
      } finally {
        setLoading(false);
      }
    };

    loadUserData();
  }, []);
  //Fonction de suppression d'un membre
  const handleRemoveMember = async (teamId, memberId) => {
    if (!window.confirm("Voulez-vous vraiment retirer ce membre ?")) return;
    
    try {
        await removeMemberFromTeam(teamId, memberId, currentUser.id);
        
        // Mise à jour locale de l'état pour éviter un rechargement complet
        const updatedTeams = teams.map(t => {
            if (t.id === teamId) {
                // On filtre la liste des membres
                return {
                    ...t,
                    members: t.members ? t.members.filter(m => m.id !== memberId) : []
                };
            }
            return t;
        });
        
        setTeams(updatedTeams);
        
        // Si l'équipe modifiée est celle actuellement affichée, on met à jour selectedTeam
        if (selectedTeam && selectedTeam.id === teamId) {
            setSelectedTeam(updatedTeams.find(t => t.id === teamId));
        }
        
        showNotification("Membre retiré avec succès.", "success");
    } catch (error) {
        console.error("Erreur suppression membre:", error);
        // On affiche le message d'erreur du backend si dispo (ex: "Seul le chef...")
        const msg = error.response?.data || "Impossible de retirer le membre.";
        showNotification(msg, "error");
    }
  };
  // --- GESTION DES ÉQUIPES ---
  const handleCreateTeam = async () => {
      if(!newTeamName.trim()) return;
      try {
          const newTeam = await createTeam(currentUser.id, { 
              name: newTeamName, 
              description: "Groupe créé via le frontend" 
          });
          // Sécurité : s'assurer que teams est bien un tableau avant le spread
          setTeams([...(Array.isArray(teams) ? teams : []), newTeam]);
          setNewTeamName('');
          setShowCreateTeam(false);
          showNotification(`Équipe "${newTeam.name}" créée !`, "success");
      } catch (error) {
          showNotification("Erreur création équipe", "error");
      }
  };

  const handleInviteMember = async (teamId) => {
      if(!inviteUsername.trim()) return;
      try {
          const userMemberId = await getUserId(inviteUsername);
          await inviteUserToTeam(teamId, userMemberId, currentUser.id);
          showNotification(`Invitation envoyée à ${inviteUsername}`, "success");
          setInviteUsername('');
          
      } catch (error) {
          showNotification("Utilisateur introuvable ou erreur serveur", "error");
      }
  };


  // --- GESTION DES TÂCHES ---

  const handleAddTask = async (taskData) => {
    try {
      // taskData contient déjà { title, priority, duration, assignee, team } venant de TodoList
      const newTask = {
        ...taskData,
        userId: currentUser.id,
        completed: false,
        scheduledTime: null
      };

      console.log("Création de la tâche avec les données :", newTask);
      
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
      const savedTask = await updateTask(taskId, taskToUpdate);
      
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
      // Récupération de la préférence depuis le localStorage
      const useGoogleMaps = getGoogleMapsPreference();

      if (eventToEdit) {
        // --- LOGIQUE DE MODIFICATION ---
        const eventId = eventToEdit.id;
        
        // Fusionner l'ancien événement avec les nouvelles données du formulaire
        const updatedEventPayload = {
          ...eventToEdit,
          ...eventData, 
        };

        // On passe la préférence via l'API (qui a été mise à jour)
        const savedEvent = await updateEvent(eventId, updatedEventPayload, useGoogleMaps);
        
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
        
        // On passe la préférence
        const createdEvent = await createEvent(newEventPayload, useGoogleMaps);
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
      // On utilise aussi la préférence stockée pour l'édition rapide
      const useGoogleMaps = getGoogleMapsPreference();
      const savedEvent = await updateEvent(eventId, updatedEvent, useGoogleMaps);
      
      setEvents(events.map(e => e.id === eventId ? savedEvent : e));
      showNotification("Événement modifié", "success");
    } catch (err) {
      console.error("Erreur lors de la modification de l'événement:", err);
      showNotification("Impossible de modifier l'événement", "error");
    }
  };

const handleReshuffle = async () => {
    if (!currentUser) return;

  const handleDropTaskOnCalendar = async (taskId, day, hour) => {
    try {
      setLoading(true);
      // Supprimé : setError(null) -> Pas besoin car on utilise les notifications

      // 1. Appel du Backend pour réorganiser
      await reshuffleSchedule(currentUser.id);

      // 2. Recharger les données
      const rawTasksData = await getUserTasks(currentUser.id);
      
      // Normalisation des tâches (on reprend la logique de votre useEffect)
      let tasksArray = [];
      if (Array.isArray(rawTasksData)) {
          tasksArray = rawTasksData;
      } else if (rawTasksData?.data) {
          tasksArray = rawTasksData.data;
      } else if (rawTasksData?.content) {
          tasksArray = rawTasksData.content;
      // First-Fit logique backend
      const plannedTask = await planifyTask(taskId, null, null); 

      setTasks(tasks.map(t => t.id === taskId ? plannedTask : t));

      if (!plannedTask.event) {
          throw new Error("Le service de planification n'a pas retourné l'événement créé.");
      }
      
      setTasks(tasksArray);

      // 3. Mettre à jour les événements
      const updatedEvents = tasksArray
          .filter(t => t.event) 
          .map(t => ({
              id: t.event.id,
              taskId: t.id, 
              title: t.title,
              startTime: t.event.startTime, 
              endTime: t.event.endTime,
              priority: t.priority,
              day: new Date(t.event.startTime).toISOString().split('T')[0],
              hour: new Date(t.event.startTime).getHours()
          }));

      setEvents(updatedEvents);
      
      // Ajout du nouvel événement au calendrier
      setEvents([...events, newEvent]);
      showNotification("Tâche planifiée automatiquement !", "success");
      
    } catch (err) {
      console.error("Erreur lors de la planification automatique de la tâche:", err);
      showNotification("Impossible de planifier la tâche automatiquement", "error");
    }
  };

  // --- GESTION DES ÉVÉNEMENTS ---

  const handleCellClick = (day, hour) => {
      setEventToEdit(null); // Mode création : pas d'événement à éditer
      setSelectedDate(day);
      setSelectedHour(hour);
      setIsEventFormOpen(true);
    };

  const handleOpenEditModal = (event) => {
      setEventToEdit(event); // Mode édition : on stocke l'event
      setSelectedDate(null); // Pas besoin, l'event a déjà ses dates
      setIsEventFormOpen(true);
  };

  const handleSaveEvent = async (eventData) => {
    try {
      const useGoogleMaps = getGoogleMapsPreference();

      if (eventToEdit) {
        const eventId = eventToEdit.id;
        const updatedEventPayload = {
          ...eventToEdit,
          ...eventData,
        };

        const savedEvent = await updateEvent(eventId, updatedEventPayload, useGoogleMaps);
        const formattedEvent = formatEventForCalendar(savedEvent);
        formattedEvent.color = eventData.color || eventToEdit.color;

        setEvents(events.map(e => e.id === eventId ? formattedEvent : e));
        showNotification("Événement modifié !", "success");

      } else {
        const newEventPayload = {
          ...eventData,
          userId: currentUser.id,
          // Si on est dans un contexte d'équipe, on pourrait lier l'event à l'équipe ici aussi
        };

        const createdEvent = await createEvent(newEventPayload, useGoogleMaps);
        const formattedEvent = formatEventForCalendar(createdEvent);
        formattedEvent.color = eventData.color;

        setEvents(prev => [...prev, formattedEvent]);
        showNotification("Événement créé avec succès !", "success");
      }

      setIsEventFormOpen(false);
      setEventToEdit(null);

    } catch (error) {
      console.error("Erreur lors de la sauvegarde de l'événement:", error);
      if (error.response && error.response.data) {
        showNotification(error.response.data, "error");
      } else {
        showNotification("Impossible de sauvegarder l'événement (Erreur inconnue)", "error");
      }
    }
  };

  const handleDeleteEvent = async (eventId) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event) return;

      await deleteEvent(eventId);

      if (event.taskId) {
        const task = tasks.find(t => t.id === event.taskId);
        if (task) {
          const updatedTask = {
            ...task,
            scheduledTime: null
          };
          await updateTask(event.taskId, updatedTask);
          setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
        }
      }

      setEvents(events.filter(e => e.id !== eventId));
      showNotification("Événement supprimé", "success");

    } catch (err) {
      console.error("Erreur lors du réagencement:", err);
      // Correction ici : on utilise showNotification au lieu de setError
      showNotification("Impossible de réorganiser l'emploi du temps.", "error");
    } finally {
      setLoading(false);
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
  
  // --- GESTION SUPPRESSION ÉQUIPE ---
  const handleDeleteTeam = async (teamId) => {
    if (!window.confirm("Êtes-vous sûr de vouloir supprimer cette équipe définitivement ?")) return;

    try {
        await deleteTeam(teamId, currentUser.id);
        
        // Mise à jour locale
        const updatedTeams = teams.filter(t => t.id !== teamId);
        setTeams(updatedTeams);
        
        // Si on était sur cette équipe, on revient sur "Personnel"
        if (selectedTeam && selectedTeam.id === teamId) {
            setSelectedTeam(null);
        }
        
        showNotification("Équipe supprimée.", "success");
    } catch (error) {
        console.error(error);
        
        const errorData = error.response?.data;
        let errorMessage = "Erreur suppression équipe";

        if (typeof errorData === 'string') {
            errorMessage = errorData;
        } else if (errorData && errorData.message) {
            errorMessage = errorData.message; // Spring Boot met souvent le message ici
        }

        showNotification(errorMessage, "error");
    }
  };
  // Utilisation de pageError pour les erreurs bloquantes
  if (pageError) {
      return (
          <div className="schedule-page">
              <div className="error-container">
                  <h2>Oups !</h2>
                  <p>{pageError}</p>
                  <button onClick={() => window.location.reload()}>Réessayer</button>
              </div>
          </div>
      );
  }

  if (loading) {
    return (
      <div className="schedule-page">
        <div className="loading-container"><div className="spinner"></div></div>
      </div>
    );
  }


  return (
    <div className="schedule-page">
      {currentUser && (
        <div className="schedule-welcome">
          <div className="welcome-text">
            <h1>Bonjour, {currentUser.username} 👋</h1>
            <p className="welcome-subtitle">
                {selectedTeam ? `Espace de travail : ${selectedTeam.name}` : "Votre espace personnel"}
            </p>
          </div>
        </div>
      )}

      <div className="schedule-content">
        <aside className="schedule-sidebar">
          <TodoList
            tasks={tasks} 
            onAddTask={handleAddTask}
            onEditTask={handleEditTask}
            onToggleTask={handleToggleTask}
            onDeleteTask={handleDeleteTask}
            contextTeam={selectedTeam}
            currentUser={currentUser}
          />
        </aside>

        <main className="schedule-main">
          <Calendar
            events={events} 
            onDropTask={handleDropTaskOnCalendar}
            onDeleteEvent={handleDeleteEvent}
            onMoveEvent={handleMoveEvent}
            onAddEventRequest={handleCellClick}
            onEditEvent={handleOpenEditModal}
            contextTeam={selectedTeam} 
          />
        </main>

        <aside className="teams-sidebar">
            <div className="teams-header">
                <h3>👥 Équipes</h3>
                <button className="btn-add-team" onClick={() => setShowCreateTeam(!showCreateTeam)} title="Créer une équipe">+</button>
            </div>

            {showCreateTeam && (
                <div className="create-team-box">
                    <input type="text" placeholder="Nom équipe..." value={newTeamName} onChange={e => setNewTeamName(e.target.value)} />
                    <button onClick={handleCreateTeam}>OK</button>
                </div>
            )}

            <ul className="teams-list">
                <li className={`team-item ${!selectedTeam ? 'active' : ''}`} onClick={() => setSelectedTeam(null)}>
                    <span className="team-icon">👤</span> Personnel
                </li>

                {Array.isArray(teams) && teams.map(team => (
                    <li key={team.id} className={`team-item ${selectedTeam?.id === team.id ? 'active' : ''}`}>
                        <div className="team-info" onClick={() => setSelectedTeam(team)}>
                            <div style={{ display: 'flex', alignItems: 'center', flex: 1 }}>
                                <span className="team-icon">🛡️</span> 
                                <span className="team-name">{team.name}</span>
                            </div>

                            {/* BOUTON SUPPRIMER L'ÉQUIPE (Visible uniquement pour le chef) */}
                            {currentUser.id === team.ownerId && (
                                <button 
                                    className="btn-delete-team"
                                    onClick={(e) => {
                                        e.stopPropagation(); // Empêche la sélection de l'équipe au clic
                                        handleDeleteTeam(team.id);
                                    }}
                                    title="Supprimer l'équipe"
                                    style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '1rem' }}
                                >
                                    🗑️
                                </button>
                            )}
                        </div>
                        
                        {/* AFFICHER LA LISTE DES MEMBRES SI L'ÉQUIPE EST SÉLECTIONNÉE */}
                        {selectedTeam?.id === team.id && (
                            <div className="team-details-expanded">
                                <div className="invite-box">
                                    <input 
                                        type="text" 
                                        placeholder="Inviter (username)"
                                        value={inviteUsername}
                                        onChange={e => setInviteUsername(e.target.value)}
                                    />
                                    <button onClick={() => handleInviteMember(team.id)}>Inviter</button>
                                </div>
                                
                                <div className="members-list-container">
                                    <h5>Membres :</h5>
                                    <ul className="members-list">
                                        {team.members && team.members.length > 0 ? (
                                            team.members.map(member => (
                                                <li key={member.id} className="member-row">
                                                    <span className="member-name">
                                                        {member.username} 
                                                        {member.id === team.ownerId && " 👑"}
                                                    </span>
                                                    
                                                    {/* BOUTON SUPPRIMER : Visible seulement si je suis le chef et que ce n'est pas moi */}
                                                    {currentUser.id === team.ownerId && member.id !== currentUser.id && (
                                                        <button 
                                                            className="btn-remove-member"
                                                            onClick={(e) => {
                                                                e.stopPropagation(); // Évite de re-cliquer sur l'équipe
                                                                handleRemoveMember(team.id, member.id);
                                                            }}
                                                            title="Retirer ce membre"
                                                        >
                                                            ❌
                                                        </button>
                                                    )}
                                                </li>
                                            ))
                                        ) : (
                                            <li>Aucun membre</li>
                                        )}
                                    </ul>
                                </div>
                            </div>
                        )}
                    </li>
                ))}
            </ul>
        </aside>
      </div>

      <EventForm
        isOpen={isEventFormOpen}
        onClose={() => { setIsEventFormOpen(false); setEventToEdit(null); }}
        onSave={handleSaveEvent}
        initialDate={selectedDate}
        initialHour={selectedHour}
        initialData={eventToEdit}
      />

      <Notification message={notification?.message} type={notification?.type} onClose={() => setNotification(null)} />
    </div>
  );
}

export default SchedulePage;