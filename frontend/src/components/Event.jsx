import { useState, useRef, useEffect } from 'react';
import '../styles/components/Event.css';

export function Event({ events, onAddEvent }) {
  const [showForm, setShowForm] = useState(false);
  const addressInputRef = useRef(null); // Référence pour le champ adresse
  const autoCompleteRef = useRef(null); // Référence pour l'autocomplétion Google Maps

  // State correct
  const [newEvent, setNewEvent] = useState({
    summary: '',
    location: '', // Champ pour l'adresse
    latitude: null,  
    longitude: null, 
    startTime: '',
    endTime: '' // Note: Habituellement on calcule la fin via début + durée
  });

  // Fonction pour charger le script Google Maps dynamiquement
  const loadGoogleMapsScript = (callback) => {
    const existingScript = document.getElementById('googleMapsScript');
    if (existingScript) {
      if (window.google) callback();
      return;
    }

    const script = document.createElement('script');
    // Utilisation de la variable d'environnement VITE_
    const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY; 
    
    script.src = `https://maps.googleapis.com/maps/api/js?key=${apiKey}&libraries=places&loading=async`;
    script.id = 'googleMapsScript';
    script.async = true;
    script.defer = true;
    script.onload = () => {
      if (callback) callback();
    };
    document.head.appendChild(script);
  };

  // Initialisation de l'autocomplétion Google Maps
  // Initialisation de l'autocomplétion Google Maps
  useEffect(() => {
    if (showForm) {
      loadGoogleMapsScript(async () => { // 1. On rend le callback ASYNC
        if (!addressInputRef.current || !window.google) return;

        try {
          // 2. CORRECTION : On utilise importLibrary pour charger "places" proprement
          const { Autocomplete } = await window.google.maps.importLibrary("places");

          // 3. On utilise la classe importée directement (plus besoin de window.google.maps.places...)
          autoCompleteRef.current = new Autocomplete(addressInputRef.current, {
            types: ['geocode', 'establishment'],
            fields: ['formatted_address', 'geometry', 'name'],
          });

          autoCompleteRef.current.addListener('place_changed', () => {
            const place = autoCompleteRef.current.getPlace();
            
            if (place.geometry && place.geometry.location) {
              setNewEvent(prev => ({
                ...prev,
                location: place.formatted_address || place.name,
                latitude: place.geometry.location.lat(),
                longitude: place.geometry.location.lng()
              }));
            }
          });
        } catch (error) {
          console.error("Erreur lors du chargement de la librairie Places:", error);
        }
      });
    }
  }, [showForm]); // Se déclenche quand le formulaire s'affiche

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (newEvent.summary.trim()) {
      try {
        // Appel de la fonction reçue en prop
        await onAddEvent({
          summary: newEvent.summary,
          // Envoi complet de la LocationRequest au backend
          location: newEvent.location ? { 
            address: newEvent.location,
            latitude: newEvent.latitude,
            longitude: newEvent.longitude,
            name: newEvent.summary // On peut utiliser le titre de l'event comme nom de lieu par défaut
          } : null,
          durationMinutes: newEvent.durationMinutes,
          startTime: newEvent.startTime, 
          endTime: newEvent.endTime
        });
        
        // Réinitialisation
        setNewEvent({
          summary: '',
          location: '',
          latitude: null,
          longitude: null,
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
          {/* Champ de saisie pour le lieu */}
          <input
            ref={addressInputRef} // Liaison avec la ref
            type="text"
            placeholder="Lieu (adresse)..."
            value={newEvent.location}
            onChange={(e) => setNewEvent({ 
              ...newEvent, 
              location: e.target.value,
              // Si l'utilisateur modifie manuellement, on reset les coordonnées
              // pour forcer le backend à recalculer si besoin, ou on garde les anciennes
              latitude: null, 
              longitude: null 
            })}
            style={{ marginBottom: '10px' }}
          />
          <div className="form-row">
            <label>
              Commencer à : 
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
        {events && events.length === 0 && !showForm && (
          <div className="empty-state">
            Aucun événement prévu.<br />
            Cliquez sur <strong>+</strong> pour en ajouter un.
          </div>
        )}
        
      </div>
    </div>
  );
}