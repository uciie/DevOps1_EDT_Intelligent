import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCurrentUser } from '../api/authApi';
import InvitationList from '../components/InvitationList';
import "../styles/pages/NotificationPage.css"; 

const NotificationPage = () => {
    const [user, setUser] = useState(null);
    const navigate = useNavigate();

    useEffect(() => {
        const user = getCurrentUser();
        if (!user) {
            navigate("/login");
        } else {
            setUser(user);
        }
    }, [navigate]);

    if (!user) return null;

    return (
        <div className="notification-page-container">
            <header className="page-header">
                <div className="header-text">
                    <h1>Mes Notifications</h1>
                    <p>Retrouvez ici vos invitations à rejoindre des équipes.</p>
                </div>
            </header>

            <main className="notification-content-card">
                {/* Le composant qui gère la logique de la liste */}
                <InvitationList userId={user.id} />
            </main>
        </div>
    );
};

export default NotificationPage;