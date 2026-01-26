import React, { useEffect, useState } from 'react';
import { getPendingInvitations, respondToInvitation } from '../api/teamApi';
import '../styles/components/InvitationList.css';

const InvitationList = ({ userId }) => {
    const [invitations, setInvitations] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Définition de la fonction à l'intérieur de l'effet
        const loadInvitations = async () => {
            try {
                const res = await getPendingInvitations(userId);
                setInvitations(res.data || res); 
            } catch (err) { // Ou simplement 'catch' sans variable si supporté, sinon 'err' (non utilisé mais souvent toléré si préfixé par _)
                console.error("Erreur lors du chargement des invitations", err);
            } finally {
                setLoading(false);
            }
        };

        if (userId) {
            loadInvitations();
        }
    }, [userId]);

    const handleAction = async (id, accept) => {
        try {
            await respondToInvitation(id, accept);
            setInvitations(prev => prev.filter(invit => invit.id !== id));
        } catch { // Retirez '(error)' ici
            alert("Erreur lors de la réponse à l'invitation");
        }
    };

    if (loading) return <div className="invit-loader">Chargement des notifications...</div>;

    return (
        <div className="invitations-wrapper">
            <div className="invit-header">
                <h3>Invitations en attente</h3>
                {/* {invitations.length > 0 && <span className="invit-badge">{invitations.length}</span>} */}
            </div>
            
            {invitations.length === 0 ? (
                <div className="no-invit-container">
                    <p className="no-invit-text">Aucune nouvelle invitation pour le moment.</p>
                </div>
            ) : (
                <div className="invitations-list">
                    {invitations.map(invit => (
                        <div className="invitation-item" key={invit.id}>
                            <div className="invit-details">
                                {/* On prend la 1ère lettre du nom de l'équipe */}
                                <div className="invit-avatar">
                                    {invit.team.name.charAt(0).toUpperCase()}
                                </div>
                                <div className="invit-info">
                                    <span className="invit-main-text">{invit.team.name}</span>
                                    <span className="invit-sub-text">
                                        {invit.inviter 
                                            ? `Invité par ${invit.inviter.username}` 
                                            : "Invité par un inconnu"
                                        }
                                    </span>
                                </div>
                            </div>
                            <div className="invit-buttons">
                                <button className="btn-action btn-cancel" onClick={() => handleAction(invit.id, false)}>
                                    Refuser
                                </button>
                                <button className="btn-action btn-confirm" onClick={() => handleAction(invit.id, true)}>
                                    Accepter
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default InvitationList;