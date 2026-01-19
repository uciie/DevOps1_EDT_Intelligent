import { useState } from 'react';
import { useDrop } from 'react-dnd';
import { ITEM_TYPES } from './TodoList';
import '../styles/components/Calendar.css';

// Composant pour une cellule de calendrier
function CalendarCell({ day, hour, events, onDropTask, onDeleteEvent, onAddClick, onEditEvent, isReadOnly, currentUser }) {
  const [{ isOver }, drop] = useDrop(() => ({
    accept: ITEM_TYPES.TASK,
    // MODIFICATION ICI : On vérifie que ce n'est pas un calendrier tiers ET que la tâche appartient à l'utilisateur

  canDrop: (item) => {
    if (isReadOnly) return false;
    // Sécurité : si currentUser n'est pas encore chargé
    if (!currentUser || !currentUser.id) return false;
    // Note : on vérifie que item.task existe bien (suite à notre correction précédente)
    if (!item.task) return true;
    // On autorise le drop seulement si la tâche appartient à l'utilisateur actuel ou lui est assignée
    return item.task.assigneeId === currentUser.id || item.task.userId === currentUser.id;
  },
  drop: (item) => {
    // Ceci fonctionnera maintenant sans planter
    onDropTask(item.task.id, day, hour); 
  },
    collect: (monitor) => ({
      isOver: monitor.isOver(),
    }),
  }), [day, hour, isReadOnly]);

  const cellEvents = events.filter(event => {
    const eventDate = new Date(event.startTime);
    return eventDate.getDate() === day.getDate() && 
           eventDate.getMonth() === day.getMonth() &&
           eventDate.getFullYear() === day.getFullYear() &&
           eventDate.getHours() === hour;
  });

  // --- FONCTION DE SÉCURITÉ MISE À JOUR ---
  const renderLocation = (loc) => {
    if (!loc) return null;
    
    // Si lecture seule (calendrier tiers), on peut masquer le lieu pour plus de confidentialité
    if (isReadOnly) return null;

    // Si c'est un objet, on affiche uniquement l'adresse comme demandé
    if (typeof loc === 'object') {
      return loc.address || loc.name || loc.displayName || "Lieu inconnu";
    }
    return loc;
  };

  return (
    <div
      ref={drop}
      className={`calendar-cell ${isOver ? 'drag-over' : ''} ${isReadOnly ? 'readonly-cell' : ''}`}
    >
      {!isReadOnly && (
        <button 
          className="add-event-btn" 
          onClick={() => onAddClick(day, hour)}
          title="Ajouter un événement"
        >
          +
        </button>
      )}
      {cellEvents.map((event) => {
        const start = new Date(event.startTime);
        const end = new Date(event.endTime);
        const durationHours = Math.max(1, (end - start) / (1000 * 60 * 60));
        return (
          <div 
            key={event.id} 
            className="calendar-event"
            onClick={(e) => {
              e.stopPropagation();
              if (!isReadOnly) onEditEvent(event);
            }}
            style={{ 
                height: `calc(${durationHours} * 100% + ${Math.max(0, durationHours - 1)} * 1px)`,
                zIndex: 10 
              }}
            
          >
            <div className="event-summary">{event.summary}</div>
            <div className="event-time">
              {event.startTime?.substring(11, 16)} - {event.endTime?.substring(11, 16)}
            </div>
            {renderLocation(event.location) && (
              <div className="event-location">📍 {renderLocation(event.location)}</div>
            )}
            {!isReadOnly && (
              <button
                className="delete-event-btn"
                onClick={(e) => {
                  e.stopPropagation();
                  onDeleteEvent(event.id);
                }}
                title="Supprimer l'événement"
              >
                ×
              </button>
            )}
          </div>
        )
      })}
    </div>
  );
}

function Calendar({ events, onDropTask, onDeleteEvent, onAddEventRequest, onEditEvent, isReadOnly = false, currentUser}) {
  const [currentDate, setCurrentDate] = useState(new Date());

  const days = [];
  const startOfWeek = new Date(currentDate);
  startOfWeek.setDate(currentDate.getDate() - currentDate.getDay() + (currentDate.getDay() === 0 ? -6 : 1));

  for (let i = 0; i < 7; i++) {
    const day = new Date(startOfWeek);
    day.setDate(startOfWeek.getDate() + i);
    days.push(day);
  }

  const getDynamicHours = () => {
    // Filtrer les événements qui appartiennent à la semaine affichée
    const weekTimestamps = days.map(d => d.toDateString());
    const eventsInWeek = events.filter(event => {
      if (!event.day) return false;
      return weekTimestamps.includes(new Date(event.day).toDateString());
    });

    // Trouver l'heure la plus basse (minimum 8h par défaut, maximum 23h)
    let startHour = 8; 
    if (eventsInWeek.length > 0) {
      const minEventHour = Math.min(...eventsInWeek.map(e => e.hour));
      startHour = minEventHour;
    }

    // Créer le tableau d'heures de startHour jusqu'à 23h
    const dynamicHours = [];
    for (let h = startHour; h <= 23; h++) {
      dynamicHours.push(h);
    }
    return dynamicHours;
  };

  const hours = getDynamicHours();
  const dayNames = ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'];

  const goToPreviousWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() - 7);
    setCurrentDate(newDate);
  };

  const goToNextWeek = () => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + 7);
    setCurrentDate(newDate);
  };

  const goToToday = () => setCurrentDate(new Date());

  const formatWeekRange = () => {
    const options = { day: 'numeric', month: 'long' };
    return `Semaine du ${days[0].toLocaleDateString('fr-FR', options)} au ${days[6].toLocaleDateString('fr-FR', options)}`;
  };

  const isToday = (date) => {
    const today = new Date();
    return date.toDateString() === today.toDateString();
  };

  return (
    <div className={`calendar-container ${isReadOnly ? 'mode-readonly' : ''}`}>
      <div className="calendar-header">
        <div className="calendar-nav">
          <button className="btn-nav" onClick={goToPreviousWeek}>{'<'}</button>
          <button className="btn-today" onClick={goToToday}>Aujourd'hui</button>
          <button className="btn-nav" onClick={goToNextWeek}>{'>'}</button>
        </div>
        <div className="current-week">
            {formatWeekRange()} {isReadOnly && <span className="readonly-badge" style={{marginLeft: '10px', color: 'var(--primary-color)'}}>(Consultation)</span>}
        </div>
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
                isReadOnly={isReadOnly}
                currentUser={currentUser}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

export default Calendar;