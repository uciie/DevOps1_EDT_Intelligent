import React, { useState, useEffect } from 'react';
import { getCurrentUser } from '../api/authApi';
import { getActivityStats } from '../api/activityApi';
import '../styles/pages/ActivityPage.css';

const CATEGORY_LABELS = {
  'TRAVAIL': { label: 'Travail 💼', color: '#3b82f6' },
  'ETUDE': { label: 'Études 📚', color: '#8b5cf6' },
  'SPORT': { label: 'Sport 🏃', color: '#f59e0b' },
  'LOISIR': { label: 'Loisir 🎮', color: '#ec4899' },
  'MENAGER': { label: 'Ménager 🧹', color: '#10b981' },
  'RENCONTRE': { label: 'Rencontre 🤝', color: '#ef4444' },
  'AUTRE': { label: 'Autre ❓', color: '#64748b' }
};

export default function ActivityStatsPage() {
  const [stats, setStats] = useState([]);
  const [loading, setLoading] = useState(false); // État de chargement
  const [hasSearched, setHasSearched] = useState(false); // Pour savoir si on a déjà cherché
  
  // Filtres
  const [period, setPeriod] = useState('7days'); 
  const [selectedCategories, setSelectedCategories] = useState([]); 
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');
  
  const currentUser = getCurrentUser();

  // Chargement initial (optionnel, vous pouvez le retirer si vous voulez que la page soit vide au début)
  useEffect(() => {
    if (currentUser) {
      fetchStats();
    }
    // Note : On ne met PAS 'period' ou 'selectedCategories' ici pour éviter l'auto-refresh
  }, [currentUser]); 

  const toggleCategory = (catKey) => {
    setSelectedCategories(prev => {
      if (prev.includes(catKey)) return prev.filter(k => k !== catKey);
      return [...prev, catKey];
    });
  };

  const getDatesFromPeriod = () => {
    const end = new Date();
    let start = new Date();

    if (period === '7days') {
      start.setDate(end.getDate() - 7);
    } else if (period === 'month') {
      start.setMonth(end.getMonth() - 1);
    } else if (period === 'custom') {
      if (!customStart || !customEnd) {
        alert("Veuillez sélectionner une date de début et de fin.");
        return null;
      }
      return {
        start: new Date(customStart).toISOString(),
        end: new Date(customEnd + 'T23:59:59').toISOString()
      };
    }
    
    return {
      start: start.toISOString(),
      end: end.toISOString()
    };
  };

  const fetchStats = async () => {
    const dates = getDatesFromPeriod();
    if (!dates) return;

    setLoading(true);
    setHasSearched(true);
    try {
      // Appel API (Backend mis à jour avec le DTO)
      const data = await getActivityStats(currentUser.id, dates.start, dates.end);
      
      // Tri par durée totale décroissante
      setStats(data.sort((a, b) => b.totalMinutes - a.totalMinutes));
    } catch (error) {
      console.error("Erreur lors de la récupération des stats:", error);
    } finally {
      setLoading(false);
    }
  };

  const formatDuration = (minutes) => {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h > 0) return `${h}h ${m}m`;
    return `${m} min`;
  };

  // Filtrage côté client pour l'affichage
  const displayedStats = selectedCategories.length === 0 
    ? stats 
    : stats.filter(s => selectedCategories.includes(s.category));

  return (
    <div className="activity-page container">
      <h1>📈 Analyse de vos activités</h1>
      
      {/* --- Zone de Filtres --- */}
      <div className="filters-container">
        
        <div className="filters-grid-layout">
          {/* Colonne 1 : Période */}
          <div className="filters-card">
            <h3>📅 Période</h3>
            <div className="period-buttons">
              <button className={`filter-btn ${period === '7days' ? 'active' : ''}`} onClick={() => setPeriod('7days')}>7 jours</button>
              <button className={`filter-btn ${period === 'month' ? 'active' : ''}`} onClick={() => setPeriod('month')}>30 jours</button>
              <button className={`filter-btn ${period === 'custom' ? 'active' : ''}`} onClick={() => setPeriod('custom')}>Perso</button>
            </div>

            {period === 'custom' && (
              <div className="custom-dates">
                <input type="date" value={customStart} onChange={(e) => setCustomStart(e.target.value)} />
                <span className="date-separator">au</span>
                <input type="date" value={customEnd} onChange={(e) => setCustomEnd(e.target.value)} />
              </div>
            )}
          </div>

          {/* Colonne 2 : Activités */}
          <div className="filters-card">
            <h3>🏷️ Activités</h3>
            <div className="categories-filter">
              {Object.entries(CATEGORY_LABELS).map(([key, info]) => {
                const isSelected = selectedCategories.includes(key);
                return (
                  <button
                    key={key}
                    className={`category-chip ${isSelected ? 'selected' : ''}`}
                    onClick={() => toggleCategory(key)}
                    style={{
                      borderColor: info.color,
                      backgroundColor: isSelected ? info.color : 'transparent',
                      color: isSelected ? 'white' : 'var(--neutral-700)'
                    }}
                  >
                    {info.label}
                  </button>
                );
              })}
              {selectedCategories.length > 0 && (
                <button className="category-chip clear" onClick={() => setSelectedCategories([])}>✕</button>
              )}
            </div>
          </div>
        </div>

        {/* --- BOUTON DE VALIDATION --- */}
        <div className="search-action">
          <button className="btn-search" onClick={fetchStats} disabled={loading}>
            {loading ? 'Chargement...' : '🔍 Afficher les statistiques'}
          </button>
        </div>
      </div>

      {/* --- Grille des Résultats --- */}
      {loading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Calcul des statistiques en cours...</p>
        </div>
      ) : (
        <div className="stats-grid">
          {displayedStats.map((stat) => {
            const info = CATEGORY_LABELS[stat.category] || { label: stat.category, color: '#ccc' };
            
            // On cache si vide ET qu'on n'a pas spécifiquement demandé cette catégorie
            if (stat.count === 0 && !selectedCategories.includes(stat.category)) return null;

            return (
              <div key={stat.category} className="stat-card" style={{ borderTop: `4px solid ${info.color}` }}>
                <h3>{info.label}</h3>
                
                <div className="stat-row">
                  <span className="stat-label">Temps total</span>
                  <span className="stat-value big">{formatDuration(stat.totalMinutes)}</span>
                </div>
                
                <div className="stat-details">
                  <div className="detail-item">
                    <span className="detail-val">{stat.count}</span>
                    <span className="detail-lbl">Sessions</span>
                  </div>
                  <div className="detail-item separator"></div>
                  <div className="detail-item">
                    <span className="detail-val">{formatDuration(stat.averageMinutes)}</span>
                    <span className="detail-lbl">Moyenne</span>
                  </div>
                </div>
              </div>
            );
          })}
          
          {hasSearched && displayedStats.every(s => s.count === 0) && (
            <div className="no-data">
              <span className="no-data-icon">📭</span>
              <p>Aucune activité trouvée pour cette période.</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}