import { useState } from "react";
import "../styles/components/Calendar.css";

function Calendar({ events, onDropTask, onDeleteEvent, onUpdateEvent }) {
  const [currentDate, setCurrentDate] = useState(new Date());
  const [viewMode, setViewMode] = useState("week"); // week ou day

  // Générer les heures de 8h à 20h
  const hours = Array.from({ length: 13 }, (_, i) => i + 8);

  // Obtenir les jours de la semaine
  const getWeekDays = (date) => {
    const startOfWeek = new Date(date);
    const day = startOfWeek.getDay();
    const diff = startOfWeek.getDate() - day + (day === 0 ? -6 : 1);
    startOfWeek.setDate(diff);

    return Array.from({ length: 7 }, (_, i) => {
      const d = new Date(startOfWeek);
      d.setDate(startOfWeek.getDate() + i);
      return d;
    });
  };

  const weekDays = getWeekDays(currentDate);

  const formatDate = (date) => {
    return date.toLocaleDateString("fr-FR", {
      weekday: "short",
      day: "numeric",
      month: "short",
    });
  };

  const handleDrop = (e, dayIndex, hour) => {
    e.preventDefault();
    const taskData = e.dataTransfer.getData("task");
    if (!taskData) return;

    const task = JSON.parse(taskData);
    const day = weekDays[dayIndex];

    // Créer les dates de début et fin
    const start = new Date(day);
    start.setHours(hour, 0, 0, 0);

    const end = new Date(start);
    end.setMinutes(start.getMinutes() + task.estimatedDuration);

    onDropTask(task, {
      start: start.toISOString(),
      end: end.toISOString(),
    });

    // Retirer la classe de survol
    e.currentTarget.classList.remove("drop-target");
  };

  const handleDragOver = (e) => {
    e.preventDefault();
    e.currentTarget.classList.add("drop-target");
  };

  const handleDragLeave = (e) => {
    e.currentTarget.classList.remove("drop-target");
  };

  const getEventsForSlot = (dayIndex, hour) => {
    const day = weekDays[dayIndex];
    return events.filter((event) => {
      const eventStart = new Date(event.startTime);
      const isSameDay =
        eventStart.getDate() === day.getDate() &&
        eventStart.getMonth() === day.getMonth() &&
        eventStart.getFullYear() === day.getFullYear();
      const eventHour = eventStart.getHours();
      return isSameDay && eventHour === hour;
    });
  };

  const formatEventTime = (event) => {
    const start = new Date(event.startTime);
    const end = new Date(event.endTime);
    return `${start.getHours()}:${String(start.getMinutes()).padStart(2, "0")} - ${end.getHours()}:${String(end.getMinutes()).padStart(2, "0")}`;
  };

  const navigateWeek = (direction) => {
    const newDate = new Date(currentDate);
    newDate.setDate(currentDate.getDate() + direction * 7);
    setCurrentDate(newDate);
  };

  const goToToday = () => {
    setCurrentDate(new Date());
  };

  return (
    <div className="calendar">
      <div className="calendar-header">
        <div className="calendar-navigation">
          <button onClick={() => navigateWeek(-1)} className="nav-btn">
            ← Semaine précédente
          </button>
          <button onClick={goToToday} className="today-btn">
            Aujourd'hui
          </button>
          <button onClick={() => navigateWeek(1)} className="nav-btn">
            Semaine suivante →
          </button>
        </div>
        <h2 className="calendar-title">Emploi du temps</h2>
      </div>

      <div className="calendar-grid">
        {/* En-tête avec les jours */}
        <div className="time-column header-cell">Heure</div>
        {weekDays.map((day, index) => (
          <div key={index} className="day-header">
            <div className="day-name">{formatDate(day)}</div>
          </div>
        ))}

        {/* Grille horaire */}
        {hours.map((hour) => (
          <div key={hour} className="time-row">
            <div className="time-cell">{hour}:00</div>
            {weekDays.map((_, dayIndex) => {
              const slotEvents = getEventsForSlot(dayIndex, hour);
              return (
                <div
                  key={dayIndex}
                  className="calendar-slot"
                  onDrop={(e) => handleDrop(e, dayIndex, hour)}
                  onDragOver={handleDragOver}
                  onDragLeave={handleDragLeave}
                >
                  {slotEvents.map((event) => (
                    <div key={event.id} className="calendar-event">
                      <div className="event-content">
                        <div className="event-title">{event.summary}</div>
                        <div className="event-time">{formatEventTime(event)}</div>
                      </div>
                      <button
                        className="event-delete"
                        onClick={() => onDeleteEvent(event.id)}
                        title="Supprimer"
                      >
                        ✕
                      </button>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        ))}
      </div>
    </div>
  );
}

export default Calendar;