import React, { useState, useEffect } from 'react';
import { Edit, Trash2, Save, CheckCircle, X } from 'lucide-react';
import '../styles/components/ConflictModal.css';

/**
 * Modal interactive pour afficher et résoudre les conflits de synchronisation
 * Permet l'édition, la suppression et le suivi de progression
 */
const ConflictModal = ({ conflicts, onClose, onSync, onUpdateEvent, onDeleteEvent }) => {
  // État local pour gérer les conflits et leur résolution
  const [localConflicts, setLocalConflicts] = useState([]);
  const [editingStates, setEditingStates] = useState({});
  const [resolvedConflicts, setResolvedConflicts] = useState(new Set());

  // Initialisation des conflits locaux
  useEffect(() => {
    if (conflicts && conflicts.length > 0) {
      const enrichedConflicts = conflicts.map((conflict, index) => ({
        ...conflict,
        id: conflict.id || `conflict-${index}`,
        localVersion: {
          id: conflict.eventId,
          title: conflict.title,
          startTime: conflict.startTime,
          endTime: conflict.endTime,
          source: conflict.source
        },
        googleVersion: {
          id: conflict.conflictingWithId,
          title: conflict.conflictingWithTitle,
          startTime: conflict.startTime, // Même créneau
          endTime: conflict.endTime,
          source: conflict.conflictingWithSource
        }
      }));
      setLocalConflicts(enrichedConflicts);
    }
  }, [conflicts]);

  const totalConflicts = localConflicts.length;
  const resolvedCount = resolvedConflicts.size;
  const progressPercentage = totalConflicts > 0 ? (resolvedCount / totalConflicts) * 100 : 0;

  if (!localConflicts || localConflicts.length === 0) return null;

  const formatDateTime = (dateTimeStr) => {
    const date = new Date(dateTimeStr);
    return date.toLocaleString('fr-FR', {
      weekday: 'short',
      day: 'numeric',
      month: 'short',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const formatDateTimeForInput = (dateTimeStr) => {
    if (!dateTimeStr) return '';
    
    // Créer la date sans conversion UTC
    const date = new Date(dateTimeStr);
    
    // Extraire les composants en heure locale
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    
    // Retourner au format YYYY-MM-DDTHH:mm (heure locale)
    return `${year}-${month}-${day}T${hours}:${minutes}`;
  };

  const calculateDuration = (startTime, endTime) => {
    if (!startTime || !endTime) return '';
    
    const start = new Date(startTime);
    const end = new Date(endTime);
    const durationMs = end - start;
    
    if (durationMs < 0) return 'Durée invalide';
    
    const hours = Math.floor(durationMs / (1000 * 60 * 60));
    const minutes = Math.floor((durationMs % (1000 * 60 * 60)) / (1000 * 60));
    
    if (hours === 0) {
      return `${minutes} min`;
    } else if (minutes === 0) {
      return `${hours}h`;
    } else {
      return `${hours}h${minutes.toString().padStart(2, '0')}`;
    }
  };

  const getSourceLabel = (source) => {
    return source === 'LOCAL' ? '📝 Version locale' : '📅 Google Calendar';
  };

  const getSourceColor = (source) => {
    return source === 'LOCAL' ? '#0066cc' : '#4caf50';
  };

  // Activer le mode édition pour une version spécifique
  const handleEdit = (conflictId, versionType) => {
    const conflict = localConflicts.find(c => c.id === conflictId);
    const version = versionType === 'local' ? conflict.localVersion : conflict.googleVersion;

    setEditingStates({
      ...editingStates,
      [`${conflictId}-${versionType}`]: {
        title: version.title,
        startTime: version.startTime,
        endTime: version.endTime
      }
    });
  };

  // Annuler l'édition
  const handleCancelEdit = (conflictId, versionType) => {
    const newEditingStates = { ...editingStates };
    delete newEditingStates[`${conflictId}-${versionType}`];
    setEditingStates(newEditingStates);
  };

  // Mettre à jour les champs en édition
  const handleFieldChange = (conflictId, versionType, field, value) => {
    const editKey = `${conflictId}-${versionType}`;
    const currentEditState = editingStates[editKey];
    
    // Pour les champs de date, on garde la valeur telle quelle (heure locale)
    // Le format datetime-local de l'input HTML5 gère déjà l'heure locale
    
    let updatedState = {
      ...currentEditState,
      [field]: value
    };
    
    // Si on modifie la date de début ou de fin, recalculer l'autre automatiquement
    if (field === 'startTime' && currentEditState.startTime && currentEditState.endTime) {
      // Calculer la durée actuelle
      const oldStart = new Date(currentEditState.startTime);
      const oldEnd = new Date(currentEditState.endTime);
      const durationMs = oldEnd - oldStart;
      
      // Appliquer la même durée à partir de la nouvelle date de début
      if (value) {
        const newStart = new Date(value);
        const newEnd = new Date(newStart.getTime() + durationMs);
        
        // Formater la nouvelle date de fin
        const year = newEnd.getFullYear();
        const month = String(newEnd.getMonth() + 1).padStart(2, '0');
        const day = String(newEnd.getDate()).padStart(2, '0');
        const hours = String(newEnd.getHours()).padStart(2, '0');
        const minutes = String(newEnd.getMinutes()).padStart(2, '0');
        
        updatedState.endTime = `${year}-${month}-${day}T${hours}:${minutes}`;
      }
    } else if (field === 'endTime' && currentEditState.startTime && currentEditState.endTime) {
      // Si on modifie la fin, vérifier qu'elle est après le début
      if (value && currentEditState.startTime) {
        const newEnd = new Date(value);
        const start = new Date(currentEditState.startTime);
        
        // Si la fin est avant le début, ajuster le début automatiquement
        if (newEnd < start) {
          // Calculer la durée actuelle
          const oldStart = new Date(currentEditState.startTime);
          const oldEnd = new Date(currentEditState.endTime);
          const durationMs = oldEnd - oldStart;
          
          // Recalculer le début en soustrayant la durée
          const newStart = new Date(newEnd.getTime() - durationMs);
          
          const year = newStart.getFullYear();
          const month = String(newStart.getMonth() + 1).padStart(2, '0');
          const day = String(newStart.getDate()).padStart(2, '0');
          const hours = String(newStart.getHours()).padStart(2, '0');
          const minutes = String(newStart.getMinutes()).padStart(2, '0');
          
          updatedState.startTime = `${year}-${month}-${day}T${hours}:${minutes}`;
        }
      }
    }
    
    setEditingStates({
      ...editingStates,
      [editKey]: updatedState
    });
  };

  // Sauvegarder les modifications d'une version
  const handleSaveEdit = async (conflictId, versionType) => {
    const editKey = `${conflictId}-${versionType}`;
    const editData = editingStates[editKey];
    const conflict = localConflicts.find(c => c.id === conflictId);
    const version = versionType === 'local' ? conflict.localVersion : conflict.googleVersion;

    if (onUpdateEvent) {
      try {
        // Convertir les dates du format datetime-local vers ISO local
        // Format reçu: "2025-11-26T14:00" (sans timezone)
        // On doit l'envoyer au format: "2025-11-26T14:00:00" (ISO local sans Z)
        const formatLocalDateTime = (dateTimeStr) => {
          if (!dateTimeStr) return dateTimeStr;
          // Si déjà au bon format, on retourne tel quel
          if (dateTimeStr.includes(':00', dateTimeStr.length - 3)) return dateTimeStr;
          // Sinon on ajoute les secondes
          return `${dateTimeStr}:00`;
        };

        await onUpdateEvent(version.id, {
          summary: editData.title,
          startTime: formatLocalDateTime(editData.startTime),
          endTime: formatLocalDateTime(editData.endTime)
        });

        // Mettre à jour l'état local
        const updatedConflicts = localConflicts.map(c => {
          if (c.id === conflictId) {
            if (versionType === 'local') {
              c.localVersion = { ...c.localVersion, ...editData };
            } else {
              c.googleVersion = { ...c.googleVersion, ...editData };
            }
          }
          return c;
        });
        setLocalConflicts(updatedConflicts);

        // Supprimer l'état d'édition
        const newEditingStates = { ...editingStates };
        delete newEditingStates[editKey];
        setEditingStates(newEditingStates);
      } catch (error) {
        console.error('Erreur lors de la sauvegarde:', error);
        alert('Erreur lors de la sauvegarde des modifications');
      }
    }
  };

  // Supprimer une version (événement)
  const handleDelete = async (conflictId, versionType) => {
    const conflict = localConflicts.find(c => c.id === conflictId);
    const version = versionType === 'local' ? conflict.localVersion : conflict.googleVersion;

    if (!window.confirm(`Voulez-vous vraiment supprimer l'événement "${version.title}" ?`)) {
      return;
    }

    if (onDeleteEvent) {
      try {
        await onDeleteEvent(version.id);

        // Retirer le conflit de la liste
        const updatedConflicts = localConflicts.filter(c => c.id !== conflictId);
        setLocalConflicts(updatedConflicts);

        // Marquer comme résolu
        setResolvedConflicts(new Set([...resolvedConflicts, conflictId]));
      } catch (error) {
        console.error('Erreur lors de la suppression:', error);
        alert('Erreur lors de la suppression de l\'événement');
      }
    }
  };

  // Valider un conflit (marquer comme résolu)
  const handleResolveConflict = (conflictId) => {
    setResolvedConflicts(new Set([...resolvedConflicts, conflictId]));
  };

  // Déclencher la synchronisation (même comportement que btn-sync de SchedulePage)
  const handleSyncClick = async () => {
    if (onSync) {
      await onSync();
    }
    onClose();
  };

  // Rendu d'une version (avec ou sans édition)
  const renderVersion = (conflict, versionType) => {
    const version = versionType === 'local' ? conflict.localVersion : conflict.googleVersion;
    const editKey = `${conflict.id}-${versionType}`;
    const isEditing = !!editingStates[editKey];
    const isResolved = resolvedConflicts.has(conflict.id);

    return (
      <div 
        className={`conflict-version ${isResolved ? 'resolved' : ''}`}
        style={{ borderLeft: `4px solid ${getSourceColor(version.source)}` }}
      >
        <div className="version-header">
          <span className="version-label">{getSourceLabel(version.source)}</span>
          {!isResolved && (
            <div className="version-actions">
              {!isEditing ? (
                <>
                  <button 
                    className="btn-icon btn-edit-icon"
                    onClick={() => handleEdit(conflict.id, versionType)}
                    title="Modifier"
                  >
                    <Edit size={16} />
                  </button>
                  <button 
                    className="btn-icon btn-delete-icon"
                    onClick={() => handleDelete(conflict.id, versionType)}
                    title="Supprimer"
                  >
                    <Trash2 size={16} />
                  </button>
                </>
              ) : (
                <>
                  <button 
                    className="btn-icon btn-save-icon"
                    onClick={() => handleSaveEdit(conflict.id, versionType)}
                    title="Sauvegarder"
                  >
                    <Save size={16} />
                  </button>
                  <button 
                    className="btn-icon btn-cancel-icon"
                    onClick={() => handleCancelEdit(conflict.id, versionType)}
                    title="Annuler"
                  >
                    <X size={16} />
                  </button>
                </>
              )}
            </div>
          )}
        </div>

        <div className="version-details">
          {!isEditing ? (
            <>
              <p><strong>Titre :</strong> {version.title}</p>
              <p><strong>Début :</strong> {formatDateTime(version.startTime)}</p>
              <p><strong>Fin :</strong> {formatDateTime(version.endTime)}</p>
              <p className="duration-info">
                <span className="duration-label">⏱️ Durée :</span>
                <span className="duration-value">{calculateDuration(version.startTime, version.endTime)}</span>
              </p>
            </>
          ) : (
            <div className="edit-form">
              <div className="form-field">
                <label><strong>Titre :</strong></label>
                <input
                  type="text"
                  value={editingStates[editKey].title}
                  onChange={(e) => handleFieldChange(conflict.id, versionType, 'title', e.target.value)}
                  className="input-edit"
                />
              </div>
              <div className="form-field">
                <label><strong>Début :</strong></label>
                <input
                  type="datetime-local"
                  value={formatDateTimeForInput(editingStates[editKey].startTime)}
                  onChange={(e) => handleFieldChange(conflict.id, versionType, 'startTime', e.target.value)}
                  className="input-edit"
                />
              </div>
              <div className="form-field">
                <label><strong>Fin :</strong></label>
                <input
                  type="datetime-local"
                  value={formatDateTimeForInput(editingStates[editKey].endTime)}
                  onChange={(e) => handleFieldChange(conflict.id, versionType, 'endTime', e.target.value)}
                  className="input-edit"
                />
              </div>
              <div className="duration-display">
                ⏱️ Durée : <strong>{calculateDuration(editingStates[editKey].startTime, editingStates[editKey].endTime)}</strong>
              </div>
            </div>
          )}
        </div>

        {isResolved && (
          <div className="resolved-badge">
            <CheckCircle size={16} />
            <span>Résolu</span>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="conflict-modal-overlay" onClick={onClose}>
      <div className="conflict-modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="conflict-modal-header">
          <div className="header-title-section">
            <h2>⚠️ Conflits de créneaux détectés</h2>
            <div className="progress-indicator">
              <div className="progress-text">
                {resolvedCount > 0 && <CheckCircle size={18} className="check-icon" />}
                <span>{resolvedCount} / {totalConflicts} conflit(s) résolu(s)</span>
              </div>
              <div className="progress-bar">
                <div 
                  className="progress-fill" 
                  style={{ width: `${progressPercentage}%` }}
                ></div>
              </div>
            </div>
          </div>
          <button className="btn-modal-close" onClick={onClose}>×</button>
        </div>

        <div className="conflict-modal-body">
          {localConflicts.map((conflict, index) => {
            const isResolved = resolvedConflicts.has(conflict.id);
            
            return (
              <div key={conflict.id} className={`conflict-item ${isResolved ? 'resolved' : ''}`}>
                <div className="conflict-item-header">
                  <h3>Conflit #{index + 1}</h3>
                  <p className="conflict-description">
                    L'événement <strong>"{conflict.localVersion.title}"</strong> chevauche l'événement <strong>"{conflict.googleVersion.title}"</strong>
                  </p>
                </div>

                <div className="conflict-comparison">
                  {renderVersion(conflict, 'local')}
                  <div className="conflict-separator">VS</div>
                  {renderVersion(conflict, 'google')}
                </div>

                {!isResolved && (
                  <div className="conflict-item-footer">
                    <div className="conflict-info-box">
                      <p>
                        ℹ️ Ces deux événements se chevauchent dans le temps. 
                        Modifiez les horaires pour éviter le conflit ou supprimez l'un des deux.
                      </p>
                    </div>
                    <button 
                      className="btn-validate-conflict"
                      onClick={() => handleResolveConflict(conflict.id)}
                    >
                      <CheckCircle size={18} />
                      Marquer comme résolu
                    </button>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        <div className="conflict-modal-footer">
          <p className="footer-help-text">
            {resolvedCount === totalConflicts 
              ? '✅ Tous les conflits sont résolus ! Vous pouvez maintenant synchroniser.'
              : 'Résolvez tous les conflits avant de synchroniser, ou synchronisez avec les conflits restants.'
            }
          </p>
          <div className="footer-actions">
            <button className="btn-modal-secondary" onClick={onClose}>
              Fermer
            </button>
            <button 
              className="btn-modal-sync" 
              onClick={handleSyncClick}
              disabled={localConflicts.length > 0 && resolvedCount < totalConflicts}
            >
              🔄 Synchro Google
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ConflictModal;