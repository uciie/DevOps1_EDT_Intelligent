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
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  
  // --- ÉTATS DE SÉLECTION (BROUILLON) ---
  const [period, setPeriod] = useState('7days'); 
  const [draftCategories, setDraftCategories] = useState([]); 
  const [customStart, setCustomStart] = useState('');
  const [customEnd, setCustomEnd] = useState('');

  // --- ÉTAT APPLIQUÉ (POUR L'AFFICHAGE) ---
  const [appliedCategories, setAppliedCategories] = useState([]);
  
  const currentUser = getCurrentUser();
  const currentUserId = currentUser ? currentUser.id : null;

  // --- 1. FONCTION DE RÉCUPÉRATION (Extraite pour être réutilisée) ---
  const fetchData = async () => {
    // On calcule les dates ici (ou on les passe en arguments)
    const dates = getDatesFromPeriod();
    if (!dates) return;

    setLoading(true);
    setHasSearched(true);

    try {
      const data = await getActivityStats(currentUserId, dates.start, dates.end);
      setStats(data.sort((a, b) => b.totalMinutes - a.totalMinutes));
    } catch (error) {
      console.error("Erreur stats:", error);
    } finally {
      setLoading(false);
    }
  };

  // --- 2. CALCUL DES DATES ---
  const getDatesFromPeriod = () => {
    const end = new Date();
    let start = new Date();

    if (period === '7days') {
      start.setDate(end.getDate() - 7);
    } else if (period === 'month') {
      start.setMonth(end.getMonth() - 1);
    } else if (period === 'custom') {
      if (!customStart || !customEnd) {
        // Petit fix : on évite l'alert au chargement initial si c'est 'custom'
        if (loading) return null; 
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

  // --- 3. USE EFFECT (Chargement Initial) ---
  useEffect(() => {
    if (currentUserId) {
      fetchData(); // Appel de la fonction extraite
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentUserId]); // On ne dépend que de l'ID pour éviter la boucle

  // --- 4. GESTION DES FILTRES ---
  const toggleCategory = (catKey) => {
    setDraftCategories(prev => {
      if (prev.includes(catKey)) return prev.filter(k => k !== catKey);
      return [...prev, catKey];
    });
  };

  // --- 5. ACTION BOUTON RECHERCHER ---
  const handleSearch = () => {
    // On applique les catégories sélectionnées
    setAppliedCategories([...draftCategories]);
    // On relance la recherche (au cas où la période a changé)
    fetchData();
  };

  const formatDuration = (minutes) => {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h > 0) return `${h}h ${m}m`;
    return `${m} min`;
  };

  // Filtrage : Si aucune catégorie appliquée, on montre tout. Sinon on filtre.
  const displayedStats = appliedCategories.length === 0 
    ? stats 
    : stats.filter(s => appliedCategories.includes(s.category));

  return (
    <div className="activity-page container">
      <h1>📈 Analyse de vos activités</h1>
      
      <div className="filters-container">
        
        <div className="filters-grid-layout">
          {/* Période */}
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

          {/* Activités (Sélection multiple) */}
          <div className="filters-card">
            <h3>🏷️ Filtrer par activité</h3>
            <div className="categories-filter">
              {Object.entries(CATEGORY_LABELS).map(([key, info]) => {
                const isSelected = draftCategories.includes(key);
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
              {draftCategories.length > 0 && (
                <button className="category-chip clear" onClick={() => setDraftCategories([])}>✕</button>
              )}
            </div>
          </div>
        </div>

        {/* Bouton de Validation */}
        <div className="search-action">
          <button className="btn-search" onClick={handleSearch} disabled={loading}>
            {loading ? 'Chargement...' : '✅ Confirmer et Actualiser'}
          </button>
        </div>
      </div>

      {/* Résultats */}
      {loading ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Calcul des statistiques en cours...</p>
        </div>
      ) : (
        <div className="stats-grid">
          {displayedStats.map((stat) => {
            const info = CATEGORY_LABELS[stat.category] || { label: stat.category, color: '#ccc' };
            
            // Si le filtre est actif, on cache les catégories non sélectionnées
            // Si le filtre est vide (tout voir), on cache les catégories à 0 count pour ne pas polluer
            if (appliedCategories.length > 0 && !appliedCategories.includes(stat.category)) return null;
            if (stat.count === 0 && appliedCategories.length === 0) return null;

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
          
          {hasSearched && displayedStats.length === 0 && (
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