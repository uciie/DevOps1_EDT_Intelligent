import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './SyncSettings.css';

const SyncSettings = ({ userId }) => {
  const [strategy, setStrategy] = useState('GOOGLE_PRIORITY');
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState(null);

  useEffect(() => {
    loadStrategy();
  }, [userId]);

  const loadStrategy = async () => {
    try {
      setLoading(true);
      const response = await axios.get(`http://localhost:8080/api/conflicts/user/${userId}/strategy`);
      setStrategy(response.data.strategy);
    } catch (error) {
      console.error('Erreur lors du chargement de la stratégie:', error);
    } finally {
      setLoading(false);
    }
  };

  const saveStrategy = async (newStrategy) => {
    try {
      setSaving(true);
      await axios.put(`http://localhost:8080/api/conflicts/user/${userId}/strategy`, {
        strategy: newStrategy
      });
      setStrategy(newStrategy);
      setMessage({ type: 'success', text: 'Stratégie mise à jour avec succès !' });
      setTimeout(() => setMessage(null), 3000);
    } catch (error) {
      console.error('Erreur lors de la sauvegarde:', error);
      setMessage({ type: 'error', text: 'Erreur lors de la mise à jour' });
      setTimeout(() => setMessage(null), 3000);
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return <div className="sync-settings-loading">Chargement...</div>;
  }

  return (
    <div className="sync-settings-container">
      <h3>⚙️ Paramètres de synchronisation</h3>
      <p className="sync-settings-description">
        Choisissez comment gérer les conflits lorsqu'un événement est modifié à la fois localement et sur Google Calendar
      </p>

      {message && (
        <div className={`sync-message ${message.type}`}>
          {message.text}
        </div>
      )}

      <div className="strategy-options">
        <div 
          className={`strategy-card ${strategy === 'GOOGLE_PRIORITY' ? 'selected' : ''}`}
          onClick={() => !saving && saveStrategy('GOOGLE_PRIORITY')}
        >
          <div className="strategy-icon">📅</div>
          <h4>Google Calendar prioritaire</h4>
          <p>En cas de conflit, la version de Google Calendar sera toujours conservée automatiquement.</p>
          <div className="strategy-badge">
            {strategy === 'GOOGLE_PRIORITY' && <span className="badge-active">✓ Activé</span>}
          </div>
        </div>

        <div 
          className={`strategy-card ${strategy === 'LOCAL_PRIORITY' ? 'selected' : ''}`}
          onClick={() => !saving && saveStrategy('LOCAL_PRIORITY')}
        >
          <div className="strategy-icon">💻</div>
          <h4>Version locale prioritaire</h4>
          <p>En cas de conflit, la version locale sera conservée et synchronisée vers Google Calendar.</p>
          <div className="strategy-badge">
            {strategy === 'LOCAL_PRIORITY' && <span className="badge-active">✓ Activé</span>}
          </div>
        </div>

        <div 
          className={`strategy-card ${strategy === 'ASK_USER' ? 'selected' : ''}`}
          onClick={() => !saving && saveStrategy('ASK_USER')}
        >
          <div className="strategy-icon">👤</div>
          <h4>Me demander à chaque fois</h4>
          <p>Vous serez notifié de chaque conflit et pourrez choisir quelle version conserver.</p>
          <div className="strategy-badge">
            {strategy === 'ASK_USER' && <span className="badge-active">✓ Activé</span>}
          </div>
          <div className="strategy-note">
            <strong>Recommandé</strong> pour un contrôle total
          </div>
        </div>
      </div>

      <div className="sync-info">
        <h4>  Information</h4>
        <ul>
          <li>La synchronisation automatique s'effectue toutes les 15 minutes</li>
          <li>Vous pouvez déclencher une synchronisation manuelle à tout moment</li>
          <li>Les conflits sont détectés uniquement si les deux versions ont été modifiées depuis la dernière synchronisation</li>
        </ul>
      </div>
    </div>
  );
};

export default SyncSettings;