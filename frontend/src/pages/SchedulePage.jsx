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
import { syncGoogleCalendar } from '../api/syncApi';
import '../styles/pages/SchedulePage.css';
import ChatAssistant from '../components/ChatAssistant';

// Helper pour normaliser les données (gérer content, data ou array direct)
const normalizeData = (response) => {
    // 0. Sécurité : si null/undefined
    if (!response) return [];

    // 1. Cas : Parfois Axios ou le backend renvoie une string JSON si le contenu est complexe
    if (typeof response === 'string') {
        try {
            response = JSON.parse(response);
        } catch (e){
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

  // État pour indiquer si une synchronisation est en cours (pour désactiver le bouton et éviter les appels concurrents)
  const [isSyncing, setIsSyncing] = useState(false);

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
      summary: evt.summary || evt.title || "Sans titre",
      source: evt.source || 'LOCAL' // Important pour l'affichage conditionnel
    };
  };

  // Fonction extraite pour pouvoir être rappelée après une synchro
  const loadUserData = async (userOverride = null) => {
    const user = userOverride || currentUser;
    if (!user) return;

    try {
        setLoading(true);
        // 1. Tâches
        const [rawTasksResponse, rawDelegatedResponse] = await Promise.all([
          getUserTasks(user.id),
          getDelegatedTasks(user.id)
        ]);
        const myTasks = normalizeData(rawTasksResponse);
        const delegatedTasks = normalizeData(rawDelegatedResponse);
        const allTasksMap = new Map();
        myTasks.forEach(t => allTasksMap.set(t.id, t));
        delegatedTasks.forEach(t => allTasksMap.set(t.id, t));
        setTasks(Array.from(allTasksMap.values()));

        // 2. Événements
        const rawEventsData = await getUserEvents(user.id);
        const eventsArray = Array.isArray(rawEventsData) ? rawEventsData : [];
        const loadedEvents = eventsArray.map(evt => formatEventForCalendar(evt));
        setEvents(loadedEvents);

        // 3. Équipes (seulement au premier chargement si nécessaire)
        if(!teams.length) {
            try {
                const teamsResponse = await getMyTeams(user.id);
                setTeams(normalizeData(teamsResponse));
            } catch (teamErr) { setTeams([]); }
        }
    } catch (err) {
        console.error("Erreur loadUserData:", err);
        setPageError("Impossible de charger les données");
    } finally {
        setLoading(false);
    }
  };

  useEffect(() => {
    const init = async () => {
        const user = getCurrentUser();
        if (!user) {
            setPageError("Utilisateur non connecté");
            return;
        }
        setCurrentUser(user);
        await loadUserData(user);
    };
    init();
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
      } catch {
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
      } catch {
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
        showNotification("Membre retiré avec succès", "success");
    } catch {
        showNotification("Erreur suppression membre", "error");
    }
  };

  const handleDeleteTeam = async (teamId) => {
      if (!window.confirm("Voulez-vous vraiment supprimer cette équipe ?")) return;
      try {
          await deleteTeam(teamId, currentUser.id);
          setTeams(teams.filter(t => t.id !== teamId));
          if (selectedTeam?.id === teamId) setSelectedTeam(null);
          showNotification("Équipe supprimée avec succès", "success");
      } catch {
          showNotification("Erreur suppression équipe", "error");
      }
  };

  // --- GESTION TÂCHES ---

  const handleAddTask = async (taskInput) => {
    if (!currentUser) return;
    try {
      const newTaskPayload = {
        title: taskInput.title,
        description: taskInput.description || '',
        duration: taskInput.duration || 60,
        deadline: taskInput.deadline || null,
        priority: taskInput.priority || 'NORMAL',
        category: taskInput.category || 'PERSONAL',
        teamId: selectedTeam ? selectedTeam.id : null,
        location: taskInput.location || null,
        useGoogleMaps: getGoogleMapsPreference(),
      };
      const created = await createTask(newTaskPayload, currentUser.id);
      setTasks([...tasks, created]);
      showNotification("Tâche ajoutée avec succès", "success");
    } catch {
      showNotification("Erreur lors de l'ajout de la tâche", "error");
    }
  };

  const handleEditTask = async (taskId, updatedData) => {
    const taskToUpdate = tasks.find(t => t.id === taskId);
    if (!taskToUpdate) return;

    const updatedPayload = {
      ...taskToUpdate,
      ...updatedData,
      useGoogleMaps: getGoogleMapsPreference(),
    };

    try {
      await updateTask(taskId, updatedPayload, currentUser.id);
      setTasks(tasks.map(t => (t.id === taskId ? { ...t, ...updatedData } : t)));
      showNotification("Tâche modifiée avec succès", "success");
    } catch {
      showNotification("Erreur lors de la modification", "error");
    }
  };

  const handleToggleTask = async (taskId) => {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;

    try {
      const newStatus = task.status === 'COMPLETED' ? 'TODO' : 'COMPLETED';
      await updateTask(taskId, { ...task, status: newStatus }, currentUser.id);
      setTasks(tasks.map(t => (t.id === taskId ? { ...t, status: newStatus } : t)));
      showNotification("Tâche mise à jour", "success");
    } catch {
      showNotification("Erreur mise à jour", "error");
    }
  };

  const handleDeleteTask = async (taskId) => {
    if (!window.confirm("Voulez-vous vraiment supprimer cette tâche ?")) return;
    try {
      await deleteTask(taskId, currentUser.id);
      setTasks(tasks.filter(t => t.id !== taskId));
      setEvents(events.filter(evt => evt.taskId !== taskId));
      showNotification("Tâche supprimée avec succès", "success");
    } catch {
      showNotification("Erreur suppression", "error");
    }
  };

  const handleDropTaskOnCalendar = async (taskId, day, hour) => {
    const task = tasks.find(t => t.id === taskId);
    if (!task) return;

    const startTime = new Date(day);
    startTime.setHours(hour, 0, 0, 0);
    const duration = task.duration || 60;
    const endTime = new Date(startTime.getTime() + duration * 60 * 1000);

    try {
      const planified = await planifyTask(taskId, startTime.toISOString(), currentUser.id);
      const updatedTask = { ...task, scheduledTime: startTime.toISOString() };
      setTasks(tasks.map(t => t.id === taskId ? updatedTask : t));

      if (planified && planified.event) {
        const newEvent = formatEventForCalendar(planified.event);
        setEvents([...events.filter(e => e.taskId !== taskId), newEvent]);
        showNotification("Tâche planifiée avec succès", "success");
      }
    } catch {
      showNotification("Erreur lors de la planification", "error");
    }
  };

  // --- GESTION ÉVÉNEMENTS MANUELS ---

  const handleCellClick = (day, hour) => {
    setSelectedDate(day);
    setSelectedHour(hour);
    setIsEventFormOpen(true);
  };

  const handleOpenEditModal = (event) => {
      setEventToEdit(event);
      setIsEventFormOpen(true);
  };

  const handleSaveEvent = async (eventData) => {
    // On utilise directement ces valeurs au lieu de reconstruire depuis day/hour
    
    console.log("Données reçues de EventForm:", eventData);

    // Validation : s'assurer que startTime et endTime existent
    if (!eventData.startTime || !eventData.endTime) {
      showNotification("Dates de début et de fin requises", "error");
      console.error("startTime ou endTime manquant:", eventData);
      return;
    }

    // Ajouter userId au payload
    const payload = {
      ...eventData,
      userId: currentUser.id, 
    };

    try {
      if (eventToEdit) {
        // Modification
        const updated = await updateEvent(eventToEdit.id, payload);
        setEvents(events.map(evt => evt.id === updated.id ? formatEventForCalendar(updated) : evt));
        showNotification("Événement modifié avec succès", "success");
      } else {
        // Création - CORRECTION : On n'envoie plus userId comme 2ème paramètre
        const created = await createEvent(payload);
        setEvents([...events, formatEventForCalendar(created)]);
        showNotification("Événement créé avec succès", "success");
      }
      setIsEventFormOpen(false);
      setEventToEdit(null);
    } catch (error) {
      showNotification(eventToEdit ? "Erreur modification" : "Erreur création", "error");
    }
  };

  const handleDeleteEvent = async (eventId) => {
    if (!window.confirm("Voulez-vous vraiment supprimer cet événement ?")) return;
    try {
      await deleteEvent(eventId);
      setEvents(events.filter(e => e.id !== eventId));
      showNotification("Événement supprimé", "success");
    } catch {
      showNotification("Erreur suppression", "error");
    }
  };

  const handleMoveEvent = async (eventId, newDay, newHour) => {
    const event = events.find(e => e.id === eventId);
    if (!event || !event.taskId) return;

    const task = tasks.find(t => t.id === event.taskId);
    if (!task) return;

    try {
      const startTime = new Date(newDay);
      startTime.setHours(newHour, 0, 0, 0);
      const duration = task.duration || 60;
      const endTime = new Date(startTime.getTime() + duration * 60 * 1000);

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
    } catch {
      showNotification("Impossible de déplacer l'événement", "error");
    }
  };

  // --- ACTION RESHUFFLE ---
  const handleReshuffle = async () => {
    if (!currentUser) return;
    try {
      setLoading(true);
      await reshuffleSchedule(currentUser.id);
      // Rechargement des données après réorganisation
      const rawTasks = await getUserTasks(currentUser.id);
      const tasksArray = normalizeData(rawTasks);
      setTasks(tasksArray);

      const updatedEvents = tasksArray
          .filter(t => t.event) 
          .map(t => formatEventForCalendar(t.event));

      setEvents(updatedEvents);
      showNotification("Agenda réorganisé avec succès !", "success");
    } catch {
      showNotification("Erreur lors de la réorganisation", "error");
    } finally {
      setLoading(false);
    }
  };

  // --- GESTION SYNCHRO GOOGLE CALENDAR (CORRIGÉE) ---
  const handleSyncGoogle = async () => {
    if (!currentUser) {
      showNotification("Utilisateur non connecté", "error");
      return;
    }

    // Empêcher les appels concurrents
    if (isSyncing) {
      return;
    }

    try {
      setIsSyncing(true);
      showNotification("Synchronisation avec Google en cours...", "info");
      
      // Appel de l'API de synchronisation
      const result = await syncGoogleCalendar(currentUser.id);
      
      // Vérification du résultat
      if (result && result.success) {
        // RECHARGEMENT IMPORTANT : Récupérer les nouveaux événements importés
        await loadUserData(currentUser);
        
        showNotification(
          result.message || "Calendrier synchronisé avec succès !", 
          "success"
        );
      } else {
        // Erreur retournée par l'API
        showNotification(
          result?.message || "Échec de la synchronisation.", 
          "error"
        );
      }
    } catch (error) {
      console.error("[SYNC] Erreur de synchronisation:", error);
      
      // Gestion des erreurs spécifiques
      if (error.message) {
        showNotification(error.message, "error");
      } else {
        showNotification("Échec de la synchronisation. Vérifiez votre connexion Google.", "error");
      }
    } finally {
      setIsSyncing(false);
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
          <div className="action-buttons">
            <button className="btn-reshuffle" onClick={handleReshuffle} disabled={loading}>
              ⚡ Réorganiser
            </button>
            {/* Bouton Synchro corrigé avec état de chargement */}
            <button 
              className="btn-sync" 
              onClick={handleSyncGoogle} 
              disabled={isSyncing}
            >
              {isSyncing ? '🔄 Synchronisation...' : '🔄 Synchro Google'}
            </button>
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
            currentUser={currentUser}
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
      
      {/* Assistant Planificateur flottant */}
      <div className="chat-assistant-wrapper fixed bottom-6 right-6 z-50">
        <ChatAssistant />
      </div>
    </div>
  );
}

export default SchedulePage;