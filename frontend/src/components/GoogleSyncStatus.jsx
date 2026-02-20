import React, { useState, useEffect } from 'react';
import { checkGoogleCalendarStatus } from '../api/syncApi';
import '../styles/components/GoogleSyncStatus.css';

/**
 * Composant d'indicateur de statut de connexion Google Calendar
 * Affiche si l'utilisateur est connecté et permet la navigation vers la configuration
 */
const GoogleSyncStatus = ({ userId, onNavigateToSetup }) => {
  const [status, setStatus] = useState({
    connected: false,
    loading: true,
    error: null
  });

  useEffect(() => {
    const fetchStatus = async () => {
      if (!userId) return;
      
      try {
        const result = await checkGoogleCalendarStatus(userId);
        setStatus({
          connected: result.connected || false,
          loading: false,
          error: null
        });
      } catch (error) {
        setStatus({
          connected: false,
          loading: false,
          error: error.message
        });
      }
    };

    fetchStatus();
    
    // Rafraîchir le statut toutes les 5 minutes
    const interval = setInterval(fetchStatus, 5 * 60 * 1000);
    
    return () => clearInterval(interval);
  }, [userId]);

  if (status.loading) {
    return (
      <div className="google-sync-status loading">
        <span className="status-spinner"></span>
        <span className="status-text">Vérification...</span>
      </div>
    );
  }

  if (status.error) {
    return (
      <div className="google-sync-status error">
        <span className="status-icon">⚠️</span>
        <span className="status-text">Erreur de vérification</span>
      </div>
    );
  }

  return (
    <div className={`google-sync-status ${status.connected ? 'connected' : 'disconnected'}`}>
      <span className="status-dot"></span>
      <span className="status-text">
        {status.connected ? 'Google Calendar connecté' : 'Google Calendar non connecté'}
      </span>
      {!status.connected && onNavigateToSetup && (
        <button className="btn-connect-google" onClick={onNavigateToSetup}>
          Connecter
        </button>
      )}
    </div>
  );
};

export default GoogleSyncStatus;