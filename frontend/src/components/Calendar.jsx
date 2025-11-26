import { useState } from 'react';
import { useDrop } from 'react-dnd';
import { ITEM_TYPES } from './TodoList';
import '../styles/components/Calendar.css';

// Composant pour une cellule de calendrier qui peut recevoir des tâches
function CalendarCell({ day, hour, events, onDropTask, onDeleteEvent }) {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: ITEM_TYPES.TASK,
    drop: (item) => {
      onDropTask(item.task.id, day, hour);
    },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }), [day, hour]);

  // Filtrer les événements pour cette cellule
  const cellEvents = events.filter(event => {
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
      {cellEvents.map((event) => (
        <div
          key={event.id}
          className={`calendar-event priority-${event.priority}`}
        >
          <div className="event-content">
            <div className="event-title">{event.title}</div>
            <div className="event-time">
              {new Date(event.startTime).toLocaleTimeString('fr-FR', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })} - {new Date(event.endTime).toLocaleTimeString('fr-FR', { 
                hour: '2-digit', 
                minute: '2-digit' 
              })}
            </div>
          </div>
          <button
            className="btn-delete-event"
            onClick={() => onDeleteEvent(event.id)}
            aria-label="Supprimer l'événement"
          >
            ×
          </button>
        </div>
      ))}
    </div>
  );
}

export default function Calendar({ events, onDropTask, onDeleteEvent, onMoveEvent }) {
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

  // Heures de 8h à 20h
  const hours = Array.from({ length: 13 }, (_, i) => i + 8);

  const days = getDaysOfWeek();

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
            ‹
          </button>
          <button className="btn-today" onClick={goToToday}>
            Aujourd'hui
          </button>
          <button className="btn-nav" onClick={goToNextWeek} aria-label="Semaine suivante">
            ›
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
            {/* Label de l'heure */}
            <div className="time-label">
              {`${hour}:00`}
            </div>

            {/* Cellules pour chaque jour */}
            {days.map((day, dayIndex) => (
              <CalendarCell
                key={`cell-${hour}-${dayIndex}`}
                day={day}
                hour={hour}
                events={events}
                onDropTask={onDropTask}
                onDeleteEvent={onDeleteEvent}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}