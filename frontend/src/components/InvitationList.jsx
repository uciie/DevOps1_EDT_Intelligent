import React, { useEffect, useState } from 'react';
import { getPendingInvitations, respondToInvitation } from '../api/teamApi';
import '../styles/components/InvitationList.css';

const InvitationList = ({ userId }) => {
    const [invitations, setInvitations] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (userId) {
            loadInvitations();
        }
    }, [userId]);

    const loadInvitations = async () => {
        try {
            const res = await getPendingInvitations(userId);
            // On s'assure de récupérer les données correctement selon la structure de réponse API
            setInvitations(res.data || res); 
        } catch (error) {
            console.error("Erreur lors du chargement des invitations", error);
        } finally {
            setLoading(false);
        }
    };

    const handleAction = async (id, accept) => {
        try {
            await respondToInvitation(id, accept);
            // Mise à jour instantanée de l'interface
            setInvitations(prev => prev.filter(invit => invit.id !== id));
        } catch (error) {
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
                                    <span className="invit-sub-text">Invité par {invit.inviter.username}</span>
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