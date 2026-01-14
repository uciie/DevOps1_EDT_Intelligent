import React, { useState, useEffect } from 'react';
import Calendar from '../components/Calendar';
import TodoList from '../components/TodoList';
import EventForm from '../components/form/EventForm';
import Notification from '../components/Notification';
import { getCurrentUser } from '../api/authApi';
import { getUserId } from '../api/userApi';
import { 
    getUserTasks, getDelegatedTasks, createTask, 
    updateTask, deleteTask, planifyTask, reshuffleSchedule 
} from '../api/taskApi'; // Ajout de reshuffleSchedule ici
import { createEvent, getUserEvents, updateEvent, deleteEvent } from '../api/eventApi';
import { getMyTeams, createTeam, inviteUserToTeam, removeMemberFromTeam, deleteTeam } from '../api/teamApi';
import '../styles/pages/SchedulePage.css';

const normalizeData = (response) => {
    if (!response) return [];
    if (typeof response === 'string') {
        try { response = JSON.parse(response); } catch (e) { return []; }
    }
    if (Array.isArray(response)) return response;
    if (response.data && Array.isArray(response.data)) return response.data;
    if (response.content && Array.isArray(response.content)) return response.content;
    if (response.data?.content && Array.isArray(response.data.content)) return response.data.content;
    return [];
};

function SchedulePage() {
    // --- ÉTATS ---
    const [tasks, setTasks] = useState([]);
    const [events, setEvents] = useState([]);
    const [teams, setTeams] = useState([]);
    const [currentUser, setCurrentUser] = useState(null);
    const [selectedTeam, setSelectedTeam] = useState(null);
    const [loading, setLoading] = useState(true);
    const [pageError, setPageError] = useState(null);
    const [notification, setNotification] = useState(null);

    const [isEventFormOpen, setIsEventFormOpen] = useState(false);
    const [eventToEdit, setEventToEdit] = useState(null);
    const [selectedDate, setSelectedDate] = useState(null);
    const [selectedHour, setSelectedHour] = useState(9);
    
    const [showCreateTeam, setShowCreateTeam] = useState(false);
    const [newTeamName, setNewTeamName] = useState('');
    const [inviteUsername, setInviteUsername] = useState('');

    const showNotification = (message, type = 'success') => setNotification({ message, type });

    const formatEventForCalendar = (evt) => {
        if (!evt || !evt.startTime) return evt;
        const date = new Date(evt.startTime);
        return {
            ...evt,
            day: date.toISOString().split('T')[0],
            hour: date.getHours(),
            summary: evt.summary || evt.title || "Sans titre"
        };
    };

    // --- CHARGEMENT ---
    useEffect(() => {
        const loadData = async () => {
            try {
                setLoading(true);
                const user = getCurrentUser();
                if (!user) { setPageError("Non connecté"); return; }
                setCurrentUser(user);

                const [resT, resD, resE, resTm] = await Promise.all([
                    getUserTasks(user.id),
                    getDelegatedTasks(user.id),
                    getUserEvents(user.id),
                    getMyTeams(user.id).catch(() => [])
                ]);

                const allTasksMap = new Map();
                normalizeData(resT).forEach(t => allTasksMap.set(t.id, t));
                normalizeData(resD).forEach(t => allTasksMap.set(t.id, t));
                setTasks(Array.from(allTasksMap.values()));
                setEvents((Array.isArray(resE) ? resE : []).map(formatEventForCalendar));
                setTeams(normalizeData(resTm));
            } catch (err) {
                setPageError("Erreur de chargement");
            } finally {
                setLoading(false);
            }
        };
        loadData();
    }, []);

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
        } catch (err) {
            showNotification("Erreur lors de la réorganisation", "error");
        } finally {
            setLoading(false);
        }
    };

    // --- AUTRES HANDLERS ---
    const handleAddTask = async (data) => {
        try {
            const created = await createTask({ ...data, userId: currentUser.id });
            setTasks(prev => [...prev, created]);
            showNotification("Tâche ajoutée");
        } catch (err) { showNotification("Erreur ajout", "error"); }
    };

    const handleDropTaskOnCalendar = async (taskId) => {
        try {
            const planned = await planifyTask(taskId, null, null);
            setTasks(prev => prev.map(t => t.id === taskId ? planned : t));
            if (planned.event) {
                setEvents(prev => [...prev, formatEventForCalendar(planned.event)]);
                showNotification("Planification réussie");
            }
        } catch (err) { showNotification("Erreur planification", "error"); }
    };

    const handleSaveEvent = async (eventData) => {
        try {
            const useMaps = JSON.parse(localStorage.getItem("useGoogleMaps") ?? "true");
            if (eventToEdit) {
                const updated = await updateEvent(eventToEdit.id, { ...eventToEdit, ...eventData }, useMaps);
                setEvents(prev => prev.map(e => e.id === eventToEdit.id ? formatEventForCalendar(updated) : e));
            } else {
                const created = await createEvent({ ...eventData, userId: currentUser.id }, useMaps);
                setEvents(prev => [...prev, formatEventForCalendar(created)]);
            }
            setIsEventFormOpen(false);
        } catch (err) { showNotification("Erreur sauvegarde", "error"); }
    };

    const handleDeleteEvent = async (id) => {
        if (!window.confirm("Supprimer ?")) return;
        try {
            await deleteEvent(id);
            setEvents(prev => prev.filter(e => e.id !== id));
        } catch (err) { showNotification("Erreur suppression", "error"); }
    };

    if (loading) return <div className="schedule-page"><div className="spinner"></div></div>;
    if (pageError) return <div className="schedule-page"><div className="error-container"><h2>{pageError}</h2></div></div>;

    return (
        <div className="schedule-page">
            {notification && <Notification message={notification.message} type={notification.type} onClose={() => setNotification(null)} />}
            
            <div className="schedule-welcome">
                <div>
                    <h1>Bonjour, {currentUser?.username} 👋</h1>
                    <p>{selectedTeam ? `Équipe : ${selectedTeam.name}` : "Espace personnel"}</p>
                </div>
                {/* BOUTON RESHUFFLE ICI */}
                <button className="btn-reshuffle" onClick={handleReshuffle} title="Optimiser l'agenda">
                    🔄 Réorganiser l'agenda
                </button>
            </div>

            <div className="schedule-content">
                <aside className="schedule-sidebar">
                    <TodoList tasks={tasks} onAddTask={handleAddTask} currentUser={currentUser} />
                </aside>

                <main className="schedule-main">
                    <Calendar 
                        events={events} 
                        onDropTask={handleDropTaskOnCalendar} 
                        onDeleteEvent={handleDeleteEvent}
                        onAddEventRequest={(d, h) => { setEventToEdit(null); setSelectedDate(d); setSelectedHour(h); setIsEventFormOpen(true); }}
                        onEditEvent={(e) => { setEventToEdit(e); setIsEventFormOpen(true); }}
                    />
                </main>

                <aside className="teams-sidebar">
                    <h3>Équipes <button onClick={() => setShowCreateTeam(!showCreateTeam)}>+</button></h3>
                    {showCreateTeam && (
                        <div className="create-team-box">
                            <input value={newTeamName} onChange={e => setNewTeamName(e.target.value)} placeholder="Nom..." />
                            <button onClick={async () => {
                                const team = await createTeam(currentUser.id, { name: newTeamName });
                                setTeams([...teams, team]);
                                setShowCreateTeam(false);
                            }}>OK</button>
                        </div>
                    )}
                    <ul className="teams-list">
                        <li className={!selectedTeam ? 'active' : ''} onClick={() => setSelectedTeam(null)}>👤 Personnel</li>
                        {teams.map(t => (
                            <li key={t.id} className={selectedTeam?.id === t.id ? 'active' : ''} onClick={() => setSelectedTeam(t)}>🛡️ {t.name}</li>
                        ))}
                    </ul>
                </aside>
            </div>

            {isEventFormOpen && (
                <EventForm 
                    isOpen={isEventFormOpen} onClose={() => setIsEventFormOpen(false)}
                    onSave={handleSaveEvent} eventToEdit={eventToEdit}
                    initialDate={selectedDate} initialHour={selectedHour}
                />
            )}
        </div>
    );
}

export default SchedulePage;