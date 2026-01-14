import React, { useState, useEffect } from 'react';
import Calendar from '../components/Calendar';
import TodoList from '../components/TodoList';
import EventForm from '../components/form/EventForm';
import Notification from '../components/Notification';
import { getCurrentUser } from '../api/authApi';
import { getUserId} from '../api/userApi';
import { getUserTasks, getDelegatedTasks, createTask, updateTask, deleteTask, planifyTask, reshuffleSchedule } from '../api/taskApi';
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
      day: date.toISOString().split('T')[0], // Utilisé pour comparer les jours
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

        // --- 1. CHARGEMENT DES DONNÉES UTILISATEUR (Tâches + Events) ---
        
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
        const eventsArray = Array.isArray(rawEventsData) ? rawEventsData : [];

        const loadedEvents = eventsArray.map(evt => formatEventForCalendar(evt));
        setEvents(loadedEvents);

        // --- CHARGEMENT DES ÉQUIPES ---
        try {
            const teamsResponse = await getMyTeams(user.id);
            const myTeams = normalizeData(teamsResponse);
            setTeams(myTeams);
        } catch (teamErr) {
            console.warn("Impossible de charger les équipes", teamErr);
            setTeams([]); 
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

  // --- GESTION DES ÉQUIPES ---

  const handleCreateTeam = async () => {
      if(!newTeamName.trim()) return;
      try {
          const newTeam = await createTeam(currentUser.id, { 
              name: newTeamName, 
              description: "Groupe créé via le frontend" 
          });
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

  const handleRemoveMember = async (teamId, memberId) => {
    if (!window.confirm("Voulez-vous vraiment retirer ce membre ?")) return;
    try {
        await removeMemberFromTeam(teamId, memberId, currentUser.id);
        const updatedTeams = teams.map(t => {
            if (t.id === teamId) {
                return {
                    ...t,
                    members: t.members ? t.members.filter(m => m.id !== memberId) : []
                };
            }
            return t;
        });
        setTeams(updatedTeams);
        if (selectedTeam && selectedTeam.id === teamId) {
            setSelectedTeam(updatedTeams.find(t => t.id === teamId));
        }
        showNotification("Membre retiré avec succès.", "success");
    } catch (error) {
        const msg = error.response?.data || "Impossible de retirer le membre.";
        showNotification(msg, "error");
    }
  };

  const handleDeleteTeam = async (teamId) => {
    if (!window.confirm("Êtes-vous sûr de vouloir supprimer cette équipe définitivement ?")) return;
    try {
        await deleteTeam(teamId, currentUser.id);
        const updatedTeams = teams.filter(t => t.id !== teamId);
        setTeams(updatedTeams);
        if (selectedTeam && selectedTeam.id === teamId) {
            setSelectedTeam(null);
        }
        showNotification("Équipe supprimée.", "success");
    } catch (error) {
        showNotification(error.response?.data?.message || "Erreur suppression équipe", "error");
    }
  };


  // --- GESTION DES TÂCHES ---

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
      showNotification("Tâche ajoutée avec succès !", "success");
      return createdTask;
    } catch (err) {
      showNotification("Impossible d'ajouter la tâche", "error");
      throw err;
    }
  };

  const handleEditTask = async (taskId, editData) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return;
      const taskToUpdate = {
        ...task,
        title: editData.title,
        estimatedDuration: editData.durationMinutes, 
        duration: editData.durationMinutes,       
        durationMinutes: editData.durationMinutes,
        priority: editData.priority
      };
      const savedTask = await updateTask(taskId, taskToUpdate);
      if (savedTask) {
        setTasks(tasks.map(t => t.id === taskId ? savedTask : t));
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
      showNotification("Impossible de modifier la tâche", "error");
    }
  };

  const handleToggleTask = async (taskId) => {
    try {
      const task = tasks.find(t => t.id === taskId);
      if (!task) return;
      const updatedTask = { ...task, completed: !task.completed };
      console.log("Mise à jour de la tâche :", updatedTask);
      await updateTask(taskId, updatedTask, currentUser.id);
      setTasks(tasks.map(t => t.id === taskId ? updatedTask : t));
    } catch (err) {
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
      showNotification("Impossible de supprimer la tâche", "error");
    }
  };

  const handleDropTaskOnCalendar = async (taskId, day, hour) => {
    try {
      // 2. Appeler le service backend 'planifyTask' avec NULL pour déclencher la logique First-Fit.
      const plannedTask = await planifyTask(taskId, null, null); 

      // 3. Mettre à jour les états locaux avec la réponse du backend
      setTasks(tasks.map(t => t.id === taskId ? plannedTask : t));

      if (!plannedTask.event) {
          throw new Error("Le service de planification n'a pas retourné l'événement créé.");
      }
      
      const newEvent = formatEventForCalendar({
        id: plannedTask.event.id,
        taskId: plannedTask.id, 
        title: plannedTask.title,
        startTime: plannedTask.event.startTime, 
        endTime: plannedTask.event.endTime,
        priority: plannedTask.priority,
      });
      
      setEvents([...events, newEvent]);
      showNotification("Tâche planifiée automatiquement !", "success");
    } catch (err) {
      showNotification("Impossible de planifier la tâche automatiquement", "error");
    }
  };

  // --- GESTION DES ÉVÉNEMENTS ---

  const handleCellClick = (day, hour) => {
    setEventToEdit(null); // Mode création
    setSelectedDate(day);
    setSelectedHour(hour);
    setIsEventFormOpen(true);
  };

  const handleOpenEditModal = (event) => {
    setEventToEdit(event); // Mode édition
    setSelectedDate(null);
    setIsEventFormOpen(true);
  };

  const handleSaveEvent = async (eventData) => {
    try {
      const useGoogleMaps = getGoogleMapsPreference();
      if (eventToEdit) {
        const eventId = eventToEdit.id;
        const updatedEventPayload = { ...eventToEdit, ...eventData };
        const savedEvent = await updateEvent(eventId, updatedEventPayload, useGoogleMaps);
        const formattedEvent = formatEventForCalendar(savedEvent);
        formattedEvent.color = eventData.color || eventToEdit.color;
        setEvents(events.map(e => e.id === eventId ? formattedEvent : e));
        showNotification("Événement modifié !", "success");
      } else {
        const newEventPayload = { ...eventData, userId: currentUser.id };
        const createdEvent = await createEvent(newEventPayload, useGoogleMaps);
        const formattedEvent = formatEventForCalendar(createdEvent);
        formattedEvent.color = eventData.color;
        setEvents(prev => [...prev, formattedEvent]);
        showNotification("Événement créé avec succès !", "success");
      }
      setIsEventFormOpen(false);
      setEventToEdit(null);
    } catch (error) {
      const msg = error.response?.data || "Impossible de sauvegarder l'événement";
      showNotification(msg, "error");
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
          const updatedTask = { ...task, scheduledTime: null };
          await updateTask(event.taskId, updatedTask, currentUser.id);
          setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
        }
      }
      setEvents(events.filter(e => e.id !== eventId));
      showNotification("Événement supprimé", "success");
    } catch (err) {
      showNotification("Impossible de supprimer l'événement", "error");
    }
  };

  const handleMoveEvent = async (eventId, newDay, newHour) => {
    try {
      const event = events.find(e => e.id === eventId);
      if (!event || !event.taskId) return;
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
      const updatedTask = { ...task, scheduledTime: startTime.toISOString() };

      await updateTask(event.taskId, updatedTask, currentUser.id);
      setEvents(events.map(e => e.id === eventId ? updatedEvent : e));
      setTasks(tasks.map(t => t.id === event.taskId ? updatedTask : t));
    } catch (err) {
      showNotification("Impossible de déplacer l'événement", "error");
    }
  };

  const handleReshuffle = async () => {
    if (!currentUser) return;
    try {
      setLoading(true);
      await reshuffleSchedule(currentUser.id);
      const rawTasksData = await getUserTasks(currentUser.id);
      const tasksArray = normalizeData(rawTasksData);
      setTasks(tasksArray);

      const updatedEvents = tasksArray
          .filter(t => t.event) 
          .map(t => formatEventForCalendar({
              id: t.event.id,
              taskId: t.id, 
              title: t.title,
              startTime: t.event.startTime, 
              endTime: t.event.endTime,
              priority: t.priority
          }));
      setEvents(updatedEvents);
      showNotification("Emploi du temps réorganisé !", "success");
    } catch (err) {
      showNotification("Impossible de réorganiser l'emploi du temps.", "error");
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="schedule-page">
        <div className="loading-container"><div className="spinner"></div><p>Chargement...</p></div>
      </div>
    );
  }

  if (pageError) {
    return (
      <div className="schedule-page">
        <div className="error-container">
          <h2>Oups !</h2>
          <p>{pageError}</p>
          <button className="btn-retry" onClick={() => window.location.reload()}>Réessayer</button>
        </div>
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
          <button className="btn-reshuffle" onClick={handleReshuffle}>Réorganiser l'agenda ⚡</button>
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
                <button className="btn-add-team" onClick={() => setShowCreateTeam(!showCreateTeam)}>+</button>
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
                            {currentUser.id === team.ownerId && (
                                <button className="btn-delete-team" onClick={(e) => { e.stopPropagation(); handleDeleteTeam(team.id); }}>🗑️</button>
                            )}
                        </div>
                        {selectedTeam?.id === team.id && (
                            <div className="team-details-expanded">
                                <div className="invite-box">
                                    <input type="text" placeholder="Inviter..." value={inviteUsername} onChange={e => setInviteUsername(e.target.value)} />
                                    <button onClick={() => handleInviteMember(team.id)}>Inviter</button>
                                </div>
                                <ul className="members-list">
                                    {team.members?.map(member => (
                                        <li key={member.id} className="member-row">
                                            <span>{member.username} {member.id === team.ownerId && "👑"}</span>
                                            {currentUser.id === team.ownerId && member.id !== currentUser.id && (
                                                <button onClick={() => handleRemoveMember(team.id, member.id)}>❌</button>
                                            )}
                                        </li>
                                    ))}
                                </ul>
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