import React, { useEffect } from 'react';
// On réutilise les styles définis dans SchedulePage.css pour l'instant
// (ou vous pouvez les déplacer dans un fichier dédié si vous préférez)

export default function Notification({ message, type, onClose }) {
  useEffect(() => {
    if (message) {
      // Disparition automatique après 1 seconde
      const timer = setTimeout(() => {
        onClose();
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [message, onClose]);

  if (!message) return null;

  return (
    <div className={`notification notification-${type}`}>
      <span className="notification-icon">
        {type === 'success' ? '✅' : '⚠️'}
      </span>
      <span className="notification-message">{message}</span>
      <button 
        className="notification-close"
        onClick={onClose}
      >
        ×
      </button>
    </div>
  );
}