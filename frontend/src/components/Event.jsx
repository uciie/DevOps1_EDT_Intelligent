import { useState } from 'react';
import '../styles/components/Event.css';

// 1. Ajoutez les props ici : { events, onAddEvent }
export function Event({ events, onAddEvent }) {
  const [showForm, setShowForm] = useState(false);
  
  // State correct
  const [newEvent, setNewEvent] = useState({
    summary: '',
    startTime: '',
    endTime: '' // Note: Habituellement on calcule la fin via début + durée
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newEvent.summary.trim()) {
      try {
        // 2. Appel de la fonction reçue en prop
        await onAddEvent({
          summary: newEvent.summary,
          durationMinutes: newEvent.durationMinutes,
          startTime: newEvent.startTime, // Assurez-vous que votre API attend ça
          endTime: newEvent.endTime
        });
        
        // Réinitialisation
        setNewEvent({
          summary: '',
          durationMinutes: 60,
          startTime: '',
          endTime: ''
        });
        setShowForm(false);
      } catch (error) {
        console.error('Erreur lors de l\'ajout de l\'événement:', error);
      }
    }
  };

  const handleCancel = () => {
    setShowForm(false);
  };

  return (
    <div className="event">
      <div className="event-header">
        <h2>📅 Événements</h2>
        <button
          className="btn-add-task"
          onClick={() => setShowForm(!showForm)}
          aria-label="Ajouter un événement"
        >
          +
        </button>
      </div>

      {showForm && (
        <form className="task-form" onSubmit={handleSubmit}>
          <input
            type="text"
            placeholder="Titre de l'événement..."
            value={newEvent.summary}
            onChange={(e) => setNewEvent({ ...newEvent, summary: e.target.value })}
            autoFocus
            required
          />
          <div className="form-row">
            <label>
              Commencer à : 
              {/* 3. Correction: utilisation de newEvent au lieu de newTask */}
              <input
                type="datetime-local"
                min="5"
                max="480"
                step="5"
                value={newEvent.startTime}
                onChange={(e) => setNewEvent({ ...newEvent, startTime: e.target.value })}
              />
            </label>
            <label>
              Terminer à : 
              <input
                type="datetime-local"
                min="5"
                max="480"
                step="5"
                value={newEvent.endTime}
                onChange={(e) => setNewEvent({ ...newEvent, endTime: e.target.value })}
              />
            </label>
            {/* Vous pouvez ajouter ici un champ pour la date de début si nécessaire */}
          </div>
          <div className="form-actions">
            <button type="submit" className="btn-submit">
              ✓ Ajouter
            </button>
            <button type="button" className="btn-cancel" onClick={handleCancel}>
              Annuler
            </button>
          </div>
        </form>
      )}

      <div className="tasks-section">
        {/* 4. Utilisation de la prop events au lieu de tasks */}
        {events && events.length === 0 && !showForm && (
          <div className="empty-state">
            Aucun événement prévu.<br />
            Cliquez sur <strong>+</strong> pour en ajouter un.
          </div>
        )}
        
        {/* Liste des événements (optionnel, si vous voulez les lister ici) */}
        {events && events.length > 0 && (
            <ul className="event-list-preview">
                {events.slice(0, 5).map(evt => (
                    <li key={evt.id}>{evt.title || evt.summary}</li>
                ))}
            </ul>
        )}
      </div>
    </div>
  );
}