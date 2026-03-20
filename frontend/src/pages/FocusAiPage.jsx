import React, { useState, useEffect } from 'react';


import { getCurrentUser } from '../api/authApi';
import '../styles/pages/FocusAiPage.css';

import { uploadSchedulePdf, confirmTasks, refineSchedule } from '../api/focusAiApi';

const FocusAiPage = () => {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [suggestions, setSuggestions] = useState(null);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [currentUser, setCurrentUser] = useState(null);

    // Récupération de l'utilisateur au montage du composant
    useEffect(() => {
        const user = getCurrentUser();
        if (user) {
            setCurrentUser(user);
        } else {
            setError("Session expirée. Veuillez vous reconnecter.");
        }
    }, []);

    const handleFileChange = (e) => {
        setFile(e.target.files[0]);
        setError('');
        setMessage(''); // On nettoie le message de succès précédent si on change de fichier
    };

    const handleUpload = async (e) => {
        e.preventDefault();
        if (!file) return setError("Veuillez sélectionner un fichier PDF.");
        if (!currentUser) return setError("Utilisateur non identifié.");

        setLoading(true);
        setError('');
        try {
            // Utilisation de l'ID dynamique du currentUser
            const data = await uploadSchedulePdf(currentUser.id, file);
            setSuggestions(data);
        } catch (err) {
            setError("Erreur lors de l'analyse du PDF. Vérifiez que le fichier est valide.");
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    const handleConfirm = async () => {
        if (!currentUser) return;
        
        setLoading(true);
        try {
            const result = await confirmTasks(
                currentUser.id, // ID dynamique ici aussi
                suggestions.globalExplanation, 
                suggestions.tasks
            );
            setMessage(result);
            setSuggestions(null);
            setFile(null); // Reset du fichier après succès
        } catch (err) {
            setError("Erreur lors de la validation du planning.");
        } finally {
            setLoading(false);
        }
    };

    const [feedback, setFeedback] = useState('');

    const handleRefine = async () => {
    if (!feedback.trim()) return;
    
    setLoading(true);
    setError('');
    try {
        // Le flux d'échange : on envoie le texte + les suggestions actuelles
        const data = await refineSchedule(feedback, suggestions.tasks);
        setSuggestions(data); // On remplace les anciennes suggestions par les nouvelles
        setFeedback(''); // On vide le champ texte
    } catch (err) {
        setError("L'IA n'a pas pu modifier le planning. Réessayez.");
    } finally {
        setLoading(false);
    }
};

    // Si pas d'utilisateur, on peut afficher un message d'attente ou d'erreur
    if (!currentUser && !error) return <div className="loading-screen">Chargement du profil...</div>;

    return (
        <div className="focus-ai-container">
            <header className="focus-header">
                <h1> AI Document-to-Schedule ✨</h1>
                <p className="subtitle">
                    Bonjour <strong>{currentUser?.username || 'Étudiant'}</strong>, uploadez votre document du temps pour commencer.
                </p>
            </header>

            {/* Zone d'Upload */}
            {!suggestions && (
                <div className="upload-card">
                    <form onSubmit={handleUpload} className="upload-section">
                        <div className="file-input-wrapper">
                            <input type="file" accept=".pdf" onChange={handleFileChange} id="file-upload" />
                            <label htmlFor="file-upload" className="file-label">
                                {file ? file.name : "Choisir un fichier PDF"}
                            </label>
                        </div>
                        <button type="submit" className="btn-primary" disabled={loading || !file}>
                            {loading ? "Analyse intelligente..." : "Analyser mon planning"}
                        </button>
                    </form>
                </div>
            )}

            {error && <div className="alert alert-error">{error}</div>}
            {message && <div className="alert alert-success">{message}</div>}

            {/* Zone de prévisualisation */}
            {suggestions && (
                <div className="suggestions-section animate-fade-in">
                    <div className="ai-brief">
                        <h3>Analyse du planning</h3>
                        <p>{suggestions.globalExplanation}</p>
                    </div>

                    <div className="task-grid">
                        {suggestions.tasks.map((task, index) => (
                            <div key={index} className="task-card">
                                <div className="task-badge">Priorité {index + 1}</div>
                                <h4>{task.title}</h4>
                                <div className="task-meta">
                                    <span>⏱ {task.durationMinutes} min</span>
                                </div>
                                <p className="task-reasoning">{task.reasoning}</p>
                            </div>
                        ))}
                    </div>

                    <div className="sticky-actions">
                        <button className="btn-confirm" onClick={handleConfirm} disabled={loading}>
                            {loading ? "Synchronisation..." : "Appliquer à mon emploi du temps"}
                        </button>
                        <button className="btn-link" onClick={() => setSuggestions(null)}>
                            Annuler
                        </button>
                    </div>
                </div>
            )}
            {/* --- NOUVEAU : Bloc de discussion --- */}
        <div className="refine-section">
            <h3>Pas satisfait ?</h3>
            <p>Demandez à l'IA d'ajuster le planning (ex: "Mets le README à la fin") :</p>
            <div className="refine-input-group">
                <input 
                    type="text" 
                    placeholder="Votre demande de modification..." 
                    value={feedback}
                    onChange={(e) => setFeedback(e.target.value)}
                    disabled={loading}
                />
                <button className="btn-secondary" onClick={handleRefine} disabled={loading || !feedback}>
                    {loading ? "Mise à jour..." : "Modifier avec l'IA"}
                </button>
            </div>
        </div>
   
        </div>
        
        
    );
};

export default FocusAiPage;