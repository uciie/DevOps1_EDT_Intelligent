import { useState, useEffect, useRef } from 'react';
import { useLoadScript } from '@react-google-maps/api';
import '../styles/components/Event.css';

const libraries = ["places"];

export default function GooglePlacesAutocomplete({ value, onChange, placeholder = "Entrez une adresse..." }) {
  const [inputValue, setInputValue] = useState(value || '');
  const [predictions, setPredictions] = useState([]);
  const [showSuggestions, setShowSuggestions] = useState(false);

  // Référence uniquement pour l'autocomplétion (les détails passeront par la classe Place directement)
  const autocompleteService = useRef(null);
  const containerRef = useRef(null);

  const apiKey = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || '';

  const { isLoaded } = useLoadScript({
    googleMapsApiKey: apiKey,
    libraries,
  });

  // Initialisation du service d'autocomplétion uniquement
  useEffect(() => {
    if (isLoaded && !autocompleteService.current && window.google) {
      autocompleteService.current = new window.google.maps.places.AutocompleteService();
    }
  }, [isLoaded]);

  // Sync si la prop value change
  useEffect(() => {
    setInputValue(value || '');
  }, [value]);

  // Gestion du clic en dehors pour fermer la liste
  useEffect(() => {
    const handleClickOutside = (event) => {
      if (containerRef.current && !containerRef.current.contains(event.target)) {
        setShowSuggestions(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleInputChange = (e) => {
    const val = e.target.value;
    setInputValue(val);
    onChange(val); // Remonte la valeur texte brute au parent

    if (!val) {
      setPredictions([]);
      return;
    }

    if (autocompleteService.current) {
      autocompleteService.current.getPlacePredictions(
        { input: val}, // 'geocode' pour les adresses
        (results, status) => {
          if (status === window.google.maps.places.PlacesServiceStatus.OK && results) {
            setPredictions(results);
            setShowSuggestions(true);
          } else {
            setPredictions([]);
          }
        }
      );
    }
  };

  // Gestion de la sélection d'une suggestion
  const handleSelectPrediction = async (prediction) => {
    const address = prediction.structured_formatting.main_text + ", " + prediction.structured_formatting.secondary_text;
    
    setInputValue(address);
    setShowSuggestions(false);

    if (window.google && window.google.maps && window.google.maps.places) {
      try {
        // 1. Instanciation de la nouvelle classe Place avec l'ID
        const place = new window.google.maps.places.Place({
          id: prediction.place_id,
        });

        // 2. Récupération des champs souhaités (notez le camelCase : location, formattedAddress)
        // L'API attend 'location' pour lat/lng et 'formattedAddress' pour l'adresse complète.
        await place.fetchFields({
          fields: ['location', 'formattedAddress'],
        });

        // 3. Utilisation des données (plus besoin de callback)
        const location = place.location; // Objet LatLng
        
        if (location) {
          // On passe l'objet complet au parent
          // Note : .lat() et .lng() sont des fonctions sur l'objet location
          onChange({
             address: place.formattedAddress || address,
             lat: typeof location.lat === 'function' ? location.lat() : location.lat,
             lng: typeof location.lng === 'function' ? location.lng() : location.lng
          });
        } else {
          // Fallback si pas de location
          onChange(address);
        }

      } catch (error) {
        console.error("Erreur lors de la récupération des détails du lieu (API Place) :", error);
        // En cas d'erreur, on renvoie au moins l'adresse texte
        onChange(address);
      }
    } else {
      onChange(address);
    }
  };

  if (!apiKey) return <div className="error-text">Clé API manquante</div>;
  if (!isLoaded) return <div className="loading-text">Chargement...</div>;

  return (
    <div className="places-autocomplete-container" ref={containerRef}>
      <input
        type="text"
        value={inputValue}
        onChange={handleInputChange}
        onFocus={() => inputValue && setShowSuggestions(true)}
        placeholder={placeholder}
        className="input-field"
        autoComplete="off"
      />

      {showSuggestions && predictions.length > 0 && (
        <ul className="places-suggestions-list">
          {predictions.map((prediction) => (
            <li 
              key={prediction.place_id} 
              onClick={() => handleSelectPrediction(prediction)}
              className="places-suggestion-item"
            >
              <span className="places-main-text">
                {prediction.structured_formatting.main_text}
              </span>
              <span className="places-secondary-text">
                {", " + prediction.structured_formatting.secondary_text}
              </span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}