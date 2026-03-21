import React, { useState, useEffect } from 'react';
import axios from 'axios';

import '../styles/pages/EventPage.css'; 
import { useNavigate } from 'react-router-dom';
const EventPage = () => {
    const [events, setEvents] = useState([]);
    const [searchTerm, setSearchTerm] = useState('théâtre');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // 1. Fonction de récupération des données
    const fetchEvents = (query) => {
        setLoading(true);
        setError(null);
        
        const encodedQuery = encodeURIComponent(query);
        // On appelle ton API Spring Boot
        axios.get(`http://localhost:8080/api/events/idf/search?q=${encodedQuery}`)
            .then(response => {
                // IMPORTANT : axios utilise .data pour le corps de la réponse
                setEvents(response.data); 
                setLoading(false);
            })
            .catch(err => {
                console.error("Erreur API :", err);
                setError("Impossible de charger les événements. Vérifiez que le Backend est lancé.");
                setLoading(false);
            });
    };

    // 2. Chargement initial
    useEffect(() => {
        fetchEvents(searchTerm);
    }, []);

    // 3. Gestion du formulaire de recherche
    const handleSearch = (e) => {
        e.preventDefault();
        fetchEvents(searchTerm);
    };
    const navigate = useNavigate();
    const handleAddToCalendar = async (event) => {
    try {
        // Remplace 1 par ton système de récupération d'ID (ex: user.id)
        const userId = 1; 
        
        const response = await axios.post(`http://localhost:8080/api/events/idf/add?userId=${userId}`, event);
        
        if (response.status === 200) {
            alert(`Succès ! "${event.title}" est maintenant dans votre EDT.`);
            navigate('/schedule')
        }
    } catch (err) {
        console.error(err);
        alert("Erreur lors de l'ajout à l'agenda.");
    }
};

    return (
        <div className="event-page">
            <header className="event-header">
                <h1>Événements en Île-de-France</h1>
                <p>Découvrez les activités culturelles pour 2026</p>
                
                <form onSubmit={handleSearch} className="search-bar">
                    <input 
                        type="text" 
                        value={searchTerm} 
                        onChange={(e) => setSearchTerm(e.target.value)} 
                        placeholder="Ex: Jazz, Expo, Théâtre..."
                    />
                    <button type="submit">🔍 Rechercher</button>
                </form>
            </header>

            {/* Affichage des erreurs éventuelles */}
            {error && <div className="error-message">{error}</div>}

            {/* État de chargement */}
            {loading ? (
                <div className="loader">
                    <div className="spinner"></div>
                    <p>Recherche des meilleurs événements...</p>
                </div>
            ) : (
                <div className="event-grid">
                    {events.length > 0 ? (
                        events.map((event, index) => (
                            <div key={index} className="event-card">
                               <div className="card-image-container" style={{ backgroundColor: '#ddd', height: '180px' }}>
                                    <img 
                                        src={event.image || event.posterUrl || ''} 
                                        alt={event.title} 
                                        className="event-poster"
                                        onError={(e) => {
                                            // Si l'image de l'événement échoue, on cache l'élément img
                                            // et on laisse le fond gris du container
                                            e.target.style.display = 'none';
                                        }}
                                    />
                                    {(!event.image && !event.posterUrl) && (
                                        <div className="no-image-text">📅 Pas d'image</div>
                                    )}
                                </div>
                                
                                <div className="card-body">
                                    <span className="category-tag">Événement</span>
                                    <h3>{event.title}</h3>
                                    
                                    <div className="event-info">
                                        <p>📅 <strong>Date :</strong> {event.start ? new Date(event.start).toLocaleDateString('fr-FR') : 'À venir'}</p>
                                        <p>📍 <strong>Lieu :</strong> {event.location || 'Lieu non précisé'}</p>
                                    </div>

                                    <p className="description">
                                        {event.description ? (event.description.substring(0, 100) + "...") : "Pas de description disponible."}
                                    </p>
                                    
                                    <div className="card-footer">
                                        <a href={event.eventUrl} target="_blank" rel="noopener noreferrer" className="btn-visit">
                                            Détails & Billets
                                        </a>
                                        <button 
                                            className="btn-add-calendar" 
                                            onClick={() => handleAddToCalendar(event)}
                                        >
                                            📅 +
                                        </button>
                                    </div>
                                </div>
                            </div>
                        ))
                    ) : (
                        <div className="no-results">Aucun événement trouvé pour "{searchTerm}".</div>
                    )}
                </div>
            )}
        </div>
    );
};

export default EventPage;