import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './ConflictResolution.css';

const ConflictResolution = ({ userId, onConflictsResolved }) => {
  const [conflicts, setConflicts] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadConflicts();
  }, [userId]);

  const loadConflicts = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`http://localhost:8080/api/conflicts/user/${userId}`);
      setConflicts(response.data);
    } catch (error) {
      console.error('Erreur lors du chargement des conflits:', error);
    } finally {
      setLoading(false);
    }
  };

  const resolveConflict = async (conflictId, resolution) => {
    try {
      await axios.post(`http://localhost:8080/api/conflicts/${conflictId}/resolve`, {
        resolution: resolution
      });
      
      // Recharger les conflits
      await loadConflicts();
      
      if (onConflictsResolved) {
        onConflictsResolved();
      }
    } catch (error) {
      console.error('Erreur lors de la résolution du conflit:', error);
      alert('Erreur lors de la résolution du conflit');
    }
  };

  const formatDateTime = (dateTimeStr) => {
    const date = new Date(dateTimeStr);
    return date.toLocaleString('fr-FR', {
      dateStyle: 'short',
      timeStyle: 'short'
    });
  };

  if (loading) {
    return <div className="conflicts-loading">Chargement des conflits...</div>;
  }

  if (conflicts.length === 0) {
    return null; // Pas de conflits, ne rien afficher
  }

  return (
    <div className="conflicts-container">
      <div className="conflicts-header">
        <h2>⚠️ Conflits de synchronisation ({conflicts.length})</h2>
        <p>Des modifications ont été effectuées à la fois localement et sur Google Calendar</p>
      </div>

      <div className="conflicts-list">
        {conflicts.map((conflict) => (
          <div key={conflict.id} className="conflict-card">
            <div className="conflict-info">
              <h3>Événement : {conflict.localTitle || conflict.googleTitle}</h3>
              <p className="conflict-detected">
                Conflit détecté le {formatDateTime(conflict.detectedAt)}
              </p>
            </div>

            <div className="conflict-comparison">
              <div className="version local-version">
                <h4>📝 Version locale</h4>
                <div className="version-details">
                  <p><strong>Titre :</strong> {conflict.localTitle}</p>
                  <p><strong>Description :</strong> {conflict.localDescription || 'Aucune'}</p>
                  <p><strong>Début :</strong> {formatDateTime(conflict.localStartTime)}</p>
                  <p><strong>Fin :</strong> {formatDateTime(conflict.localEndTime)}</p>
                  {conflict.localLastModified && (
                    <p className="last-modified">
                      Modifié le {formatDateTime(conflict.localLastModified)}
                    </p>
                  )}
                </div>
                <button 
                  className="btn-resolve btn-keep-local"
                  onClick={() => resolveConflict(conflict.id, 'KEEP_LOCAL')}
                >
                  Garder cette version
                </button>
              </div>

              <div className="version-divider">
                <span>VS</span>
              </div>

              <div className="version google-version">
                <h4>📅 Version Google Calendar</h4>
                <div className="version-details">
                  <p><strong>Titre :</strong> {conflict.googleTitle}</p>
                  <p><strong>Description :</strong> {conflict.googleDescription || 'Aucune'}</p>
                  <p><strong>Début :</strong> {formatDateTime(conflict.googleStartTime)}</p>
                  <p><strong>Fin :</strong> {formatDateTime(conflict.googleEndTime)}</p>
                  {conflict.googleLastModified && (
                    <p className="last-modified">
                      Modifié le {formatDateTime(conflict.googleLastModified)}
                    </p>
                  )}
                </div>
                <button 
                  className="btn-resolve btn-keep-google"
                  onClick={() => resolveConflict(conflict.id, 'KEEP_GOOGLE')}
                >
                  Garder cette version
                </button>
              </div>
            </div>

            <div className="conflict-actions">
              <button 
                className="btn-cancel"
                onClick={() => resolveConflict(conflict.id, 'CANCELLED')}
              >
                Ignorer ce conflit (garder l'état actuel)
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default ConflictResolution;