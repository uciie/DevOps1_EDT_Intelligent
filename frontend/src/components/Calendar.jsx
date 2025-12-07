import { useState } from 'react';
import { useDrop } from 'react-dnd';
import { ITEM_TYPES } from './TodoList';
import '../styles/components/Calendar.css';

// Composant pour une cellule de calendrier
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
      <button
        className="ghost-button"
        onClick={() => onAddClick(day, hour)}
        title="Ajouter un événement"
      >
        <span className="ghost-icon">+</span>
      </button>

      {cellEvents.map((event) => {
        // --- CALCUL DE LA HAUTEUR (Dynamique JS obligatoire) ---
        const start = new Date(event.startTime);
        const end = event.endTime ? new Date(event.endTime) : new Date(start.getTime() + 60 * 60000);
        const durationMinutes = (end - start) / (1000 * 60);
        const heightPx = ((durationMinutes / 60) * 60) + 1; 

        return (
          <div
            key={event.id}
            className="calendar-event-block"
            title="Modifier l'évènement"
            onClick={(e) => {
              e.stopPropagation();
              onEditEvent(event);
            }}
            style={{ 
              // Hauteur dynamique calculée (+1px pour le chevauchement)
              height: `${heightPx}px`, 
              // ON CHANGE ICI : La couleur est appliquée à la bordure gauche
              borderLeftColor: event.color || '#3b82f6' 
            }}
          >
            <div className="event-block-title">
              {event.summary || event.title || "Sans titre"}
            </div>

            <div className="event-block-time">
              {start.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })} - {end.toLocaleTimeString('fr-FR', { hour: '2-digit', minute: '2-digit' })}
            </div>

            {event.location && (
               <div className="event-block-location">
                 {typeof event.location === 'object' 
                   ? (event.location.name || event.location.address || "") 
                   : event.location}
               </div>
            )}
            
            <button
              className="btn-delete-event-block"
              onClick={(e) => {
                e.stopPropagation();
                onDeleteEvent(event.id);
              }}
            >
              ×
            </button>
          </div>
        );
      })}
    </div>
  );
}

export default function Calendar({ events, onDropTask, onDeleteEvent, onAddEventRequest, onEditEvent }) {
  const [currentWeekStart, setCurrentWeekStart] = useState(() => {
    const today = new Date();
    const day = today.getDay();
    const diff = today.getDate() - day + (day === 0 ? -6 : 1);
    return new Date(today.setDate(diff));
  });

  const getDaysOfWeek = () => {
    const days = [];
    for (let i = 0; i < 7; i++) {
      const day = new Date(currentWeekStart);
      day.setDate(currentWeekStart.getDate() + i);
      days.push(day);
    }
    return days;
  };

  const days = getDaysOfWeek();

  const startHour = (() => {
    const eventsInCurrentWeek = events.filter(event => {
      if (!event.startTime) return false;
      const eventDate = new Date(event.startTime);
      return days.some(day => day.toDateString() === eventDate.toDateString());
    });

    if (eventsInCurrentWeek.length === 0) return 8;

    const hoursInWeek = eventsInCurrentWeek.map(e => new Date(e.startTime).getHours());
    return Math.min(...hoursInWeek);
  })();

  const hours = Array.from({ length: 24 - startHour }, (_, i) => i + startHour);

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
          <button className="btn-nav" onClick={goToPreviousWeek}>{'<'}</button>
          <button className="btn-today" onClick={goToToday}>Aujourd'hui</button>
          <button className="btn-nav" onClick={goToNextWeek}>{'>'}</button>
        </div>
        <div className="current-week">{formatWeekRange()}</div>
      </div>

      <div className="calendar-grid">
        <div className="day-header"></div>
        {days.map((day, index) => (
          <div key={index} className={`day-header ${isToday(day) ? 'today' : ''}`}>
            <span className="day-name">{dayNames[index]}</span>
            <span className="day-date">{day.getDate()}</span>
          </div>
        ))}

        {hours.map((hour) => (
          /* Utilisation de la classe CSS .calendar-hour-row au lieu de display: contents inline */
          <div key={`hour-${hour}`} className="calendar-hour-row">
            <div className="time-label">{`${hour}:00`}</div>
            {days.map((day, dayIndex) => (
              <CalendarCell
                key={`cell-${hour}-${dayIndex}`}
                day={day}
                hour={hour}
                events={events}
                onDropTask={onDropTask}
                onDeleteEvent={onDeleteEvent}
                onAddClick={onAddEventRequest}
                onEditEvent={onEditEvent}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}