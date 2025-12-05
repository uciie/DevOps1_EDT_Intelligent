import { useState } from 'react';
import { useDrop } from 'react-dnd';
import { ITEM_TYPES } from './TodoList';
import '../styles/components/Calendar.css';

// Composant pour une cellule de calendrier qui peut recevoir des tâches
function CalendarCell({ day, hour, events, onDropTask, onDeleteEvent, onAddClick, onEditEvent }) {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: ITEM_TYPES.TASK,
    drop: (item) => {
      onDropTask(item.task.id, day, hour);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }), [day, hour]);

  const cellEvents = events.filter(event => {
    if (!event.day) return false;
    const eventDate = new Date(event.day);
    return (
      eventDate.toDateString() === day.toDateString() &&
      event.hour === hour
    );
  });

  return (
    <div
      ref={drop}
      className={`calendar-cell ${isOver ? 'drop-target' : ''}`}
    >
      {/* Zone pour cliquer et ajouter un événement */}
      <button
        className="ghost-button"
        onClick={() => onAddClick(day, hour)}
        title="Ajouter un événement"
      >
        <span className="ghost-icon">+</span>
      </button>

      {/* Affichage des événements existants par dessus le bouton */}
      {cellEvents.map((event) => (
        <div
          key={event.id}
          className="calendar-event"
          title="Modifier l'évènement"
          // Ajout du clic pour l'édition 
          onClick={(e) => {
            e.stopPropagation(); // Empêche de déclencher le onAddClick du parent (création)
            onEditEvent(event);  // Passe l'objet event complet au parent pour modification
          }}
          style={{ 
            borderLeftColor: event.color || '#3b82f6',
            backgroundColor: event.color ? `${event.color}15` : '#eeffff',
            cursor: 'pointer' // Indique que c'est cliquable
          }}
        >
          <div className="event-content">
            <div className="event-title">{event.summary || event.title || "Sans titre"}</div>
            <div className="event-time">
              {new Date(event.startTime).toLocaleTimeString('fr-FR', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </div>
          </div>
          <button
            className="btn-delete-event"
            onClick={(e) => {
              e.stopPropagation();
              onDeleteEvent(event.id);
            }}
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}

export default function Calendar({ events, onDropTask, onDeleteEvent, onAddEventRequest, onEditEvent }) {
  const [currentWeekStart, setCurrentWeekStart] = useState(() => {
    const today = new Date();
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1); // Lundi = début de semaine
    return new Date(today.setDate(diff));
  });

  // Générer les 7 jours de la semaine
  const getDaysOfWeek = () => {
    const days = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(currentWeekStart);
      day.setDate(currentWeekStart.getDate() + i);
      days.push(day);
    }
    return days;
  };

  // Récupère les jours de la semaine actuelle
  const days = getDaysOfWeek();

  // Calcule l'heure de début basée sur les événements de la semaine visible
  const startHour = (() => {
    // 1. On filtre pour ne garder que les événements de la semaine affichée
    const eventsInCurrentWeek = events.filter(event => {
      if (!event.startTime) return false;
      const eventDate = new Date(event.startTime);
      // On regarde si la date de l'événement correspond à un des jours affichés
      return days.some(day => day.toDateString() === eventDate.toDateString());
    });

    // 2. S'il n'y a aucun événement cette semaine, on commence à 8h par défaut
    if (eventsInCurrentWeek.length === 0) return 8;

    // 3. Sinon, on trouve l'heure minimale (minHour)
    const hoursInWeek = eventsInCurrentWeek.map(e => new Date(e.startTime).getHours());
    const minHour = Math.min(...hoursInWeek);

    // Optionnel : On peut ajouter une marge (padding) pour ne pas coller l'événement tout en haut
    return minHour; 
  })();

  // Génère les heures de 'startHour' jusqu'à 23h (fin de journée)
  const hours = Array.from({ length: 24 - startHour }, (_, i) => i + startHour);

  // Fonctions de navigation
  const goToPreviousWeek = () => {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(currentWeekStart.getDate() - 7);
    setCurrentWeekStart(newDate);
  };

  const goToNextWeek = () => {
    const newDate = new Date(currentWeekStart);
    newDate.setDate(currentWeekStart.getDate() + 7);
    setCurrentWeekStart(newDate);
  };

  const goToToday = () => {
    const today = new Date();
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    setCurrentWeekStart(new Date(today.setDate(diff)));
  };

  const formatWeekRange = () => {
    const start = days[0];
    const end = days[6];
    return `${start.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long' })} - ${end.toLocaleDateString('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' })}`;
  };

  const isToday = (date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  const dayNames = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  return (
    <div className="calendar-container">
      <div className="calendar-header">
        <div className="calendar-nav">
          <button className="btn-nav" onClick={goToPreviousWeek} aria-label="Semaine précédente">
            {'<'}
          </button>
          <button className="btn-today" onClick={goToToday}>
            Aujourd'hui
          </button>
          <button className="btn-nav" onClick={goToNextWeek} aria-label="Semaine suivante">
            {'>'}
          </button>
        </div>
        <div className="current-week">{formatWeekRange()}</div>
      </div>

      <div className="calendar-grid">
        {/* Colonne vide pour l'en-tête des heures */}
        <div className="day-header"></div>

        {/* En-têtes des jours */}
        {days.map((day, index) => (
          <div key={index} className={`day-header ${isToday(day) ? 'today' : ''}`}>
            <span className="day-name">{dayNames[index]}</span>
            <span className="day-date">{day.getDate()}</span>
          </div>
        ))}

        {/* Grille avec heures et cellules */}
        {hours.map((hour) => (
          <div key={`hour-${hour}`} style={{ display: 'contents' }}>
            <div className="time-label">{`${hour}:00`}</div>
            {days.map((day, dayIndex) => (
              <CalendarCell
                key={`cell-${hour}-${dayIndex}`}
                day={day}
                hour={hour}
                events={events}
                onDropTask={onDropTask}
                onDeleteEvent={onDeleteEvent}
                onAddClick={onAddEventRequest} // Callback pour création (cellule vide)
                onEditEvent={onEditEvent}      // Callback pour modification (event existant)
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}