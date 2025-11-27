import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { getCurrentUser, logoutUser } from "../api/authApi";
import "../styles/pages/Home.css";

// Page principale d'accueil
function Home() {
  const [user, setUser] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const currentUser = getCurrentUser();
    setUser(currentUser);
  }, []);

  const handleLogout = () => {
    logoutUser();
    setUser(null);
    navigate("/", { replace: true });
  };

  return (
    <div className="home-page">
      <div className="home-hero">
        <h1>Bienvenue sur EDT Intelligent 😎</h1>
        <p className="hero-subtitle">
          Gérez votre temps efficacement avec notre système intelligent de
          planification
        </p>

        {user ? (
          <div className="user-section">
            <div className="welcome-card">
              <div className="welcome-header">
                <span className="user-avatar">
                  {user.username.charAt(0).toUpperCase()}
                </span>
                <div>
                  <h2>Bonjour, {user.username} !</h2>
                  <p>Prêt à organiser votre journée ?</p>
                </div>
              </div>

              <div className="action-buttons">
                <Link to="/schedule" className="btn btn-primary">
                  📅 Voir mon emploi du temps
                </Link>
                <button onClick={handleLogout} className="btn btn-secondary">
                  🚪 Se déconnecter
                </button>
              </div>
            </div>
          </div>
        ) : (
          <div className="guest-section">
            <div className="guest-actions">
              <Link to="/login" className="btn btn-primary">
                🔐 Se connecter
              </Link>
              <Link to="/register" className="btn btn-outline">
                ✨ Créer un compte
              </Link>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default Home;